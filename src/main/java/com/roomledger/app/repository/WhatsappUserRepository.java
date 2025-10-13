package com.roomledger.app.repository;

import com.roomledger.app.model.WhatsappUser;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface WhatsappUserRepository extends JpaRepository<WhatsappUser, UUID> {
    Optional<WhatsappUser> findByWaId(String waId);
}
