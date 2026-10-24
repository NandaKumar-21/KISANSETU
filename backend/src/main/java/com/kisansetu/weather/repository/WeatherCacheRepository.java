package com.kisansetu.weather.repository;

import com.kisansetu.weather.entity.WeatherCache;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface WeatherCacheRepository extends JpaRepository<WeatherCache, UUID> {

    Optional<WeatherCache> findByLatitudeAndLongitude(BigDecimal latitude, BigDecimal longitude);
}