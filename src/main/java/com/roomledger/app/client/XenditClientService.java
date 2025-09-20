package com.roomledger.app.client;
import com.roomledger.app.dto.ReusableCodeResult;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.OffsetDateTime;
import java.util.*;

@Service
public class XenditClientService {

    private final RestClient xendit;

    public XenditClientService(@Qualifier("xenditRestClient") RestClient xendit) {
        this.xendit = xendit;
    }

  /* ============================
     CREATE (v3/payment_requests)
     ============================ */

    /** Create REUSABLE VA per customer (auto-generate number). */
    public ReusableCodeResult createReusableVa(String customerId, String displayName, String bankChannelCode) {
        return createReusableCode(customerId, Map.of(
                "channel_code", bankChannelCode,                                // e.g. "BNI_VIRTUAL_ACCOUNT"
                "channel_properties", Map.of(
                        "display_name", displayName,
                        "expires_at", "2030-12-31T23:59:59Z"
                )
        ));
    }

    /** Create REUSABLE static QR (QRIS). */
    public ReusableCodeResult createReusableQris(String customerId) {
        return createReusableCode(customerId, Map.of(
                "channel_code", "QRIS",
                "channel_properties", Map.of(
                        "expires_at", "2030-12-31T23:59:59Z"
                )
        ));
    }

    /** One-off VA (exact amount) for a bill (type=PAY). Use when you want closed amount. */
    public Map<String, Object> createPayVa(String bookingRef, long amount, String bankChannelCode,
                                           String displayName, Long expectedAmount /* nullable */) {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("display_name", displayName);
        if (expectedAmount != null) props.put("expected_amount", expectedAmount);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("reference_id", bookingRef);
        body.put("type", "PAY");
        body.put("country", "ID");
        body.put("currency", "IDR");
        body.put("request_amount", amount);
        body.put("channel_code", bankChannelCode);                   // e.g. "BNI_VIRTUAL_ACCOUNT"
        body.put("channel_properties", props);

        return post("/v3/payment_requests", body);
    }

    /** One-off QRIS (dynamic, encodes amount). */
    public Map<String, Object> createPayQris(String bookingRef, long amount) {
        Map<String, Object> body = Map.of(
                "reference_id", bookingRef,
                "type", "PAY",
                "country", "ID",
                "currency", "IDR",
                "request_amount", amount,
                "channel_code", "QRIS"
        );
        return post("/v3/payment_requests", body);
    }

    /* ============ GET/LIST ============ */

    public Map<String, Object> getPaymentRequest(String paymentRequestId) {
        return xendit.get()
                .uri("/v3/payment_requests/{id}", paymentRequestId)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw new XenditClientException(res.getStatusCode().value(), res.getBody().toString());
                })
                .body(Map.class);
    }

    /** List payment requests for your reference id (e.g., customer id). */
    public Map<String, Object> listPaymentRequestsByReference(String referenceId, Integer limit, String afterId) {
        return xendit.get()
                .uri(uri -> {
                    var b = uri.path("/payment_requests").queryParam("reference_id", referenceId);
                    if (limit != null) b.queryParam("limit", limit);
                    if (afterId != null) b.queryParam("after_id", afterId);
                    return b.build();
                })
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw new XenditClientException(res.getStatusCode().value(), res.getBody().toString());
                })
                .body(Map.class);
    }

    /* ============ Helpers ============ */

    /** Create REUSABLE_PAYMENT_CODE with arbitrary channel bits. */
    private ReusableCodeResult createReusableCode(String referenceId, Map<String, Object> extra) {
        String idem = UUID.randomUUID().toString();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("reference_id", referenceId);
        body.put("type", "REUSABLE_PAYMENT_CODE");
        body.put("country", "ID");
        body.put("currency", "IDR");
        // merge extras
        body.putAll(extra);

        Map<String, Object> resp = xendit.post()
                .uri("/v3/payment_requests")
                .header("Idempotency-Key", idem)
                .body(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw new XenditClientException(res.getStatusCode().value(), res.getBody().toString());
                })
                .body(Map.class);

        String prId = (String) resp.get("id");
        String channel = (String) resp.getOrDefault("channel_code", extra.get("channel_code"));

        // extract from actions[]
        Map<String, Object> va = findAction(resp, "VIRTUAL_ACCOUNT_NUMBER");
        Map<String, Object> qr = findAction(resp, "QR_CODE");

        String codeValue = null;
        ReusableCodeResult.Kind kind = null;

        if (va != null) {
            codeValue = asString(va.getOrDefault("value", va.get("account_number")));
            kind = ReusableCodeResult.Kind.VIRTUAL_ACCOUNT;
        } else if (qr != null) {
            codeValue = asString(qr.getOrDefault("qr_string", qr.get("value")));
            kind = ReusableCodeResult.Kind.QR;
        }

        OffsetDateTime expiresAt = parseIso((String) resp.get("expires_at"));
        if (expiresAt == null) {
            // sometimes expires lives in channel_properties or not present for certain channels
            Map<String, Object> chProps = (Map<String, Object>) extra.get("channel_properties");
            if (chProps != null) expiresAt = parseIso(asString(chProps.get("expires_at")));
        }

        return new ReusableCodeResult(prId, referenceId, channel, kind, codeValue, expiresAt, resp);
    }

    private Map<String, Object> post(String uri, Map<String, Object> body) {
        String idem = UUID.randomUUID().toString();
        return xendit.post()
                .uri(uri)
                .header("Idempotency-Key", idem)
                .body(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw new XenditClientException(res.getStatusCode().value(), res.getBody().toString());
                })
                .body(Map.class);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> findAction(Map<String, Object> resp, String descriptor) {
        Object actionsObj = resp.get("actions");
        if (!(actionsObj instanceof List<?> list)) return null;
        for (Object o : list) {
            if (o instanceof Map<?, ?> m) {
                Object d = m.get("descriptor");
                if (descriptor.equals(Objects.toString(d, null))) {
                    return (Map<String, Object>) m;
                }
            }
        }
        return null;
    }

    private String asString(Object o) { return (o == null) ? null : String.valueOf(o); }

    private OffsetDateTime parseIso(String s) {
        try { return (s == null) ? null : OffsetDateTime.parse(s); }
        catch (Exception e) { return null; }
    }

    /* ======== DTOs / Exception ======== */



    public static class XenditClientException extends RuntimeException {
        private final int status;
        public XenditClientException(int status, String body) {
            super("Xendit API error " + status + ": " + body);
            this.status = status;
        }
        public int status() { return status; }
    }
}
