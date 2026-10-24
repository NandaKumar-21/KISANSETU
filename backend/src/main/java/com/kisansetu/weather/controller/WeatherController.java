package com.kisansetu.weather.controller;

import com.kisansetu.common.ApiResponse;
import com.kisansetu.weather.service.WeatherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/weather")
@RequiredArgsConstructor
@Tag(name = "Weather", description = "Weather information for farmer locations")
public class WeatherController {

    private final WeatherService weatherService;

    @GetMapping
    @Operation(summary = "Current weather for coordinates")
    public ApiResponse<Map<String, Object>> weather(
            @RequestParam @DecimalMin("-90") @DecimalMax("90") double latitude,
            @RequestParam @DecimalMin("-180") @DecimalMax("180") double longitude) {
        WeatherService.Weather w = weatherService.getWeather(latitude, longitude);
        return ApiResponse.ok(Map.of(
                "temperature", w.temperature(),
                "condition", w.condition(),
                "humidity", w.humidity(),
                "windSpeed", w.windSpeed(),
                "precipitation", w.precipitation(),
                "rainChance", w.rainChance(),
                "tempMax", w.tempMax(),
                "tempMin", w.tempMin()
        ));
    }
}