package com.kisansetu.farmer.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "farmer_calendar")
@Getter
@Setter
public class FarmerCalendarEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "farmer_id", nullable = false)
    private UUID farmerId;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String description;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;

    @Column(name = "reminder_enabled")
    private boolean reminderEnabled;

    private boolean completed;

    @Column(name = "crop_type")
    private String cropType;

    @Column(name = "weather_dependent")
    private boolean weatherDependent;

    @Column(name = "suggested_by_ai")
    private boolean suggestedByAi;

    @Column(name = "notification_sent")
    private boolean notificationSent;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        reminderEnabled = true;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}