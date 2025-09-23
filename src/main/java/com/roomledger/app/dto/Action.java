package com.roomledger.app.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Action {
    @JsonProperty("type")
    private String type;

    @JsonProperty("descriptor")
    private String descriptor;

    @JsonProperty("value")
    private String value;
}
