package com.jc.aiagent.tools;

import com.jc.aiagent.advisor.LoggerAdvisor;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class WeatherToolTest {

    @Resource
    private ChatModel dashscopeChatModel;

    @Resource
    private WeatherTool weatherTool;  // 注入 Spring 管理的实例

    /**
     * 方式1：通过 ChatClient Builder 设置默认工具（推荐用于全局工具）
     */
    @Test
    void getWeather1() {
        ChatClient chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultAdvisors(new LoggerAdvisor())
                .defaultTools(weatherTool)   // 使用注入的实例，避免 new 新对象
                .build();

        String aiReply = chatClient.prompt()
                .user("深圳今天天气怎么样?")
                .call()
                .content();

        System.out.println("AI回复: " + aiReply);
        assertNotNull(aiReply);
        assertTrue(aiReply.contains("深圳") || aiReply.contains("天气"));
    }

    /**
     * 方式2：在单次请求中动态提供工具（推荐用于按需工具）
     */
    @Test
    void getWeather2() {
        String aiReply = ChatClient.create(dashscopeChatModel)
                .prompt()
                .user("深圳今天天气怎么样?")
                .advisors(new LoggerAdvisor())
                .tools(weatherTool)          // 单次请求级别注册工具
                .call()
                .content();

        System.out.println("AI回复: " + aiReply);
        assertNotNull(aiReply);
    }

    /**
     * 方式3：通过 ToolCallingChatOptions 底层配置（高级用法）
     */
    @Test
    void getWeather3() {
        ToolCallingChatOptions toolOptions = ToolCallingChatOptions.builder()
                .toolCallbacks(ToolCallbacks.from(weatherTool))
                .build();

        String aiReply = ChatClient.create(dashscopeChatModel)
                .prompt(new Prompt("深圳今天天气怎么样?", toolOptions))
                .advisors(new LoggerAdvisor())
                .call()
                .content();

        System.out.println("AI回复: " + aiReply);
        assertNotNull(aiReply);
    }

    /**
     * 方式4：直接调用工具方法（不经过 AI，验证工具本身）
     */
    @Test
    void getWeatherDirect() {
        String result = weatherTool.getWeather("深圳市");
        System.out.println("工具直接返回: " + result);
        assertNotNull(result);
    }

    @Test
    void getLocalWeather() {
        String result = weatherTool.getLocalWeather();
        System.out.println("工具直接返回: " + result);
        assertNotNull(result);
    }
}