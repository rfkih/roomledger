package com.roomledger.app.controller;

import com.roomledger.app.dto.RegisterTenantRequest;
import com.roomledger.app.exthandler.InvalidAccountException;
import com.roomledger.app.exthandler.InvalidTransactionException;
import com.roomledger.app.model.Tenant;
import com.roomledger.app.repository.TenantRepository;
import com.roomledger.app.util.ResponseCode;
import com.roomledger.app.util.ResponseService;
import com.roomledger.app.util.ResponseUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Optional;

@RestController
@RequestMapping("/api/tenants")
public class TenantController {

    private final TenantRepository tenantRepository;

    @Value("${application.code}")
    private String applicationCode;

    public TenantController(TenantRepository repo) {
        this.tenantRepository = repo;
    }

    @PostMapping("/register")
    public ResponseService register(@Valid @RequestBody RegisterTenantRequest req) throws InvalidTransactionException {
        if (tenantRepository.findByPhone(req.phone()).isPresent()) {
            throw new InvalidAccountException("Phone already registered");
        }

        Tenant t = new Tenant();
        t.setName(req.name());
        t.setPhone(req.phone());
        t.setNo_id(req.no_id());
        t.setGender(req.gender());
        t = tenantRepository.save(t);

        return ResponseUtil.setResponse(
                HttpStatus.OK.value(),
                applicationCode,
                ResponseCode.SUCCESS.getCode(),
                ResponseCode.SUCCESS.getDescription(),
                t
                ).getBody();
    }


    @GetMapping("/{id}")
    public ResponseService get(@PathVariable java.util.UUID id) {
        Tenant tenant = tenantRepository.findById(id).orElse(null);

        return ResponseUtil.setResponse(
                HttpStatus.OK.value(),
                applicationCode,
                ResponseCode.SUCCESS.getCode(),
                ResponseCode.SUCCESS.getDescription(),
                tenant
        ).getBody();
    }

    @GetMapping("/by-phone")
    public ResponseService getByPhone(@RequestParam String phone) {
        Optional<Tenant> t = tenantRepository.findByPhone(phone);
        return ResponseUtil.setResponse(
                HttpStatus.OK.value(),
                applicationCode,
                ResponseCode.SUCCESS.getCode(),
                ResponseCode.SUCCESS.getDescription(),
                t
        ).getBody();
    }



    @GetMapping
    public java.util.List<Tenant> list() { return tenantRepository.findAll(); }
}
