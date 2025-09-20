package com.roomledger.app.dto;


import jakarta.validation.constraints.NotBlank;

public record CreateReusableQrisRequest(
        @NotBlank String customerId
) {}