package com.roomledger.app.repository;

import com.roomledger.app.model.WhatsappUserSession;
import com.roomledger.app.model.WhatsappUser;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface WhatsappUserSessionRepository extends JpaRepository<WhatsappUserSession, UUID> {
    Optional<WhatsappUserSession> findByUserAndWaPhoneId(WhatsappUser user, UUID waPhoneId);
}
