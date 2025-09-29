package com.roomledger.app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentStartItem {
    private UUID paymentId;
    private String type;           // RENT / DEPOSIT
    private long amount;
    private String channelCode;    // e.g., BNI, BCA, QRIS
    private String vaNumber;       // null for QRIS
    private String qrisQrString;   // null for VA
    private LocalDateTime expiresAt;     // adapt type to your field type


}
