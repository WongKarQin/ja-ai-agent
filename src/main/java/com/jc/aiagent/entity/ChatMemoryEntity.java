package com.jc.aiagent.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMemoryEntity {
    private Long id;
    private String chatId;
    private Long userId;
    private String content;
    private String type;//USER /ASSISTANt / SYSTEM/ TOOL
    private LocalDateTime timeStamp;
}
