package com.roomledger.app.util;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResponseService {
    Object responseCode;
    Object responseDesc;
    Object responseData;

    public ResponseService() {
    }

    public ResponseService(Object responseCode, Object responseDesc, Object responseData) {
        this.responseCode = responseCode;
        this.responseDesc = responseDesc;
        this.responseData = responseData;
    }

}