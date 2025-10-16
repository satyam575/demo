package com.example.demo.repositories;

import java.util.UUID;

public interface PostLikeCount {
    UUID getPostId();
    Long getLikeCount();
}
