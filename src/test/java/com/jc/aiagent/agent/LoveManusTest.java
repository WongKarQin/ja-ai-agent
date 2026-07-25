package com.jc.aiagent.agent;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class LoveManusTest {
    @Resource
    private LoveManus loveManus;

    @Test
    void chat() {
        String userPrompt = """  
                我居住在深圳宝安区，请帮我找到 5 公里内合适的约会的地点，
                并结合一些网络图片，制定一份详细的行程计划，
                并以 PDF 格式输出
                """;
        String answer = loveManus.run(userPrompt);
        assertNotNull(answer);
    }
}