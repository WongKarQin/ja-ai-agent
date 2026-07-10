package com.jc.aiagent;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = {
        // 为了便于开发调试和部署，取消数据库自动配置，需要使用 PgVector 时把 DataSourceAutoConfiguration.class 删除
        //DataSourceAutoConfiguration.class
        //为了使用特定的Embedding模型，需要：1.查看官方文档去修改PgVectorStoreConfig的配置维度2.启动类要排除掉自动加载。
        PgVectorStoreAutoConfiguration.class
})
public class AIagentApplication {

    public static void main(String[] args) {
        SpringApplication.run(AIagentApplication.class, args);
    }

}