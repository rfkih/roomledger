package com.roomledger.app.util;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/** Builder helpers for ResponseService envelopes. */
public final class ResponseUtil {

    private ResponseUtil() {}

    /** Matches: setResponse(HttpStatus.OK.value(), appCode, code, desc, data) */
    public static <T> ResponseEntity<ResponseService> setResponse(
            int httpStatus,
            String applicationCode,
            Object responseCode,
            Object responseDesc,
            T responseData
    ) {
        ResponseService body = new ResponseService(responseCode, responseDesc, responseData);
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(httpStatus);
        if (applicationCode != null && !applicationCode.isBlank()) {
            builder.header("X-Application-Code", applicationCode);
        }
        return builder.body(body);
    }

    /** Same as above but using HttpStatus directly. */
    public static <T> ResponseEntity<ResponseService> setResponse(
            HttpStatus status,
            String applicationCode,
            Object responseCode,
            Object responseDesc,
            T responseData
    ) {
        return setResponse(status.value(), applicationCode, responseCode, responseDesc, responseData);
    }

    /** Simpler variant without applicationCode header. */
    public static <T> ResponseEntity<ResponseService> setResponse(
            int httpStatus,
            Object responseCode,
            Object responseDesc,
            T responseData
    ) {
        return ResponseEntity.status(httpStatus)
                .body(new ResponseService(responseCode, responseDesc, responseData));
    }

    /** Convenience for OK (200). */
    public static <T> ResponseEntity<ResponseService> ok(
            Object responseCode,
            Object responseDesc,
            T responseData
    ) {
        return setResponse(HttpStatus.OK.value(), responseCode, responseDesc, responseData);
    }

}
