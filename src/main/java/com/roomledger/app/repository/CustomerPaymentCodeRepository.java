package com.roomledger.app.repository;

import com.roomledger.app.model.CustomerPaymentCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface CustomerPaymentCodeRepository  extends JpaRepository<CustomerPaymentCode, UUID> {
    @Query("""
     select c
       from CustomerPaymentCode c
      where c.owner.id = :ownerId
        and (
              (:buildingId is null and c.building is null)
              or (c.building.id = :buildingId)
            )
        and c.customerId  = :customerId
        and c.channelCode = :channelCode
        and c.status      = com.roomledger.app.model.CustomerPaymentCode.Status.ACTIVE
     """)
    Optional<CustomerPaymentCode> findActiveByOwnerAndBuildingAndCustomerAndChannel(
            @Param("ownerId") UUID ownerId,
            @Param("buildingId") UUID buildingId,     // pass null if code is shared across owner
            @Param("customerId") String customerId,   // <-- use String (your schema uses VARCHAR)
            @Param("channelCode") String channelCode
    );

}