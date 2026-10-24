package com.kisansetu.farmer.dto;

import com.kisansetu.farmer.entity.FarmerCalendarEvent;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record CalendarEventResponse(
        UUID id,
        String title,
        String description,
        String eventType,
        LocalDate eventDate,
        boolean reminderEnabled,
        boolean completed,
        String cropType,
        boolean weatherDependent,
        boolean suggestedByAi,
        Instant createdAt
) {

    public static CalendarEventResponse from(FarmerCalendarEvent e) {
        return new CalendarEventResponse(
                e.getId(), e.getTitle(), e.getDescription(), e.getEventType(), e.getEventDate(),
                e.isReminderEnabled(), e.isCompleted(), e.getCropType(), e.isWeatherDependent(),
                e.isSuggestedByAi(), e.getCreatedAt());
    }
}