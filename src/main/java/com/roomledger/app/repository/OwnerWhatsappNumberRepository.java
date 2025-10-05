package com.roomledger.app.repository;

import com.roomledger.app.model.OwnerWhatsappNumber;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OwnerWhatsappNumberRepository extends JpaRepository<OwnerWhatsappNumber, UUID> {
    boolean existsByPhoneNumberId(String phoneNumberId);
}
