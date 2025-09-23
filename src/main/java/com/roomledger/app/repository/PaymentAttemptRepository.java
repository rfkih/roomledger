package com.roomledger.app.repository;

import com.roomledger.app.model.PaymentAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PaymentAttemptRepository extends JpaRepository<PaymentAttempt, UUID> {

}