package com.roomledger.app.controller;

import com.roomledger.app.dto.BuildingResponse;
import com.roomledger.app.dto.CreateBuildingRequest;
import com.roomledger.app.model.Building;
import com.roomledger.app.repository.BuildingRepository;
import com.roomledger.app.service.BuildingService;
import com.roomledger.app.util.ResponseCode;
import com.roomledger.app.util.ResponseService;
import com.roomledger.app.util.ResponseUtil;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/buildings")
public class BuildingController {
    private final BuildingRepository repo;
    private final BuildingService buildingService;
    @Value("${application.code}")
    private String applicationCode;
    public BuildingController(BuildingRepository repo, BuildingService buildingService) { this.repo = repo;
        this.buildingService = buildingService;
    }

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

    @PostMapping("/{ownerId}/create")
    public ResponseEntity<BuildingResponse> create(
            @PathVariable UUID ownerId,
            @Valid @RequestBody CreateBuildingRequest req
    ) {
        var out = buildingService.create(ownerId, req);
        return ResponseEntity.created(URI.create("/api/owners/" + ownerId + "/buildings/" + out.id())).body(out);
    }
}

