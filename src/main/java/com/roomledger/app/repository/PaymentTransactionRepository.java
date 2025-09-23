package com.roomledger.app.repository;

import com.roomledger.app.model.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, UUID> {
    Optional<PaymentTransaction> findByProviderPaymentId(String providerPaymentId);

    @Query(value = """
      SELECT pt.*
      FROM payment_transactions pt
      WHERE pt.reference_id = :referenceId
        AND pt.status = :status
        AND pt.payment_id = :paymentId
      """, nativeQuery = true)
    Optional<PaymentTransaction> findAllByRefStatusAndPaymentId(
            @Param("referenceId") String referenceId,
            @Param("status") String status,          // or use the enum type if stored as TEXT
            @Param("paymentId") UUID paymentId
    );

}