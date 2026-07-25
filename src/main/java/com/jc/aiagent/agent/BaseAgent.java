package com.jc.aiagent.agent;

import cn.hutool.core.util.StrUtil;
import com.jc.aiagent.agent.model.AgentState;
import com.jc.aiagent.context.UserContext;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 抽象基础代理类，用于管理代理状态和执行流程。
 * 提供状态转换、内存管理和基于步骤的执行循环的基础功能。
 * 子类必须实现step方法。
 */
@Data
@Slf4j
public abstract class BaseAgent {

    /**
     * 名称
     */
    private String name;

    /**
     * 提示
     */
    private String systemPrompt;
    private String nextStepPrompt;

    /**
     * 状态
     */
    private AgentState state = AgentState.IDLE;

    /**
     * 执行控制
     */
    private int maxSteps = 10;
    private int currentStep = 0;

    /**
     * LLM
     */
    private ChatClient chatClient;

    /**
     * Memory（需要自主维护会话上下文）
     */
    private List<Message> messageList = new ArrayList<>();

    /**
     * 运行代理
     *
     * @param userPrompt 用户提示词
     * @return 执行结果
     */
    public String run(String userPrompt) {
        if (this.state != AgentState.IDLE) {
            throw new RuntimeException("Cannot run agent from state: " + this.state);
        }
        if (StrUtil.isBlank(userPrompt)) {
            throw new RuntimeException("Cannot run agent with empty user prompt");
        }

        // 更改状态
        state = AgentState.RUNNING;

        // 记录消息上下文
        messageList.add(new UserMessage(userPrompt));

        // 保存结果列表
        List<String> results = new ArrayList<>();
        try {
            for (int i = 0; i < maxSteps && state != AgentState.FINISHED; i++) {
                int stepNumber = i + 1;
                currentStep = stepNumber;
                log.info("Executing step " + stepNumber + "/" + maxSteps);
                // 单步执行
                String stepResult = step();
                String result = "Step " + stepNumber + ": " + stepResult;
                results.add(result);
            }
            // 检查是否超出步骤限制
            if (currentStep >= maxSteps) {
                state = AgentState.FINISHED;
                results.add("Terminated: Reached max steps (" + maxSteps + ")");
            }
            return String.join("\n", results);

        } catch (Exception e) {
            state = AgentState.ERROR;
            log.error("Error executing agent", e);
            return "执行错误" + e.getMessage();

        } finally {
            // 清理资源
            this.cleanup();
        }
    }

    /**
     * 执行单个步骤
     *
     * @return 步骤执行结果
     */
    public abstract String step();

    /**
     * 清理资源
     */
    protected void cleanup() {
        // 子类可以重写此方法来清理资源
    }

    /**
     * 逐字发送消息以实现打字机效果
     *
     * @param emitter SseEmitter实例
     * @param message 完整的消息内容
     * @throws IOException 如果发送失败
     */
    private void sendWithTypingEffect(SseEmitter emitter, String message) throws IOException {
        // 每个字符之间的延迟时间（毫秒），控制打字速度
        int delay = 50; // 可以根据需要调整这个值

        // 用于构建逐步显示的字符串
        StringBuilder currentMessage = new StringBuilder();

        // 逐个字符添加并发送
        for (char c : message.toCharArray()) {
            currentMessage.append(c);

            // 发送当前累积的消息
            emitter.send(currentMessage.toString());

            try {
                // 等待指定的时间
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("发送中断: " + e.getMessage());
            }
        }
    }

    /**
     * 运行代理（流式输出）
     * @param userPrompt 用户提示词
     * @return SseEmitter 实例
     */
    public SseEmitter runStream(String userPrompt){
        // 创建SseEmitter，设置较长的超时时间
        SseEmitter emitter = new SseEmitter(300000L); // 5分钟超时

        // 在请求线程中捕获 userId，用于在异步线程中恢复用户上下文
        Long userId = UserContext.getUserId();

        // 使用线程异步处理，避免阻塞主线程
        CompletableFuture.runAsync(() -> {
            // 在异步线程中恢复用户上下文（解决 ThreadLocal 跨线程丢失问题）
            if (userId != null) {
                UserContext.setUserId(userId);
            }
            try {
                if (this.state != AgentState.IDLE) {
                    emitter.send("错误：该状态无法运行代理: " + this.state);
                    emitter.complete();
                    return;
                }
                if (StrUtil.isBlank(userPrompt)) {
                    emitter.send("错误：不能使用空提示词运行代理");
                    emitter.complete();
                    return;
                }

                // 更改状态
                setState(AgentState.RUNNING);
                // 记录消息上下文
                messageList.add(new UserMessage(userPrompt));

                try {
                    for (int i = 1; i <= maxSteps && state != AgentState.FINISHED; i++) {
                        setCurrentStep(i);
                        log.info("Executing step " + i + "/" + maxSteps);

                        // 单步执行
                        String stepResult = step();
                        String result = "Step " + i + ": " + stepResult;
                        emitter.send(result);
                    }
                    // 检查是否超出步骤限制
                    if (currentStep > maxSteps) {
                        state = AgentState.FINISHED;
                        emitter.send("执行结束: 达到最大步骤 (" + maxSteps + ")");
                    }
                    emitter.complete();

                } catch (Exception e) {
                    state = AgentState.ERROR;
                    log.error("执行智能体失败", e);
                    try {
                        emitter.send("执行错误: " + e.getMessage());
                        emitter.complete();
                    } catch (Exception ex) {
                        emitter.completeWithError(ex);
                    }

                } finally {
                    // 清理资源
                    this.cleanup();
                }

            } catch (Exception e) {
                emitter.completeWithError(e);
            } finally {
                // 清理异步线程上的用户上下文，防止内存泄漏
                UserContext.clear();
            }
        });

        // 设置超时和完成回调
        emitter.onTimeout(() -> {
            setState(AgentState.ERROR);
            this.cleanup();
            log.warn("SSE connection timed out");
        });

        emitter.onCompletion(() -> {
            if (this.state == AgentState.RUNNING) {
                setState(AgentState.FINISHED);
            }
            this.cleanup();
            log.info("SSE connection completed");
        });

        return emitter;
    }
}