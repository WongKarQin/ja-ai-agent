package com.jc.aiagent.agent;

import cn.hutool.core.util.StrUtil;
import com.jc.aiagent.agent.model.AgentState;
import com.jc.aiagent.context.UserContext;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.CompletableFuture;

/**
 * ReAct (Reasoning and Acting) 模式的代理抽象类
 * 实现了思考-行动的循环模式
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public abstract class ReActAgent extends BaseAgent {

    /**
     * 最终答案（当 think 返回 false 时，LLM 给出的回复）
     */
    private String finalAnswer;

    /**
     * 当前思考状态文本（用于前端精简展示）
     */
    private String currentThinkingText;

    /**
     * 处理当前状态并决定下一步行动
     *
     * @return 是否需要执行行动，true表示需要执行，false表示不需要执行
     */
    public abstract boolean think();

    /**
     * 执行决定的行动
     *
     * @return 行动执行结果
     */
    public abstract String act();

    /**
     * 执行单个步骤：思考和行动
     *
     * @return 步骤执行结果
     */
    @Override
    public String step() {
        try {
            boolean shouldAct = think();
            if (!shouldAct) {
                return "思考完成 - 无需行动";
            }
            return act();
        } catch (Exception e) {
            // 记录异常日志
            e.printStackTrace();
            return "步骤执行失败: " + e.getMessage();
        }
    }

    /**
     * 运行代理（流式输出，使用 SSE 命名事件）
     * <p>
     * 事件类型：
     * - thinking：思考状态（精简标签，如 "⚡ 正在调用 天气查询"）
     * - answer：最终答案
     * - complete：完成信号
     * - error：错误信息
     *
     * @param userPrompt 用户提示词
     * @return SseEmitter 实例
     */
    @Override
    public SseEmitter runStream(String userPrompt) {
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
                if (this.getState() != AgentState.IDLE) {
                    emitter.send(SseEmitter.event().name("error").data("错误：该状态无法运行代理: " + this.getState()));
                    emitter.complete();
                    return;
                }
                if (StrUtil.isBlank(userPrompt)) {
                    emitter.send(SseEmitter.event().name("error").data("错误：不能使用空提示词运行代理"));
                    emitter.complete();
                    return;
                }

                // 更改状态
                setState(AgentState.RUNNING);
                // 记录消息上下文
                getMessageList().add(new UserMessage(userPrompt));

                boolean answerSent = false;

                try {
                    for (int i = 1; i <= getMaxSteps() && getState() != AgentState.FINISHED; i++) {
                        setCurrentStep(i);
                        log.info("Executing step " + i + "/" + getMaxSteps());

                        // 发送思考开始状态
                        emitter.send(SseEmitter.event().name("thinking").data("🤔 正在思考..."));

                        // 执行思考
                        boolean shouldAct = think();

                        if (!shouldAct) {
                            // 思考完成，无需行动 → 输出最终答案
                            String answer = (finalAnswer != null && !finalAnswer.isBlank())
                                    ? finalAnswer
                                    : "任务已完成";
                            emitter.send(SseEmitter.event().name("answer").data(answer));
                            answerSent = true;
                            break;
                        }

                        // 发送工具调用状态（精简展示）
                        if (currentThinkingText != null) {
                            emitter.send(SseEmitter.event().name("thinking").data(currentThinkingText));
                        }

                        // 执行行动（工具调用）
                        act();
                    }

                    // 如果循环结束但未发送答案（如达到最大步骤或调用了终止工具）
                    if (!answerSent) {
                        if (getCurrentStep() >= getMaxSteps() && getState() != AgentState.FINISHED) {
                            setState(AgentState.FINISHED);
                        }
                        String answer = (finalAnswer != null && !finalAnswer.isBlank())
                                ? finalAnswer
                                : "任务已完成";
                        emitter.send(SseEmitter.event().name("answer").data(answer));
                    }

                    // 发送完成事件
                    emitter.send(SseEmitter.event().name("complete").data(""));
                    emitter.complete();

                } catch (Exception e) {
                    setState(AgentState.ERROR);
                    log.error("执行智能体失败", e);
                    try {
                        emitter.send(SseEmitter.event().name("error").data("执行错误: " + e.getMessage()));
                        emitter.complete();
                    } catch (Exception ex) {
                        emitter.completeWithError(ex);
                    }
                } finally {
                    // 清理资源
                    this.cleanup();
                }

            } catch (Exception e) {
                try {
                    emitter.send(SseEmitter.event().name("error").data("系统错误: " + e.getMessage()));
                } catch (Exception ignored) {
                }
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
            if (this.getState() == AgentState.RUNNING) {
                setState(AgentState.FINISHED);
            }
            this.cleanup();
            log.info("SSE connection completed");
        });

        return emitter;
    }
}
