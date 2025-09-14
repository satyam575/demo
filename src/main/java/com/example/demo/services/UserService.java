package com.example.demo.services;

import com.example.demo.models.User;
import com.example.demo.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    
    private final UserRepository userRepository;
    
    @Transactional
    public User upsertUserByPhone(String phone) {
        Optional<User> existingUser = userRepository.findByPhone(phone);
        
        if (existingUser.isPresent()) {
            User user = existingUser.get();
            user.markPhoneVerified();
            User savedUser = userRepository.save(user);
            log.info("Updated existing user: {} with phone: {}", savedUser.getId(), phone);
            return savedUser;
        } else {
            User newUser = new User(phone);
            newUser.markPhoneVerified();
            User savedUser = userRepository.save(newUser);
            log.info("Created new user: {} with phone: {}", savedUser.getId(), phone);
            return savedUser;
        }
    }
    
    public Optional<User> getUserById(UUID userId) {
        return userRepository.findById(userId);
    }
    
    public Optional<User> getUserByPhone(String phone) {
        return userRepository.findByPhone(phone);
    }
    
    public boolean isNewUser(String phone) {
        return !userRepository.existsByPhone(phone);
    }
    
    @Transactional
    public User updateUser(UUID userId, String name, String email, String username, String avatarUrl) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        
        if (name != null) user.updateName(name);
        if (email != null) user.updateEmail(email);
        if (username != null) user.updateUsername(username);
        if (avatarUrl != null) user.updateAvatarUrl(avatarUrl);
        
        return userRepository.save(user);
    }
    
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
    
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }
    
    @Transactional
    public User createUser(String phone) {
        User newUser = new User(phone);
        User savedUser = userRepository.save(newUser);
        log.info("Created new user: {} with phone: {}", savedUser.getId(), phone);
        return savedUser;
    }
    
    @Transactional
    public User upsertUserByEmail(String email) {
        Optional<User> existingUser = userRepository.findByEmail(email);
        
        if (existingUser.isPresent()) {
            User user = existingUser.get();
            user.markEmailVerified();
            User savedUser = userRepository.save(user);
            log.info("Updated existing user: {} with email: {}", savedUser.getId(), email);
            return savedUser;
        } else {
            User newUser = new User(email);
            newUser.markEmailVerified();
            User savedUser = userRepository.save(newUser);
            log.info("Created new user: {} with email: {}", savedUser.getId(), email);
            return savedUser;
        }
    }
    
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }
    
    public boolean isNewUserByEmail(String email) {
        return !userRepository.existsByEmail(email);
    }
}
