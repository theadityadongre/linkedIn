package com.project.linkedin.notification_service.events.connections_service;

import lombok.Data;

@Data
public class AcceptConnectionRequestEvent {
    private Long senderId;
    private Long receiverId;
}
