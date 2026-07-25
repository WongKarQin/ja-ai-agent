package com.jc.aiagent.agent;

import com.jc.aiagent.advisor.LoggerAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

/**
 * 恋爱大师智能体
 */
@Component
public class LoveManus extends ToolCallAgent {
    public LoveManus(ToolCallback[] allTools, ChatModel dashscopeChatModel) {
        super(allTools);
        this.setName("loveManus");
        this.setSystemPrompt("""
                你是 OpenManus，一个全能的AI助手，旨在解决用户提出的任何任务。
                你可以使用各种工具来高效完成复杂请求。

                重要规则：
                - 当用户询问天气相关问题时，必须使用 getWeather 或 getLocalWeather 工具来查询天气，禁止使用网页搜索或网页抓取来获取天气信息。
                - getWeather 用于查询指定城市的天气，参数为城市名称（如：深圳、北京、上海）。
                - getLocalWeather 用于自动获取用户当前位置的天气，无需参数。
                - 查询到天气结果后，用自然语言向用户播报天气情况。
                """);
        this.setNextStepPrompt("""  
                Based on user needs, proactively select the most appropriate tool or combination of tools.
                For complex tasks, you can break down the problem and use different tools step by step to solve it.
                After using each tool, clearly explain the execution results and suggest the next steps.
                If you want to stop the interaction at any point, use the `terminate` tool/function call.
                """);
        this.setMaxSteps(20);
        this.setChatClient(
                ChatClient.builder(dashscopeChatModel)
                        .defaultAdvisors(new LoggerAdvisor())
                        .build()
        );
    }
}
