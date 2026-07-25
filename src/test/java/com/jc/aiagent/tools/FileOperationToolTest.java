package com.jc.aiagent.tools;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FileOperationToolTest {

    @Test
    void writeFile() {
        String result = new FileOperationTool().writeFile(
                "恋爱大师.txt",
                "https://www.codefather.cn 程序员编程学习交流社区"
        );
        System.out.println(result);
        assertNotNull(result);
    }

    @Test
    void readFile() {
        String result = new FileOperationTool().readFile("恋爱大师.txt");
        assertNotNull(result);
        System.out.println(result);
    }
}