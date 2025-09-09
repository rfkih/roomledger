package com.roomledger.app.controller;

import com.roomledger.app.dto.RoomInquiryRequest;
import com.roomledger.app.exthandler.InvalidTransactionException;
import com.roomledger.app.model.Booking;
import com.roomledger.app.model.Room;
import com.roomledger.app.repository.RoomRepository;
import com.roomledger.app.util.ResponseCode;
import com.roomledger.app.util.ResponseService;
import com.roomledger.app.util.ResponseUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomRepository roomRepository;
    @Value("${application.code}")
    private String applicationCode;

    // constructor injection
    public RoomController(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    @PostMapping("/inquiry")
    public ResponseService inquiry(@RequestBody RoomInquiryRequest req) throws InvalidTransactionException {
        // parse optional status
        Room.Status st = null;
        if (req.status() != null && !req.status().isBlank()) {
            try {
                st = Room.Status.valueOf(req.status().trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                throw new InvalidTransactionException("Invalid status. Use AVAILABLE|OCCUPIED|MAINTENANCE");
            }
        }

        // validate dates if both present
        LocalDate start = req.startDate();
        LocalDate end   = req.endDate();
        if (start != null && end != null && start.isAfter(end)) {
            throw new InvalidTransactionException("startDate must be <= endDate");
        }

        List<Room> result = (start != null && end != null)
                ? roomRepository.inquiryWithDates(
                req.buildingId(), st, req.minPrice(), req.maxPrice(),
                start, end, Booking.Status.CANCELLED)
                : roomRepository.inquiryNoDates(
                req.buildingId(), st, req.minPrice(), req.maxPrice());

        return ResponseUtil.setResponse(
                HttpStatus.OK.value(),
                applicationCode,
                ResponseCode.SUCCESS.getCode(),
                ResponseCode.SUCCESS.getDescription(),
                result
        ).getBody();
    }
}

