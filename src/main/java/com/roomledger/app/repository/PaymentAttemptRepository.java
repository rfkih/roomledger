package com.roomledger.app.repository;

import com.roomledger.app.model.PaymentAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface PaymentAttemptRepository extends JpaRepository<PaymentAttempt, UUID> {
    @Query(value = """
      SELECT pa.*
      FROM payment_attempts pa
      WHERE pa.booking_id = :bookingId
        AND pa.status = :status
      """, nativeQuery = true)
    Optional<PaymentAttempt> findAllByBookingIdAndStatus(
            @Param("bookingId") String bookingId,
            @Param("status") String status
    );

}