package com.roomledger.app.repository;

import com.roomledger.app.model.MediaLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MediaLinkRepository extends JpaRepository<MediaLink, UUID> {}
