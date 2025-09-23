package com.roomledger.app.dto;


import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class XenditPaymentRequestDTO {
    private String referenceId;
    private String type;
    private String country;
    private String currency;
    private long requestAmount;
    private String channelCode;
    private ChannelProperties channelProperties;
}
