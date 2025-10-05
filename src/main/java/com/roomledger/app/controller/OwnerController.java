package com.roomledger.app.controller;

import com.roomledger.app.dto.AddWhatsappRequest;
import com.roomledger.app.dto.CreateOwnerRequest;
import com.roomledger.app.dto.OwnerResponse;
import com.roomledger.app.dto.OwnerWhatsappResponse;
import com.roomledger.app.exthandler.InvalidTransactionException;
import com.roomledger.app.service.OwnerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/owners")
@RequiredArgsConstructor
public class OwnerController {

    private final OwnerService ownerService;

    // POST /api/owners : register owner
    @PostMapping
    public ResponseEntity<OwnerResponse> registerOwner(@Valid @RequestBody CreateOwnerRequest req) {
        var out = ownerService.register(defaults(req));
        return ResponseEntity.created(URI.create("/api/owners/" + out.id())).body(out);
    }

    // POST /api/owners/{ownerId}/whatsapp : add WA number
    @PostMapping("/{ownerId}/whatsapp")
    public ResponseEntity<OwnerWhatsappResponse> addWhatsapp(
            @PathVariable UUID ownerId,
            @Valid @RequestBody AddWhatsappRequest req
    ) throws InvalidTransactionException {
        var out = ownerService.addNumber(ownerId, req);
        return ResponseEntity.created(URI.create("/api/owners/" + ownerId + "/whatsapp/" + out.id())).body(out);
    }

    // Provide sensible defaults if not sent
    private CreateOwnerRequest defaults(CreateOwnerRequest in) {
        String tz = (in.timezone() == null || in.timezone().isBlank()) ? "Asia/Jakarta" : in.timezone();
        return new CreateOwnerRequest(
                in.slug(),
                in.displayName(),
                in.type(),
                in.status(),
                tz,
                in.xenditSubId()
        );
    }
}