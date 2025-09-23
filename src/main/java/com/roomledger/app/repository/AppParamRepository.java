package com.roomledger.app.repository;

import com.roomledger.app.model.AppParam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface AppParamRepository extends JpaRepository<AppParam, String> {}