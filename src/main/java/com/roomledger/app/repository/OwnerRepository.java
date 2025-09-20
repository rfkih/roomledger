package com.roomledger.app.repository;

import com.roomledger.app.model.Owner;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface OwnerRepository extends JpaRepository<Owner, UUID> {

}

