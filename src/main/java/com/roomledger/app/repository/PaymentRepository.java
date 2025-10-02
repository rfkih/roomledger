package com.roomledger.app.repository;


import com.roomledger.app.model.Booking;
import com.roomledger.app.model.Commons.Enum.BookingStatus;
import com.roomledger.app.model.Payment;
import com.roomledger.app.model.Commons.Enum.PaymentStatus;
import com.roomledger.app.model.Commons.Enum.PaymentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findFirstByBookingIdAndTypeOrderByIdAsc(UUID bookingId, PaymentType type);

    List<Payment> findByBookingIdAndTypeAndStatus(UUID bookingId, PaymentType type, PaymentStatus status);
    List<Payment> findByBookingIdAndStatus(UUID bookingId,PaymentStatus status );
    Optional<Payment> findByPrId(String prId);
    Optional<Payment> findByReferenceId(String referenceId);
    List<Payment> findByBookingAndStatusAndChannelCode(Booking booking, PaymentStatus status, String channelCode);

    @Query("""
      select p
      from Payment p
      join fetch p.booking b
      left join fetch b.room r
      where p.id = :id
      """)
    Optional<Payment> findByIdWithBookingAndRoom(UUID id);

    boolean existsByBookingIdAndTypeAndStatus(UUID bookingId, PaymentType type, PaymentStatus status);

    List<Payment> findByBookingIdAndTypeInAndStatusInOrderByPaidAtDesc(
            UUID bookingId,
            Collection<PaymentType> types,
            Collection<PaymentStatus> statuses
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
      UPDATE roomledger.payments
         SET status = 'PAID',
             method = :method,
             reference = :reference,
             paid_at = :paidAt,
             updated_at = :now
       WHERE booking_id = :bookingId
         AND type = 'DEPOSIT'
         AND status = 'PENDING'
      """, nativeQuery = true)
    int markDepositPaidByBooking(@Param("bookingId") UUID bookingId,
                                 @Param("method") String method,
                                 @Param("reference") String reference,
                                 @Param("paidAt") LocalDateTime paidAt,
                                 @Param("now") LocalDateTime now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
      UPDATE roomledger.payments
         SET status = 'PAID',
             method = :method,
             reference = :reference,
             paid_at = :paidAt,
             updated_at = :now
       WHERE booking_id = :bookingId
         AND type = 'RENT'
         AND status = 'PENDING'
      """, nativeQuery = true)
    int markAllPendingRentPaidByBooking(@Param("bookingId") UUID bookingId,
                                        @Param("method") String method,
                                        @Param("reference") String reference,
                                        @Param("paidAt") LocalDateTime paidAt,
                                        @Param("now") LocalDateTime now);

    @Query(value = """
  select p.*
  from payments p
  join bookings b on b.id = p.booking_id
  join tenants  t on t.id = b.tenant_id
  left join rooms r on r.id = b.room_id
  where t.phone = :phone
    and p.status in ('WAITING_FOR_PAYMENT', 'PENDING')
    and b.status <> :bookingCancelled
  order by coalesce(p.period_month, b.start_date), p.created_at
""", nativeQuery = true)
    List<Payment> findActiveBillingByPhoneFetch(
            @Param("phone") String phone,
            @Param("bookingCancelled") String bookingCancelled
    );

    @Query(value = """
  select p.*
  from payments p
  where p.type = :type
    and p.status = :status
    and p.created_at < :before
""", nativeQuery = true)
    List<Payment> findByTypeAndStatusAndCreatedAtBefore(
            @Param("type") String type,
            @Param("status") String status,
            @Param("before") LocalDateTime before
    );



    @Modifying
    @Query(value = """
    UPDATE roomledger.payments
       SET status = 'CANCELLED',
           updated_at = now()
     WHERE booking_id = :bookingId
       AND status = 'PENDING'
    """, nativeQuery = true)
    int cancelPendingByBooking(@Param("bookingId") UUID bookingId);

    Optional<Payment> findByBookingIdAndTypeAndPeriodMonth(UUID bookingId, PaymentType type, LocalDate periodMonth);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update Payment p
           set p.status    = :cancelledStatus,
               p.updatedAt = :updatedAt
         where p.booking.id = :bookingId
           and p.type       = :rentType
           and p.status     = :pendingStatus
        """)
    int cancelPendingRentByBooking(UUID bookingId,
                                   PaymentType rentType,
                                   PaymentStatus pendingStatus,
                                   PaymentStatus cancelledStatus,
                                   LocalDateTime updatedAt);

    @Query("""
    select p from Payment p
    where p.booking.id in :bookingIds
      and p.status = "PENDING"
    order by p.periodMonth nulls last, p.createdAt
  """)
    List<Payment> findPendingByBookingIds(@Param("bookingIds") Collection<UUID> bookingIds);


}

