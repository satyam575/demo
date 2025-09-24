package com.example.demo.repositories;

import com.example.demo.models.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface EventRepository extends JpaRepository<Event, UUID> {
    
    List<Event> findByWeddingIdOrderByStartTimeAsc(UUID weddingId);
    
    @Query("SELECT e FROM Event e WHERE e.weddingId = :weddingId AND e.startTime >= :startTime ORDER BY e.startTime ASC")
    List<Event> findByWeddingIdAndStartTimeAfterOrderByStartTimeAsc(
            @Param("weddingId") UUID weddingId, 
            @Param("startTime") Instant startTime);
    
    @Query("SELECT e FROM Event e WHERE e.weddingId = :weddingId AND e.startTime BETWEEN :startTime AND :endTime ORDER BY e.startTime ASC")
    List<Event> findByWeddingIdAndStartTimeBetweenOrderByStartTimeAsc(
            @Param("weddingId") UUID weddingId, 
            @Param("startTime") Instant startTime, 
            @Param("endTime") Instant endTime);
    
    List<Event> findByVenueId(UUID venueId);
}
