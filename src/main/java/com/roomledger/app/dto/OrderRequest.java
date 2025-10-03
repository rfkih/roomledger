package com.roomledger.app.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public record OrderRequest(@NotEmpty List<UUID> mediaIds) {}
