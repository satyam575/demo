package com.example.demo.services;

import com.example.demo.auth.dtos.JoinWeddingDto;
import com.example.demo.auth.dtos.JoinWeddingResponseDto;
import com.example.demo.auth.dtos.UserWeddingDto;
import com.example.demo.models.MemberRole;
import com.example.demo.models.MemberStatus;
import com.example.demo.models.Wedding;
import com.example.demo.models.WeddingMember;
import com.example.demo.repositories.WeddingMemberRepository;
import com.example.demo.repositories.WeddingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WeddingService {
    
    private final WeddingRepository weddingRepository;
    private final WeddingMemberRepository weddingMemberRepository;
    private final S3Service s3Service;
    
    @Transactional
    public JoinWeddingResponseDto joinWedding(UUID userId, String code, String displayName) {
        // Find wedding by code
        Optional<Wedding> weddingOpt = weddingRepository.findByCodeAndIsActive(code, true);
        if (weddingOpt.isEmpty()) {
            throw new IllegalArgumentException("Wedding not found or inactive");
        }
        
        Wedding wedding = weddingOpt.get();
        
        // Check if user is already a member
        Optional<WeddingMember> existingMember = weddingMemberRepository.findByWeddingIdAndUserId(wedding.getId(), userId);
        if (existingMember.isPresent()) {
            throw new IllegalArgumentException("User is already a member of this wedding");
        }
        
        // Create wedding member
        WeddingMember member = new WeddingMember(
                wedding.getId(),
                userId,
                displayName,
                MemberRole.GUEST,
                MemberStatus.ACCEPTED
        );
        
        WeddingMember savedMember = weddingMemberRepository.save(member);
        
        log.info("User {} joined wedding {} as member {}", userId, wedding.getId(), savedMember.getId());
        
        return new JoinWeddingResponseDto(
                savedMember.getId(),
                wedding.getId(),
                wedding.getTitle(),
                wedding.getCode(),
                savedMember.getRole(),
                savedMember.getStatus(),
                savedMember.getDisplayName(),
                savedMember.getJoinedAt(),
                true // isNewMember
        );
    }
    
    public List<UserWeddingDto> getUserWeddings(UUID userId) {
        List<WeddingMember> members = weddingMemberRepository.findByUserIdAndStatus(userId, MemberStatus.ACCEPTED);
        
        return members.stream()
                .map(member -> {
                    Wedding wedding = member.getWedding();
                    return new UserWeddingDto(
                            member.getId(), // member ID
                            wedding.getId(), // wedding ID
                            member.getUserId(), // user ID
                            wedding.getTitle(),
                            wedding.getCode(),
                            wedding.getPartner1(),
                            wedding.getPartner2(),
                            wedding.getCoverImageUrl(),
                            member.getRole(),
                            member.getStatus(),
                            member.getDisplayName(),
                            member.getJoinedAt(),
                            wedding.isActive()
                    );
                })
                .collect(Collectors.toList());
    }
    
    @Transactional
    public Wedding createWedding(String code, String title, String partner1, String partner2, String coverImageUrl) {
        // Check if code already exists
        if (weddingRepository.existsByCode(code)) {
            throw new IllegalArgumentException("Wedding code already exists");
        }
        
        // Create wedding
        Wedding wedding = new Wedding(code, title, partner1, partner2, coverImageUrl);
        Wedding savedWedding = weddingRepository.save(wedding);
        
        log.info("Created wedding {} with code {}", savedWedding.getId(), code);
        return savedWedding;
    }
    
    @Transactional
    public void deleteWedding(UUID weddingId, UUID userId) {
        // Find wedding
        Optional<Wedding> weddingOpt = weddingRepository.findById(weddingId);
        if (weddingOpt.isEmpty()) {
            throw new IllegalArgumentException("Wedding not found");
        }
        
        Wedding wedding = weddingOpt.get();
        
        // Check if user is the creator (assuming creator has ADMIN role)
        Optional<WeddingMember> memberOpt = weddingMemberRepository.findByWeddingIdAndUserId(weddingId, userId);
        if (memberOpt.isEmpty() || memberOpt.get().getRole() != MemberRole.ADMIN) {
            throw new IllegalArgumentException("User is not authorized to delete this wedding");
        }
        
        // Delete all wedding media from S3
        s3Service.deleteWeddingMedia(weddingId);
        
        // Mark wedding as inactive
        wedding.setActive(false);
        weddingRepository.save(wedding);
        
        log.info("Deleted wedding {} by user {}", weddingId, userId);
    }
    
    public boolean isUserMemberOfWedding(UUID userId, UUID weddingId) {
        return weddingMemberRepository.existsByWeddingIdAndUserIdAndStatus(weddingId, userId, MemberStatus.ACCEPTED);
    }
    
    public Optional<WeddingMember> getWeddingMember(UUID userId, UUID weddingId) {
        return weddingMemberRepository.findByWeddingIdAndUserId(weddingId, userId);
    }
}