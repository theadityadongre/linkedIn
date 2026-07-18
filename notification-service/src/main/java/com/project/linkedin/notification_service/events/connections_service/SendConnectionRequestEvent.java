package com.project.linkedin.notification_service.events.connections_service;

import lombok.Data;

@Data
public class SendConnectionRequestEvent {
    private Long senderId;
    private Long receiverId;
}
