package com.roomledger.app.repository;

import com.roomledger.app.model.Room;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface RoomRepository  extends JpaRepository<Room, UUID> {

    @Query("""
  select r from Room r
  where (:buildingId is null or r.building.id = :buildingId)
    and (:status     is null or r.status = :status)
    and (:minPrice   is null or r.monthlyPrice >= :minPrice)
    and (:maxPrice   is null or r.monthlyPrice <= :maxPrice)
  order by r.roomNo
""")
    List<Room> inquiryNoDates(
            @Param("buildingId") UUID buildingId,
            @Param("status")     Room.Status status,
            @Param("minPrice")   BigDecimal minPrice,
            @Param("maxPrice")   BigDecimal maxPrice
    );

    @Query("""
  select r from Room r
  where (:buildingId is null or r.building.id = :buildingId)
    and (:status     is null or r.status = :status)
    and (:minPrice   is null or r.monthlyPrice >= :minPrice)
    and (:maxPrice   is null or r.monthlyPrice <= :maxPrice)
    and not exists (
          select 1 from Booking b
          where b.room = r
            and b.status <> :cancelled
            and b.startDate <= :endDate
            and (b.endDate is null or b.endDate >= :startDate)
    )
  order by r.roomNo
""")
    List<Room> inquiryWithDates(
            @Param("buildingId") UUID buildingId,
            @Param("status")     Room.Status status,
            @Param("minPrice")   BigDecimal minPrice,
            @Param("maxPrice")   BigDecimal maxPrice,
            @Param("startDate")  java.time.LocalDate startDate,
            @Param("endDate")    java.time.LocalDate endDate,
            @Param("cancelled")  com.roomledger.app.model.Booking.Status cancelled
    );
}
