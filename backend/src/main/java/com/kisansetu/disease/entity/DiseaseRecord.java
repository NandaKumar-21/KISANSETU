package com.kisansetu.disease.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "disease_records")
@Getter
@Setter
public class DiseaseRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "farmer_id", nullable = false)
    private UUID farmerId;

    @Column(name = "detection_method", nullable = false)
    private String detectionMethod;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(columnDefinition = "text[]")
    private String[] symptoms;

    @Column(name = "disease_name")
    private String diseaseName;

    private String confidence;
    private String severity;
    @Column(length = 4000)
    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "analysis_json", columnDefinition = "jsonb")
    private String analysisJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}