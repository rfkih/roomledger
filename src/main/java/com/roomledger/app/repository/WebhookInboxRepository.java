package com.roomledger.app.repository;

import com.roomledger.app.model.WebhookInbox;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WebhookInboxRepository extends JpaRepository<WebhookInbox, UUID> {}
