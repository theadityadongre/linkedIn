package com.project.linkedin.notification_service.events.posts_service;

import lombok.Data;

@Data
public class PostCreatedEvent {
    Long creatorId;
    String content;
    Long postId;
}
