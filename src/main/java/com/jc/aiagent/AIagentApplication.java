package com.jc.aiagent;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.jc.aiagent.mapper")
public class AIagentApplication {

    public static void main(String[] args) {
        SpringApplication.run(AIagentApplication.class, args);
    }

}