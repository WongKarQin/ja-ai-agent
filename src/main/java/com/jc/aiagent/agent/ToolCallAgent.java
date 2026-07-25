package com.jc.aiagent.agent;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.jc.aiagent.agent.model.AgentState;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.ToolCallback;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 处理工具调用的基础代理类，具体实现了 think 和 act 方法，可以用作创建实例的父类
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public class ToolCallAgent extends ReActAgent {

    // 可用的工具
    private final ToolCallback[] availableTools;

    // 保存了工具调用信息的响应
    private ChatResponse toolCallChatResponse;

    // 工具调用管理者
    private final ToolCallingManager toolCallingManager;

    // 禁用内置的工具调用机制，自己维护上下文
    private final ChatOptions chatOptions;

    public ToolCallAgent(ToolCallback[] availableTools) {
        super();
        this.availableTools = availableTools;
        this.toolCallingManager = ToolCallingManager.builder().build();
        // 禁用 Spring AI 内置的工具调用机制，自己维护选项和消息上下文
        this.chatOptions = DashScopeChatOptions.builder()
                .withInternalToolExecutionEnabled(false)
                .build();
    }

    /**
     * 处理当前状态并决定下一步行动
     *
     * @return 是否需要执行行动
     */
    @Override
    public boolean think() {
        List<Message> messageList = getMessageList();
        String nextStepPrompt = getNextStepPrompt();
        if (StrUtil.isNotBlank(nextStepPrompt)) {
            messageList.add(
                    new UserMessage(nextStepPrompt)
            );
        }
        Prompt prompt = new Prompt(messageList, chatOptions);

        try {
            // 获取带工具选项的响应
            ChatResponse chatResponse = getChatClient().prompt(prompt)
                    .system(getSystemPrompt())
                    .toolCallbacks(availableTools)
                    .call()
                    .chatResponse();
            // 记录响应，用于 Act
            this.toolCallChatResponse = chatResponse;
            AssistantMessage assistantMessage = chatResponse.getResult().getOutput();
            // 输出提示信息
            String result = assistantMessage.getText();
            log.info(getName() + "的思考: " + result);
            // 获取要调用的工具
            List<AssistantMessage.ToolCall> toolCallList = assistantMessage.getToolCalls();
            log.info(getName() + "选择了 " + toolCallList.size() + " 个工具来使用");
            if (toolCallList.isEmpty()) {
                // 无工具调用，直接输出LLM回答
                messageList.add(assistantMessage);
                this.setFinalAnswer(result);
                this.setCurrentThinkingText(null);
                return false;
            }
            // 有工具调用时，也捕获LLM文本作为潜在最终答案
            // （LLM可能在调用 doTerminate 的同时生成自然语言回答）
            if (result != null && !result.isBlank()) {
                this.setFinalAnswer(result);
            }
            // 设置精简的思考状态文本
            this.setCurrentThinkingText("⚡ 正在调用 " + toolCallList.stream()
                    .map(tc -> beautifyToolName(tc.name()))
                    .collect(Collectors.joining("、")));
            String toolCallInfo = toolCallList.stream()
                   .map(
                             toolCall -> String.format("工具名称：%s，参数：%s",
                                    toolCall.name(), toolCall.arguments()
                            )
                    )
                    .collect(Collectors.joining("\n"));
            log.info(toolCallInfo);
            return true;

        } catch (Exception e) {
            log.error(getName() + "的思考过程遇到了问题: " + e.getMessage());
            messageList.add(
                    new AssistantMessage("处理时遇到错误: " + e.getMessage())
            );
            this.setFinalAnswer("处理时遇到错误: " + e.getMessage());
            return false;
        }
    }

    /**
     * 执行工具调用并处理结果
     *
     * @return 执行结果
     */
    @Override
    public String act() {
        if (!toolCallChatResponse.hasToolCalls()) {
            return "没有工具调用";
        }

        // 调用工具
        Prompt prompt = new Prompt(getMessageList(), chatOptions);
        ToolExecutionResult toolExecutionResult = toolCallingManager.executeToolCalls(prompt, toolCallChatResponse);
        // 记录消息上下文，conversationHistory 已经包含了历史消息和工具调用返回的结果
        List<Message> conversationHistoryMessages = toolExecutionResult.conversationHistory();
        setMessageList(conversationHistoryMessages);
        // 当前工具调用的结果
        ToolResponseMessage toolResponseMessage = (ToolResponseMessage) CollUtil.getLast(conversationHistoryMessages);
        String results = toolResponseMessage.getResponses().stream()
                .map(
                        response -> String.format("工具 %s 完成了它的任务！结果: %s",
                                response.name(), response.responseData()
                        )
                )
                .collect(Collectors.joining("\n"));
        // 判断是否调用了终止工具
        boolean terminateToolCalled = toolResponseMessage.getResponses().stream()
                .anyMatch(response -> "doTerminate".equals(response.name()));
        if (terminateToolCalled) {
            // 从 toolCallChatResponse 的 ToolCall 参数中提取 LLM 最终回答（备用）
            extractFinalAnswerFromToolCalls();
            setState(AgentState.FINISHED);
        }
        // 设置工具执行完成的思考状态
        this.setCurrentThinkingText("✅ " + toolResponseMessage.getResponses().stream()
                .map(response -> beautifyToolName(response.name()))
                .collect(Collectors.joining("、")) + " 执行完成");
        log.info(results);
        return results;
    }

    /**
     * 从 toolCallChatResponse 的 ToolCall 参数中提取最终回答
     * 当 think() 未捕获到有效文本时，此方法从 doTerminate 的参数中提取答案作为备用
     */
    private void extractFinalAnswerFromToolCalls() {
        // 仅在 finalAnswer 尚未设置时才从参数中提取
        if (getFinalAnswer() != null && !getFinalAnswer().isBlank()) {
            return;
        }
        if (this.toolCallChatResponse == null || !this.toolCallChatResponse.hasToolCalls()) {
            return;
        }
        AssistantMessage assistantMessage = this.toolCallChatResponse.getResult().getOutput();
        for (AssistantMessage.ToolCall toolCall : assistantMessage.getToolCalls()) {
            if ("doTerminate".equals(toolCall.name()) && toolCall.arguments() != null) {
                try {
                    JSONObject json = JSONUtil.parseObj(toolCall.arguments());
                    // 尝试提取常见字段名
                    String answer = json.getStr("final_answer");
                    if (answer == null || answer.isBlank()) {
                        answer = json.getStr("answer");
                    }
                    if (answer == null || answer.isBlank()) {
                        answer = json.getStr("result");
                    }
                    if (answer == null || answer.isBlank()) {
                        answer = json.getStr("message");
                    }
                    if (answer != null && !answer.isBlank()) {
                        this.setFinalAnswer(answer);
                        return;
                    }
                } catch (Exception e) {
                    // 参数不是 JSON 格式，忽略
                    log.debug("doTerminate 参数解析失败: {}", e.getMessage());
                }
            }
        }
    }

    /**
     * 将工具方法名转换为中文展示名
     */
    private String beautifyToolName(String toolName) {
        return switch (toolName) {
            case "getWeather" -> "天气查询";
            case "getLocalWeather" -> "本地天气查询";
            case "searchWeb" -> "网页搜索";
            case "scrapeWebPage" -> "网页抓取";
            case "readFile" -> "文件读取";
            case "writeFile" -> "文件写入";
            case "downloadResource" -> "资源下载";
            case "executeTerminalCommand" -> "终端操作";
            case "generatePDF" -> "PDF生成";
            case "doTerminate" -> "终止任务";
            case "userQuery" -> "用户查询";
            default -> toolName;
        };
    }

}
