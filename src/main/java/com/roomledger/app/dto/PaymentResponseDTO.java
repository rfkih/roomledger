package com.roomledger.app.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.JoinColumn;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Setter
@Getter
public class PaymentResponseDTO {

    // Fields from the JSON response
    @JsonProperty("payment_id")
    private String paymentId;

    @JsonProperty("pr_id")
    private String prId;

    @JsonProperty("va_number")
    private String vaNumber;

    @JsonProperty("qris_qr_string")
    private String qrisQrString;

    @JsonProperty("country")
    private String country;

    @JsonProperty("request_amount")
    private long requestAmount;

    @JsonProperty("currency")
    private String currency;

    @JsonProperty("status")
    private String status;

    @JsonProperty("capture_method")
    private String captureMethod;

    @JsonProperty("channel_code")
    private String channelCode;

    @JsonProperty("expires_at")
    private LocalDateTime expiresAt;

    @JsonProperty("payment_request_id")
    private String paymentRequestId;

    @JsonProperty("business_id")
    private String businessId;

    @JsonProperty("channel_properties")
    private ChannelProperties channelProperties;

    @JsonProperty("reference_id")
    private String referenceId;

    @JsonProperty("type")
    private String type;

    // Add LocalDateTime fields with @JsonProperty
    @JsonProperty("created")
    private LocalDateTime created;

    @JsonProperty("updated")
    private LocalDateTime updated;

    @JsonProperty("actions")
    private List<Action> actions;

    private RawResponse raw;

//
//    private String paymentId;
//    @JsonProperty("payment_request_id")
//    private String paymentRequestId;
//    @JsonProperty("country")
//    private String country;
//    @JsonProperty("business_id")
//    private String businessId;
//    @JsonProperty("reference_id")
//    private String referenceId;
//    @JsonProperty("created")
//    private String created;
//    @JsonProperty("updated")
//    private String updated;
//    @JsonProperty("capture_method")
//    private String captureMethod;
//    private String prId;
//    @JsonProperty("channel_code")
//    private String channelCode;
//    private String currency;
//    @JsonProperty("request_amount")
//    private long requestAmount;
//    private String vaNumber;
//    private String qrisQrString;
//    @JsonProperty("channel_properties")
//    private ChannelProperties channelProperties;
//    private LocalDateTime expiresAt;
//    private RawResponse raw;
}
