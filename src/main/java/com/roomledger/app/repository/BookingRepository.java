package com.roomledger.app.repository;


import com.roomledger.app.model.Booking;
import com.roomledger.app.model.Commons.Enum.BookingStatus;
import com.roomledger.app.model.Commons.Enum.PaymentStatus;
import com.roomledger.app.model.Commons.Enum.PaymentType;
import com.roomledger.app.model.Commons.Enum.RoomStatus;
import com.roomledger.app.model.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;


@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {
    @Query(value = """
    select count(*) > 0 from bookings b
    where b.room_id = :roomId
      and b.status <> :cancelled
      and b.start_date <= :endDate
      and (b.end_date is null or b.end_date >= :startDate)
  """, nativeQuery = true)
    boolean existsOverlap(
            @Param("roomId") UUID roomId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("cancelled") String cancelled
    );


    @Query("""
  select b from Booking b
  where b.status = "ACTIVE"
    and b.autoRenew = true
""")
    List<Booking> findAllActiveAutoRenew();

    List<Booking> findByIdInAndStatus(Iterable<UUID> ids, BookingStatus status);

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
            BookingStatus activeStatus,
            RoomStatus occupiedStatus,
            PaymentType rentType,
            PaymentStatus verifiedStatus
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
            BookingStatus activeStatus,
            RoomStatus availableStatus,
            PaymentType rentType,
            PaymentStatus pendingStatus,
            PaymentStatus verifiedStatus
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
            BookingStatus activeStatus,
            RoomStatus availableStatus
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
