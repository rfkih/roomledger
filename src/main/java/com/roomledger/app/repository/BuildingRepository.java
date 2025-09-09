package com.roomledger.app.repository;


import com.roomledger.app.model.Building;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BuildingRepository  extends JpaRepository<Building, UUID> {}
