package com.roomledger.app.client;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.roomledger.app.dto.PaymentResponseDTO;
import com.roomledger.app.dto.ReusableCodeResult;
import com.roomledger.app.dto.XenditPaymentRequestDTO;
import com.roomledger.app.exthandler.ClientErrorException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
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
    public ReusableCodeResult createReusableVa(UUID customerId, String displayName, String bankChannelCode) {




        ReusableCodeResult r = createReusableCode(customerId.toString(), Map.of(
                "channel_code", bankChannelCode,                                // e.g. "BNI_VIRTUAL_ACCOUNT"
                "channel_properties", Map.of(
                        "display_name", displayName,
                        "expires_at", "2030-12-31T23:59:59Z"
                )
        ));
        return r;

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

    /** One-off VA (exact amount) for a bill (type=PAY). */
    public PaymentResponseDTO createPayVa(XenditPaymentRequestDTO xenditPaymentRequestDTO) {
        Map<String, Object> body = new LinkedHashMap<>();
        Map<String, Object> props = new LinkedHashMap<>();

        props.put("display_name", xenditPaymentRequestDTO.getChannelProperties().getDisplayName());

        if (xenditPaymentRequestDTO.getChannelProperties().getExpectedAmount() != null) props.put("expected_amount", xenditPaymentRequestDTO.getChannelProperties().getExpectedAmount());

        body.put("reference_id", xenditPaymentRequestDTO.getReferenceId());
        body.put("type", "PAY");
        body.put("country", xenditPaymentRequestDTO.getCountry());
        body.put("currency", xenditPaymentRequestDTO.getCurrency());
        body.put("request_amount", xenditPaymentRequestDTO.getRequestAmount());
        body.put("channel_code", xenditPaymentRequestDTO.getChannelCode()); // e.g. "BNI_VIRTUAL_ACCOUNT"
        body.put("channel_properties", props);

        Map<String, Object> response = post("/v3/payment_requests", body);

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());
            return objectMapper.convertValue(response, PaymentResponseDTO.class);
        } catch (Exception e) {
            // Handle error during deserialization
            throw new RuntimeException("Error parsing payment response: " + e.getMessage(), e);
        }
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
        Map<String, Object> response = post("/v3/payment_requests", body);

        return post("/v3/payment_requests", body);
    }


    public Map getPaymentRequest(String paymentRequestId) {
        return xendit.get()
                .uri("/v3/payment_requests/{id}", paymentRequestId)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw new ClientErrorException(res.getStatusCode().value() +  res.getBody().toString());
                })
                .body(Map.class);
    }

    /**
     * List payment requests for your reference id (e.g., customer id).
     */
    public Map listPaymentRequestsByReference(String referenceId, Integer limit, String afterId) {
        return xendit.get()
                .uri(uri -> {
                    var b = uri.path("/payment_requests").queryParam("reference_id", referenceId);
                    if (limit != null) b.queryParam("limit", limit);
                    if (afterId != null) b.queryParam("after_id", afterId);
                    return b.build();
                })
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw new ClientErrorException( res.getBody().toString());
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
        body.putAll(extra);

        Map resp = xendit.post()
                .uri("/v3/payment_requests")
                .header("Idempotency-Key", idem)
                .body(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw new ClientErrorException( res.getBody().toString());
                })
                .body(Map.class);

        String prId = (String) resp.get("id");
        String channel = (String) resp.getOrDefault("channel_code", extra.get("channel_code"));

        Map<String, Object> va = findAction(resp, "VIRTUAL_ACCOUNT_NUMBER");
        Map<String, Object> qr = findAction(resp, "QR_CODE");

        String codeValue = null;
        String kind = null;

        if (va != null) {
            codeValue = asString(va.getOrDefault("value", va.get("account_number")));
            kind = "VIRTUAL_ACCOUNT";
        } else if (qr != null) {
            codeValue = asString(qr.getOrDefault("qr_string", qr.get("value")));
            kind = "QRIS";
        }

        LocalDateTime expiresAt = toLocalUtc(asString(resp.get("expires_at")));
        if (expiresAt == null) {
            @SuppressWarnings("unchecked")
            Map<String, Object> chProps = (Map<String, Object>) extra.get("channel_properties");
            if (chProps != null) {
                expiresAt = toLocalUtc(asString(chProps.get("expires_at")));
            }
        }

        return new ReusableCodeResult(prId, referenceId, channel, kind, codeValue, expiresAt, null);
    }

    private Map post(String uri, Map<String, Object> body) {
        String idem = UUID.randomUUID().toString();
        return xendit.post()
                .uri(uri)
                .header("Idempotency-Key", idem)
                .body(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw new ClientErrorException(res.getBody().toString());
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
        return Collections.<String, Object>emptyMap();
    }

    private LocalDateTime toLocalUtc(String iso) {
        if (iso == null) return null;
        try {
            return java.time.OffsetDateTime.parse(iso)
                    .toInstant()
                    .atZone(java.time.ZoneOffset.UTC)
                    .toLocalDateTime();
        } catch (Exception e) {
            try { return java.time.LocalDateTime.parse(iso); } catch (Exception ignore) { return null; }
        }
    }


    private String asString(Object o) { return (o == null) ? null : String.valueOf(o); }

    private OffsetDateTime parseIso(String s) {
        try { return (s == null) ? null : OffsetDateTime.parse(s); }
        catch (Exception e) { return null; }
    }


}
