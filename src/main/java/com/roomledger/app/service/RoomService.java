package com.roomledger.app.service;

import com.roomledger.app.dto.CreateRoomRequest;
import com.roomledger.app.dto.RoomResponse;
import com.roomledger.app.exthandler.DatabaseException;
import com.roomledger.app.exthandler.InvalidTransactionException;
import com.roomledger.app.model.Building;
import com.roomledger.app.model.Commons.Enum.RoomStatus;
import com.roomledger.app.model.Commons.Enum.RoomType;
import com.roomledger.app.model.Room;
import com.roomledger.app.repository.BuildingRepository;
import com.roomledger.app.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;
    private final BuildingRepository buildingRepository;

    @Transactional
    public RoomResponse createRoom(CreateRoomRequest req) throws InvalidTransactionException {
        if (roomRepository.existsByRoomNoAndBuildingId(req.roomNo(), req.buildingId())) {
            throw new InvalidTransactionException("Room Number already exists : " + req.roomNo());
        }

        Building building = null;
        if (req.buildingId() != null) {
            building = buildingRepository.findById(req.buildingId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Building not found"));
        }

        Room room = new Room();
        room.setBuilding(building);
        room.setRoomNo(req.roomNo());
        room.setMonthlyPrice(req.monthlyPrice());

        // defaults if null
        room.setStatus(req.status() != null ? req.status() : RoomStatus.AVAILABLE);
        room.setRoomType(req.roomType() != null ? req.roomType() : RoomType.STUDIO);
        room.setCapacity(req.capacity() != null ? req.capacity() : 1);
        room.setSizeM2(req.sizeM2()); // nullable OK

        Room saved = roomRepository.save(room);

        UUID buildingId = saved.getBuilding() != null ? saved.getBuilding().getId() : null;
        return new RoomResponse(
                saved.getId(),
                buildingId,
                saved.getRoomNo(),
                saved.getMonthlyPrice(),
                saved.getStatus(),
                saved.getRoomType(),
                saved.getCapacity(),
                saved.getSizeM2()
        );
    }
}
