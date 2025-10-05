package com.roomledger.app.service;

import com.roomledger.app.dto.BuildingResponse;
import com.roomledger.app.dto.CreateBuildingRequest;
import com.roomledger.app.model.Building;
import com.roomledger.app.model.Commons.Enum.BuildingStatus;
import com.roomledger.app.model.Owner;
import com.roomledger.app.model.OwnerWhatsappNumber;
import com.roomledger.app.repository.BuildingRepository;
import com.roomledger.app.repository.OwnerRepository;
import com.roomledger.app.repository.OwnerWhatsappNumberRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BuildingService {

    private final OwnerRepository ownerRepo;
    private final BuildingRepository buildingRepo;
    private final OwnerWhatsappNumberRepository waRepo;

    @Transactional
    public BuildingResponse create(UUID ownerId, CreateBuildingRequest req) {
        Owner owner = ownerRepo.findById(ownerId)
                .orElseThrow(() -> new EntityNotFoundException("Owner not found"));

        String code = req.code().trim();
        // pre-check for nicer error than DB unique violation
        if (buildingRepo.existsByOwnerIdAndCodeIgnoreCase(ownerId, code)) {
            throw new DataIntegrityViolationException("Building code already exists for this owner");
        }

        Building b = new Building();
        b.setOwner(owner);
        b.setCode(code);
        b.setName(req.name().trim());
        b.setAddress(req.address());
        b.setStatus(req.status() != null ? req.status() : BuildingStatus.ACTIVE);

        // Optional default WA binding via FK (enforced same-owner by composite FK if you added it)
        OwnerWhatsappNumber defaultWa = null;
        if (req.defaultWhatsappId() != null) {
            defaultWa = waRepo.findById(req.defaultWhatsappId())
                    .orElseThrow(() -> new EntityNotFoundException("WhatsApp number not found"));
            // App-level guard (DB can also enforce via composite FK)
            if (!defaultWa.getOwner().getId().equals(ownerId)) {
                throw new IllegalArgumentException("WhatsApp number belongs to a different owner");
            }
            b.setDefaultWhatsapp(defaultWa);
        }

        Building saved = buildingRepo.save(b);

        return new BuildingResponse(
                saved.getId(),
                owner.getId(),
                saved.getCode(),
                saved.getName(),
                saved.getAddress(),
                saved.getStatus(),
                saved.getDefaultWhatsapp() != null ? saved.getDefaultWhatsapp().getId() : null,
                saved.getDefaultWhatsapp() != null ? saved.getDefaultWhatsapp().getPhoneNumber() : null
        );
    }
}
