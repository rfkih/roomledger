package com.roomledger.app.controller;

import com.roomledger.app.dto.OrderRequest;
import com.roomledger.app.dto.RoomPhotoDto;
import com.roomledger.app.service.RoomPhotoAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/rooms/{roomId}/photos")
@RequiredArgsConstructor
public class RoomPhotoAdminController {

    private final RoomPhotoAdminService service;


    @GetMapping
    public ResponseEntity<List<RoomPhotoDto>> list(
            @PathVariable UUID roomId,
            @RequestParam(defaultValue = "ROOM_PHOTO") String purpose,
            @RequestParam(defaultValue = "false") boolean signed,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) Integer offset
    ) {
        var out = service.listPhotos(roomId, purpose, signed, limit, offset);
        return ResponseEntity.ok(out);
    }

    // DELETE /api/rooms/{roomId}/photos/{mediaId}?deleteObject=false
    @DeleteMapping("/{mediaId}")
    public ResponseEntity<Void> deleteOne(
            @PathVariable UUID roomId,
            @PathVariable UUID mediaId,
            @RequestParam(defaultValue = "false") boolean deleteObject,
            @RequestParam(defaultValue = "ROOM_PHOTO") String purpose
    ) throws Exception {
        service.deletePhoto(roomId, mediaId, purpose, deleteObject);
        return ResponseEntity.noContent().build();
    }

    // PATCH /api/rooms/{roomId}/photos/primary
    @PatchMapping("/main")
    public ResponseEntity<Void> setPrimary(
            @PathVariable UUID roomId,
            @RequestParam UUID mediaId,
            @RequestParam(defaultValue = "ROOM_PHOTO") String purpose
    ) {
        service.setMain(roomId, mediaId, purpose);
        return ResponseEntity.noContent().build();
    }

    // PATCH /api/rooms/{roomId}/photos/order
    @PatchMapping("/order")
    public ResponseEntity<Void> reorder(
            @PathVariable UUID roomId,
            @RequestBody OrderRequest body,
            @RequestParam(defaultValue = "ROOM_PHOTO") String purpose
    ) {
        service.reorder(roomId, purpose, body.mediaIds());
        return ResponseEntity.noContent().build();
    }


}
