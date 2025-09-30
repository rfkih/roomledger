package com.roomledger.app.repository;

import com.roomledger.app.model.WebhookInbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface WebhookInboxRepository extends JpaRepository<WebhookInbox, UUID> {

    @Modifying
    @Query(value = """
      INSERT INTO roomledger.webhook_inbox
        (id, provider, payment_id, payload, processed, received_at,
         created_at, created_by, updated_at, updated_by)
      VALUES
        (:id, :provider, :paymentId, CAST(:payloadJson AS jsonb), false, :receivedAt,
         now(), :by, now(), :by)
      ON CONFLICT (provider, event_id) DO NOTHING
      """, nativeQuery = true)
    int insertIgnore(@Param("id") UUID id,
                     @Param("provider") String provider,
                     @Param("paymentId") String paymentId,
                     @Param("payloadJson") String payloadJson,
                     @Param("receivedAt") OffsetDateTime receivedAt,
                     @Param("by") String by);

    Optional<WebhookInbox> findByProviderAndPaymentId(String provider, String paymentId);

    // Mark processed atomically; returns 1 iff it flipped from false→true
    @Modifying
    @Query(value = """
      UPDATE roomledger.webhook_inbox
      SET processed = true, updated_at = now(), updated_by = :by
      WHERE provider = :provider AND event_id = :eventId AND processed = false
      """, nativeQuery = true)
    int markProcessed(@Param("provider") String provider,
                      @Param("paymentId") String paymentId,
                      @Param("by") String by);
}
