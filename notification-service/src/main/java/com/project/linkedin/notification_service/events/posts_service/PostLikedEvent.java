package com.project.linkedin.notification_service.events.posts_service;

import lombok.Data;

@Data
public class PostLikedEvent {
    Long postId;
    Long creatorId;
    Long likedByUserId;
}
