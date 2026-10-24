package com.kisansetu.farmer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CalendarEventRequest(
        @NotBlank(message = "Title is required")
        String title,

        String description,

        @NotBlank(message = "Event type is required")
        String eventType,

        @NotNull(message = "Event date is required")
        LocalDate eventDate,

        Boolean reminderEnabled,
        String cropType,
        Boolean weatherDependent,
        Boolean suggestedByAi
) {
}