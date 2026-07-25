package com.jc.aiagent.tools;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TerminalOperationToolTest {

    @Test
    void executeTerminalCommand() {
        String result = new TerminalOperationTool().executeTerminalCommand("echo hello world!");
        System.out.println("result = " + result);
        Assertions.assertNotNull(result);
    }

}

