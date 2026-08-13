package com.hackathon.gateway.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request body for creating or updating a patient's field-level consent.
 */
public record ConsentRequest(
        @NotNull(message = "patientId is required")
        @Min(value = 1, message = "patientId must be a positive integer")
        Integer patientId,

        @NotBlank(message = "field name is required")
        String field,

        @NotNull(message = "consentGiven flag is required")
        Boolean consentGiven
) {}
