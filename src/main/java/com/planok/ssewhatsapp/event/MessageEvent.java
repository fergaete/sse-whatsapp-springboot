package com.planok.ssewhatsapp.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
public final class MessageEvent implements Serializable {

    private UUID id;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean deleted;
    private String sender;
    private String messageId;
    private String parentMessageId;
    private String errorCode;
    private String errorMessage;
    private String type;
    private String content;
    private boolean outgoing;
    private String status;
    private boolean processed;
    private Conversation conversation;

    @Data
    @ToString
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Conversation implements Serializable {
        private UUID id;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private boolean deleted;
        private String conversationId;
        private LocalDateTime expirationDate;
        private String pricing;
        private String type;
        private String metadata;
        private boolean active;
        private Environment environment;
        private String participants;
        private String lastMessage;
    }

    @Data
    @ToString
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Environment implements Serializable {
        private UUID id;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private boolean deleted;
        private String name;
        private String code;
        private String phone;
        private String token;
        private String whatsappBussinessId;
        private String whatsappPhoneNumberId;
        private String idApp;
        private String key;
        private boolean active;
        private String tenant;
    }
}