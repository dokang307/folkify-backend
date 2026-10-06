package com.folkify.instrument.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import com.folkify.instrument.entity.Song;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SongRepository extends JpaRepository<Song, UUID> {
    List<Song> findByInstrumentIdOrderByOrderIndexAsc(UUID instrumentId);

    /** Nạp sẵn instrument — dùng ngoài transaction (open-in-view tắt) mà không bị LazyInitializationException. */
    @Query("SELECT s FROM Song s JOIN FETCH s.instrument WHERE s.id = :id")
    Optional<Song> findWithInstrumentById(@Param("id") UUID id);
}
