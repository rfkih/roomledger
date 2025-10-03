package com.roomledger.app.repository;

import com.roomledger.app.model.Media;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MediaRepository extends JpaRepository<Media, UUID> {
    Optional<Media> findByStorageKey(String storageKey);
}
