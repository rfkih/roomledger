package com.roomledger.app.controller;

import com.roomledger.app.model.Building;
import com.roomledger.app.repository.BuildingRepository;
import com.roomledger.app.util.ResponseCode;
import com.roomledger.app.util.ResponseService;
import com.roomledger.app.util.ResponseUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/buildings")
public class BuildingController {
    private final BuildingRepository repo;
    @Value("${application.code}")
    private String applicationCode;
    public BuildingController(BuildingRepository repo) { this.repo = repo; }

    @GetMapping
    public List<Building> list() { return repo.findAll(); }

    @GetMapping("/{id}")
    public ResponseService get(@PathVariable UUID id) {
        Optional<Building> building = repo.findById(id);

        return ResponseUtil.setResponse(
                HttpStatus.OK.value(),
                applicationCode,
                ResponseCode.SUCCESS.getCode(),
                ResponseCode.SUCCESS.getDescription(),
                building
                ).getBody();
    }
}

