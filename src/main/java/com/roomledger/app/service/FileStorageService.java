package com.roomledger.app.service;

import com.roomledger.app.config.MinioProperties;
import com.roomledger.app.dto.UploadResult;
import io.minio.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.*;

@Service
@RequiredArgsConstructor
public class FileStorageService {

    private static final Set<String> ALLOWED_MIME = Set.of(
            MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_PNG_VALUE, "image/webp"
    );
    private static final long MAX_SIZE_BYTES = 15 * 1024 * 1024;

    private final MinioClient minio;
    private final MinioProperties props;
    private final JdbcTemplate jdbc;


    @Transactional
    public List<UploadResult> upload(List<MultipartFile> files,
                                     String visibility,
                                     String purpose,
                                     String pathPrefix) throws Exception {
        // ----- prep & validation -----
        final String bucket = "SENSITIVE".equalsIgnoreCase(visibility)
                ? props.bucketPrivate()
                : props.bucketPublic();
        ensureBucket(bucket);

        final String sanitizedPrefix = stripTrailingSlash(pathPrefix);
        final UUID ownerUuid = extractUuidFromPath(sanitizedPrefix); // throws if invalid

        final String linkPurpose = (purpose == null || purpose.isBlank()) ? "ROOM_PHOTO" : purpose;

        // compute starting sort_order once per (owner, purpose)
        Integer startOrder = jdbc.queryForObject("""
        SELECT COALESCE(MAX(sort_order), 0) + 1
        FROM media_link
        WHERE owner_type = ? AND owner_id = ? AND purpose = ?
        """, Integer.class, "ROOM", ownerUuid, linkPurpose);
        int sortOrder = (startOrder == null ? 1 : startOrder);

        // ----- loop files -----
        List<UploadResult> results = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) continue;

            if (file.getSize() > MAX_SIZE_BYTES) {
                throw new IllegalArgumentException("File too large (max 15MB)");
            }
            String contentType = Optional.ofNullable(file.getContentType()).orElse("");
            if (!ALLOWED_MIME.contains(contentType)) {
                throw new IllegalArgumentException("Unsupported type: " + contentType);
            }

            // probe & hash
            byte[] data = file.getBytes();
            String sha256 = sha256Hex(data);

            Integer width = null, height = null;
            try (InputStream is = file.getInputStream()) {
                BufferedImage img = ImageIO.read(is); // for WEBP add TwelveMonkeys
                if (img != null) { width = img.getWidth(); height = img.getHeight(); }
            } catch (Exception ignore) {}

            // storage key
            String ext = guessExt(contentType, file.getOriginalFilename());
            String storageKey = sanitizedPrefix + "/" + UUID.randomUUID() + ext;

            // upload to MinIO
            try (InputStream in = file.getInputStream()) {
                minio.putObject(
                        PutObjectArgs.builder()
                                .bucket(bucket)
                                .object(storageKey)
                                .stream(in, data.length, -1)
                                .contentType(contentType)
                                .build()
                );
            }

            // insert media (let DB time be source of truth)
            UUID mediaId = UUID.randomUUID();
            jdbc.update("""
            INSERT INTO media (id, bucket, storage_key, content_type, size, width, height, sha256, visibility, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, now())
            """,
                    mediaId, bucket, storageKey, contentType, file.getSize(), width, height, sha256,
                    "SENSITIVE".equalsIgnoreCase(visibility) ? "SENSITIVE" : "PUBLIC"
            );

            // link to owner with a non-null sort_order
            jdbc.update("""
            INSERT INTO media_link (id, owner_type, owner_id, media_id, purpose, sort_order)
            VALUES (?, ?, ?, ?, ?, ?)
            ON CONFLICT (owner_type, owner_id, media_id, purpose) DO NOTHING
            """,
                    UUID.randomUUID(), "ROOM", ownerUuid, mediaId, linkPurpose, sortOrder++
            );

            results.add(new UploadResult(mediaId, publicUrl(bucket, storageKey), storageKey, linkPurpose));
        }

        return results;
    }

    /* ---------- helpers ---------- */

    private static String stripTrailingSlash(String s) {
        if (s == null) return "";
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }

    private static UUID extractUuidFromPath(String path) {
        int i = path.lastIndexOf('/');
        String last = (i >= 0) ? path.substring(i + 1) : path;
        return UUID.fromString(last); // throws IllegalArgumentException if not a UUID
    }

    private void ensureBucket(String bucket) throws Exception {
        boolean exists = minio.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
        if (!exists) minio.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
    }

    private static String sha256Hex(byte[] data) throws Exception {
        var md = MessageDigest.getInstance("SHA-256");
        return String.format("%064x", new BigInteger(1, md.digest(data)));
    }

    private String publicUrl(String bucket, String key) {
        String base = props.publicBaseUrl().replaceAll("/+$", "");
        return base + "/" + bucket + "/" + key;
    }

    private static String guessExt(String contentType, String original) {
        return switch (contentType) {
            case MediaType.IMAGE_JPEG_VALUE -> ".jpg";
            case MediaType.IMAGE_PNG_VALUE  -> ".png";
            case "image/webp"               -> ".webp";
            default -> {
                if (original != null && original.contains(".")) yield original.substring(original.lastIndexOf('.'));
                yield "";
            }
        };
    }
}