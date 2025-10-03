package com.roomledger.app.controller;

import com.roomledger.app.dto.UploadResult;
import com.roomledger.app.service.FileStorageService;
import com.roomledger.app.util.ResponseCode;
import com.roomledger.app.util.ResponseService;
import com.roomledger.app.util.ResponseUtil;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    @Value("${application.code}")
    private String applicationCode;
    private final FileStorageService storage;

    // Generic upload (multipart)
    @PostMapping
    public ResponseService upload(
            @RequestParam("files") @NotEmpty List<MultipartFile> files,
            @RequestParam(value = "visibility", defaultValue = "PUBLIC") String visibility,
            @RequestParam(value = "purpose", defaultValue = "GENERAL_FILE") String purpose,
            @RequestParam(value = "pathPrefix", required = false, defaultValue = "uploads") String pathPrefix
    ) throws Exception {
        List<UploadResult>  uploaded = storage.upload(files, visibility, purpose, pathPrefix);
        return ResponseUtil.setResponse(
                HttpStatus.OK.value(),
                applicationCode,
                ResponseCode.SUCCESS.getCode(),
                ResponseCode.SUCCESS.getDescription(),
                uploaded
        ).getBody();
    }
}