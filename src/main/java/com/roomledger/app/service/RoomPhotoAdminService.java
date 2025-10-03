package com.roomledger.app.service;

import com.roomledger.app.config.MinioProperties;
import com.roomledger.app.dto.RoomPhotoDto;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RoomPhotoAdminService {

    private final JdbcTemplate jdbc;
    private final MinioClient minio;
    private final MinioProperties props;

    private static final String OWNER_TYPE = "ROOM";

    /** Unlink photo; optionally delete object if media is orphaned */
    @Transactional
    public void deletePhoto(UUID roomId, UUID mediaId, String purpose, boolean deleteObject) throws Exception {
        // Fetch media + link info (and keep the link row locked so sort fix is consistent)
        var row = jdbc.queryForMap("""
      SELECT ml.sort_order, m.bucket, m.storage_key
      FROM media_link ml
      JOIN media m ON m.id = ml.media_id
      WHERE ml.owner_type = ? AND ml.owner_id = ? AND ml.media_id = ? AND ml.purpose = ?
      FOR UPDATE
      """, OWNER_TYPE, roomId, mediaId, purpose);

        Integer removedOrder = (Integer) row.get("sort_order");
        String bucket = (String) row.get("bucket");
        String storageKey = (String) row.get("storage_key");

        // Unlink
        jdbc.update("""
      DELETE FROM media_link
      WHERE owner_type = ? AND owner_id = ? AND media_id = ? AND purpose = ?
      """, OWNER_TYPE, roomId, mediaId, purpose);

        // Normalize sort_order: shift down anything after the removed one
        jdbc.update("""
      UPDATE media_link
      SET sort_order = sort_order - 1
      WHERE owner_type = ? AND owner_id = ? AND purpose = ? AND sort_order > ?
      """, OWNER_TYPE, roomId, purpose, removedOrder);

        // If no more links reference this media, optionally delete object + media row
        Boolean stillLinked = jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM media_link WHERE media_id = ?)", Boolean.class, mediaId);

        if (Boolean.FALSE.equals(stillLinked) && deleteObject) {
            // Delete object from MinIO (ignore if in private/public; bucket comes from media row)
            minio.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket).object(storageKey).build());

            // Delete media row
            jdbc.update("DELETE FROM media WHERE id = ?", mediaId);
        }
    }

    /** Ensure exactly one primary for (roomId, purpose) */
    @Transactional
    public void setMain(UUID roomId, UUID mediaId, String purpose) {
        // Clear current primary
        jdbc.update("""
      UPDATE media_link
      SET is_main = FALSE
      WHERE owner_type = ? AND owner_id = ? AND purpose = ? AND is_main = TRUE
      """, OWNER_TYPE, roomId, purpose);

        // Set new primary; if the link doesn't exist, this won't create it
        int updated = jdbc.update("""
      UPDATE media_link
      SET is_main = TRUE
      WHERE owner_type = ? AND owner_id = ? AND purpose = ? AND media_id = ?
      """, OWNER_TYPE, roomId, purpose, mediaId);

        if (updated == 0) {
            throw new IllegalArgumentException("Photo not linked to this room/purpose");
        }
    }

    /** Apply ordering using a single UPDATE ... FROM (VALUES ...) */
    @Transactional
    public void reorder(UUID roomId, String purpose, List<UUID> mediaIdsInOrder) {
        if (mediaIdsInOrder == null || mediaIdsInOrder.isEmpty()) return;

        // Build VALUES list: (media_id, rank)
        StringBuilder values = new StringBuilder();
        for (int i = 0; i < mediaIdsInOrder.size(); i++) {
            if (i > 0) values.append(",");
            values.append("(?::uuid, ").append(i + 1).append(")");
        }

        String sql = """
      UPDATE media_link ml
      SET sort_order = v.rank
      FROM (VALUES %s) AS v(media_id, rank)
      WHERE ml.owner_type = ? AND ml.owner_id = ? AND ml.purpose = ? AND ml.media_id = v.media_id
      """.formatted(values);

        // Bind params: first all mediaIds, then ownerType, ownerId, purpose
        Object[] params = new Object[mediaIdsInOrder.size() + 3];
        int idx = 0;
        for (UUID id : mediaIdsInOrder) params[idx++] = id.toString();
        params[idx++] = OWNER_TYPE;
        params[idx++] = roomId;
        params[idx]   = purpose;

        jdbc.update(sql, params);
    }


    // =======================
    // INQUIRY / LIST PHOTOS
    // =======================
    @Transactional(readOnly = true)
    public List<RoomPhotoDto> listPhotos(UUID roomId,
                                         String purpose,
                                         boolean signed,
                                         Integer limit,
                                         Integer offset) {
        final String OWNER_TYPE = "ROOM";
        final String linkPurpose = (purpose == null || purpose.isBlank()) ? "ROOM_PHOTO" : purpose;
        final int lim = (limit == null || limit <= 0) ? 100 : limit;
        final int off = (offset == null || offset < 0) ? 0 : offset;

        final String sql = """
    SELECT
      ml.media_id,
      ml.is_main,
      ml.sort_order,
      m.bucket,
      m.storage_key,
      m.visibility,
      m.content_type,
      m.size,
      m.width,
      m.height,
      m.created_at
    FROM media_link ml
    JOIN media m ON m.id = ml.media_id
    WHERE ml.owner_type = ? AND ml.owner_id = ?::uuid AND ml.purpose = ?
    ORDER BY ml.is_main DESC, ml.sort_order ASC, m.created_at ASC
    LIMIT ?::int OFFSET ?::int
    """;

        return jdbc.query(sql, (rs, rowNum) -> {
            var mediaId    = rs.getObject("media_id", java.util.UUID.class);
            var isMain     = rs.getBoolean("is_main");
            var sortOrder  = rs.getInt("sort_order");
            var bucket     = rs.getString("bucket");
            var storageKey = rs.getString("storage_key");
            var visibility = rs.getString("visibility");
            var contentType= rs.getString("content_type");
            var size       = (Long) rs.getObject("size");
            var width      = (Integer) rs.getObject("width");
            var height     = (Integer) rs.getObject("height");

            // safer for both timestamptz and timestamp
            LocalDateTime createdAt;
            var ts = rs.getTimestamp("created_at");
            createdAt = (ts == null)
                    ? null
                    : ts.toInstant().atOffset(ZoneOffset.systemDefault().getRules().getOffset(ts.toInstant())).toLocalDateTime();

            String url = buildUrl(bucket, storageKey, visibility, signed);

            return new RoomPhotoDto(
                    mediaId, isMain, sortOrder, url,
                    storageKey, bucket, visibility,
                    contentType, size, width, height, createdAt
            );
        }, OWNER_TYPE, roomId, linkPurpose, lim, off);
    }

    // ---------- Helpers ----------
    private String buildUrl(String bucket, String storageKey, String visibility, boolean signed) {
        if ("SENSITIVE".equalsIgnoreCase(visibility) && signed) {
            try {
                return minio.getPresignedObjectUrl(
                        GetPresignedObjectUrlArgs.builder()
                                .method(Method.GET)
                                .bucket(bucket)
                                .object(storageKey)
                                .expiry(60 * 60) // 1 hour
                                .build()
                );
            } catch (Exception ignore) {
                // fall through to public-style URL if presign fails
            }
        }
        return publicUrl(bucket, storageKey);
    }

    private String publicUrl(String bucket, String storageKey) {
        String base = props.publicBaseUrl().replaceAll("/+$", "");
        return base + "/" + bucket + "/" + storageKey;
    }


}
