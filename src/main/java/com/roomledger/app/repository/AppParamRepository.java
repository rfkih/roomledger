package com.roomledger.app.repository;

import com.roomledger.app.model.AppParam;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppParamRepository extends JpaRepository<AppParam, String> {}