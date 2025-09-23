package com.roomledger.app.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;


@Setter
@Getter
public class ChannelProperties {

    @JsonProperty("display_name")
    private String displayName;

    @JsonProperty("expires_at")
    private LocalDateTime expiresAt;

    private String expectedAmount;

}
