package com.jc.aiagent.tools;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户查询工具
 * 测试工具上下文，传递请求会话id
 */
@Slf4j
public class UserQueryTool {

    private static Map<String, String> userNameMap = new HashMap<>() {
        {
            put("1", "张三");
            put("2", "李四");
            put("3", "王五");
        }
    };

    @Tool(description = "query username by userId")
    public String userQuery(@ToolParam(description = "id of user") String userId,
                            ToolContext toolContext) {
        log.info("requestId: {}", toolContext.getContext().get("requestId"));
        return userNameMap.getOrDefault(userId, "用户不存在");
    }
}