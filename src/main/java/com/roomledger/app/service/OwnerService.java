package com.roomledger.app.service;

import com.roomledger.app.dto.AddWhatsappRequest;
import com.roomledger.app.dto.CreateOwnerRequest;
import com.roomledger.app.dto.OwnerResponse;
import com.roomledger.app.dto.OwnerWhatsappResponse;
import com.roomledger.app.exthandler.InvalidTransactionException;
import com.roomledger.app.model.Building;
import com.roomledger.app.model.Commons.Enum.OwnerStatus;
import com.roomledger.app.model.Commons.Enum.OwnerType;
import com.roomledger.app.model.Commons.Enum.OwnerWhatsappNumberStatus;
import com.roomledger.app.model.Owner;
import com.roomledger.app.model.OwnerWhatsappNumber;
import com.roomledger.app.repository.BuildingRepository;
import com.roomledger.app.repository.OwnerRepository;
import com.roomledger.app.repository.OwnerWhatsappNumberRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OwnerService {

    private final OwnerRepository ownerRepository;
    private final OwnerWhatsappNumberRepository whatsappNumberRepository;
    private final BuildingRepository buildingRepo;

    @Transactional
    public OwnerResponse register(CreateOwnerRequest req) {
        var owner = new Owner();

        String baseSlug = (req.slug() == null || req.slug().isBlank())
                ? slugify(req.displayName())
                : slugify(req.slug());
        String uniqueSlug = ensureUniqueSlug(baseSlug);

        owner.setSlug(uniqueSlug);
        owner.setDisplayName(req.displayName());
        owner.setType(req.type() != null ? req.type() : OwnerType.PERSON);
        owner.setStatus(req.status() != null ? req.status() : OwnerStatus.ACTIVE);
        owner.setTimezone(req.timezone() != null && !req.timezone().isBlank() ? req.timezone() : "Asia/Jakarta");
        owner.setXenditSubId(req.xenditSubId());

        var saved = ownerRepository.save(owner);
        return new OwnerResponse(
                saved.getId(),
                saved.getSlug(),
                saved.getDisplayName(),
                saved.getType(),
                saved.getStatus(),
                saved.getTimezone(),
                saved.getXenditSubId()
        );
    }

    @Transactional(readOnly = true)
    public Owner getOrThrow(java.util.UUID ownerId) {
        return ownerRepository.findById(ownerId)
                .orElseThrow(() -> new EntityNotFoundException("Owner not found"));
    }

    @Transactional
    public OwnerWhatsappResponse addNumber(UUID ownerId, AddWhatsappRequest req) throws InvalidTransactionException {
        Owner owner = ownerRepository.findById(ownerId)
                .orElseThrow(() -> new EntityNotFoundException("Owner not found"));

        Building building = null;
        if (req.buildingId() != null) {
            building = buildingRepo.findById(req.buildingId())
                    .orElseThrow(() -> new EntityNotFoundException("Building not found"));
        }

        // Optional: enforce unique phone_number_id if that’s your rule
         if (whatsappNumberRepository.existsByPhoneNumberId(req.phoneNumberId())) {
             throw new InvalidTransactionException("phoneNumberId already registered");
         }

        OwnerWhatsappNumber wa = new OwnerWhatsappNumber();
        wa.setOwner(owner);
        wa.setBuilding(building);
        wa.setPhoneNumber(req.phoneNumber());
        wa.setPhoneNumberId(req.phoneNumberId());
        wa.setAccessTokenEnc(req.accessTokenEnc()); // assume encrypted already if needed
        wa.setStatus(req.status() != null ? req.status() : OwnerWhatsappNumberStatus.ACTIVE);

        OwnerWhatsappNumber saved = whatsappNumberRepository.save(wa);

        return new OwnerWhatsappResponse(
                saved.getId(),
                saved.getOwner().getId(),
                saved.getBuilding() != null ? saved.getBuilding().getId() : null,
                saved.getPhoneNumber(),
                saved.getPhoneNumberId(),
                saved.getStatus()
        );
    }

    // ---------- helpers ----------
    private String ensureUniqueSlug(String base) {
        String s = truncate(base, 50);
        if (!ownerRepository.existsBySlugIgnoreCase(s)) return s;

        for (int i = 2; i < 1_000; i++) {
            String candidate = truncate(base, 50 - (("-" + i).length())) + "-" + i;
            if (!ownerRepository.existsBySlugIgnoreCase(candidate)) return candidate;
        }
        throw new IllegalStateException("Unable to generate unique slug");
    }

    private static String slugify(String in) {
        String s = in.trim().toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        return s.isEmpty() ? "owner" : s;
    }

    private static String truncate(String s, int max) {
        return (s.length() <= max) ? s : s.substring(0, max);
    }
}
