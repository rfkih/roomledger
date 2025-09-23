package com.roomledger.app.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Setter
@Getter
public class RawResponse {
    @JsonProperty("payment_request_id")
    private String paymentRequestId;

    @JsonProperty("country")
    private String country;

    @JsonProperty("currency")
    private String currency;

    @JsonProperty("business_id")
    private String businessId;

    @JsonProperty("reference_id")
    private String referenceId;

    @JsonProperty("created")
    private LocalDateTime created;

    @JsonProperty("updated")
    private LocalDateTime updated;

    @JsonProperty("status")
    private String status;

    @JsonProperty("capture_method")
    private String captureMethod;

    @JsonProperty("channel_code")
    private String channelCode;

    @JsonProperty("request_amount")
    private long requestAmount;
    private ChannelProperties channelProperties;
    private String type;
    private List<Action> actions;
}
