package com.roomledger.app.repository;


import com.roomledger.app.model.Booking;
import com.roomledger.app.model.Payment;
import com.roomledger.app.model.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID> {
    @Query("""
    select count(b) > 0 from Booking b
    where b.room.id = :roomId
      and b.status <> :cancelled
      and b.startDate <= :endDate
      and (b.endDate is null or b.endDate >= :startDate)
  """)
    boolean existsOverlap(
            @Param("roomId") UUID roomId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("cancelled") Booking.Status cancelled
    );

    @Query("""
  select b from Booking b
  where b.status = "ACTIVE"
    and b.autoRenew = true
""")
    List<Booking> findAllActiveAutoRenew();

    List<Booking> findByIdInAndStatus(Iterable<UUID> ids, Booking.Status status);

    // Candidates to OCCUPIED (has VERIFIED rent)
    @Query("""
        select b
          from Booking b
          join fetch b.room r
         where b.status = :activeStatus
           and b.startDate = :startDate
           and r.status <> :occupiedStatus
           and exists (
                 select 1 from Payment p
                  where p.booking = b
                    and p.type   = :rentType
                    and p.status = :verifiedStatus
            )
        """)
    List<Booking> findActiveStartingTodayWithVerifiedRentAndRoomNotOccupied(
            LocalDate startDate,
            Booking.Status activeStatus,
            Room.Status occupiedStatus,
            Payment.Type rentType,
            Payment.Status verifiedStatus
    );

    // Candidates to AVAILABLE (only PENDING rent, no VERIFIED)
    @Query("""
        select b
          from Booking b
          join fetch b.room r
         where b.status = :activeStatus
           and b.startDate = :startDate
           and r.status <> :availableStatus
           and exists (
                 select 1 from Payment p
                  where p.booking = b
                    and p.type   = :rentType
                    and p.status = :pendingStatus
            )
           and not exists (
                 select 1 from Payment vp
                  where vp.booking = b
                    and vp.type   = :rentType
                    and vp.status = :verifiedStatus
            )
        """)
    List<Booking> findActiveStartingTodayWithPendingRentOnlyAndRoomNotAvailable(
            LocalDate startDate,
            Booking.Status activeStatus,
            Room.Status availableStatus,
            Payment.Type rentType,
            Payment.Status pendingStatus,
            Payment.Status verifiedStatus
    );

    /** Bookings that already ended (endDate < today), still ACTIVE, and room not AVAILABLE. */
    @Query("""
        select b
          from Booking b
          join fetch b.room r
         where b.status     = :activeStatus
           and b.endDate    < :today
           and r.status     <> :availableStatus
        """)
    List<Booking> findActiveEndedWithRoomNotAvailable(
            LocalDate today,
            Booking.Status activeStatus,
            Room.Status availableStatus
    );

    @Query("""
    select b from Booking b
      join fetch b.tenant t
      left join fetch b.room r
    where t.phone = :phone                                    
      and b.status = "ACTIVE"
      and (b.endDate is null or b.endDate >= :today)
  """)
    List<Booking> findActiveByPhoneFetch(
            @Param("phone") String phone,
            @Param("today") LocalDate today
    );


}
