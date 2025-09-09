package com.roomledger.app.dto;

import com.roomledger.app.util.ResponseCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class ResponseDto implements Serializable {
    private String responseCode;
    @Builder.Default
    private String responseDesc = ResponseCode.SUCCESS.getDescription();
    private transient Object data;
}
