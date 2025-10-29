package com.example.demo.services;

import com.example.demo.auth.dtos.ChallengeDto;
import com.example.demo.models.Challenge;
import com.example.demo.models.ChallengeParticipation;
import com.example.demo.models.Post;
import com.example.demo.repositories.ChallengeParticipationRepository;
import com.example.demo.repositories.ChallengeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChallengeService {
    private final ChallengeRepository challengeRepository;
    private final ChallengeParticipationRepository participationRepository;

    private static final Pattern HASH_TAG = Pattern.compile("#([A-Za-z0-9_]{2,32})");

    public static String canonicalTag(String raw) {
        if (raw == null) return null;
        String t = raw.trim();
        if (!t.startsWith("#")) t = "#" + t;
        return t.toLowerCase(Locale.ROOT);
    }

    @Transactional(readOnly = true)
    public List<ChallengeDto> list(UUID weddingId, UUID eventId, boolean includeCounts, boolean activeOnly) {
        List<Challenge> list = (eventId != null)
                ? challengeRepository.findByWeddingIdAndEventIdAndActiveTrueOrderByCreatedAtDesc(weddingId, eventId)
                : challengeRepository.findByWeddingIdAndActiveTrueOrderByCreatedAtDesc(weddingId);
        List<ChallengeDto> out = new ArrayList<>();
        for (Challenge c : list) {
            ChallengeDto dto = toDto(c);
            if (includeCounts) {
                long count = participationRepository.countByChallengeId(c.getId());
                dto.setCounts(new ChallengeDto.Counts(count));
            }
            out.add(dto);
        }
        return out;
    }

    @Transactional
    public ChallengeDto create(UUID weddingId, String tag, String title, String description, UUID eventId, Boolean active, Instant startAt, Instant endAt) {
        String canonical = canonicalTag(tag);
        Challenge c = new Challenge();
        c.setWeddingId(weddingId);
        c.setEventId(eventId);
        c.setTag(canonical);
        c.setTitle(title);
        c.setDescription(description);
        c.setActive(active == null ? true : active);
        c.setStartAt(startAt);
        c.setEndAt(endAt);
        Challenge saved = challengeRepository.save(c);
        return toDto(saved);
    }

    @Transactional
    public ChallengeDto update(UUID id, String tag, String title, String description, UUID eventId, Boolean active, Instant startAt, Instant endAt) {
        Challenge c = challengeRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Challenge not found"));
        if (tag != null) c.setTag(canonicalTag(tag));
        if (title != null) c.setTitle(title);
        if (description != null) c.setDescription(description);
        if (active != null) c.setActive(active);
        c.setEventId(eventId);
        c.setStartAt(startAt);
        c.setEndAt(endAt);
        Challenge saved = challengeRepository.save(c);
        return toDto(saved);
    }

    @Transactional
    public void delete(UUID id) {
        challengeRepository.deleteById(id);
    }

    @Transactional
    public void recordParticipationForPost(Post post) {
        String text = Optional.ofNullable(post.getContentText()).orElse("");
        if (text.isBlank()) return;
        Set<String> tags = new HashSet<>();
        Matcher m = HASH_TAG.matcher(text);
        while (m.find()) {
            tags.add(canonicalTag(m.group(0)));
        }
        if (tags.isEmpty()) return;

        // Load active challenges and match
        List<Challenge> candidates = challengeRepository.findByWeddingIdAndActiveTrueOrderByCreatedAtDesc(post.getWeddingId());
        for (Challenge c : candidates) {
            if (!tags.contains(c.getTag())) continue;
            // If challenge is event-scoped, require match with post.eventId
            if (c.getEventId() != null && !c.getEventId().equals(post.getEventId())) continue;
            if (!participationRepository.existsByChallengeIdAndPostId(c.getId(), post.getId())) {
                ChallengeParticipation p = new ChallengeParticipation();
                p.setChallengeId(c.getId());
                p.setPostId(post.getId());
                p.setUserId(post.getAuthorUserId());
                participationRepository.save(p);
            }
        }
    }

    private static ChallengeDto toDto(Challenge c) {
        return new ChallengeDto(
                c.getId(), c.getWeddingId(), c.getEventId(), c.getTag(), c.getTitle(), c.getDescription(),
                c.isActive(), c.getStartAt(), c.getEndAt(), null
        );
    }
}

