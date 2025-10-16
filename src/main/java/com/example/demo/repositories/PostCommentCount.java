package com.example.demo.repositories;

import java.util.UUID;

public interface PostCommentCount {
    UUID getPostId();
    Long getCommentCount();
}
