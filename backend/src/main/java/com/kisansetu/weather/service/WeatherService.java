package com.kisansetu.weather.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kisansetu.config.KisanSetuProperties;
import com.kisansetu.weather.entity.WeatherCache;
import com.kisansetu.weather.repository.WeatherCacheRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * Weather service. Uses Open-Meteo (free, no key) by default; the base URL
 * and optional API key are configurable so another provider can be plugged in.
 */
@Slf4j
@Service
public class WeatherService {

    private static final long CACHE_TTL_SECONDS = 30 * 60;
    private static final Map<Integer, String> WMO_CONDITIONS = Map.ofEntries(
            Map.entry(0, "Clear sky"), Map.entry(1, "Mainly clear"), Map.entry(2, "Partly cloudy"),
            Map.entry(3, "Overcast"), Map.entry(45, "Fog"), Map.entry(48, "Depositing rime fog"),
            Map.entry(51, "Light drizzle"), Map.entry(53, "Moderate drizzle"), Map.entry(55, "Dense drizzle"),
            Map.entry(56, "Light freezing drizzle"), Map.entry(57, "Dense freezing drizzle"),
            Map.entry(61, "Slight rain"), Map.entry(63, "Moderate rain"), Map.entry(65, "Heavy rain"),
            Map.entry(66, "Light freezing rain"), Map.entry(67, "Heavy freezing rain"),
            Map.entry(71, "Slight snowfall"), Map.entry(73, "Moderate snowfall"), Map.entry(75, "Heavy snowfall"),
            Map.entry(77, "Snow grains"), Map.entry(80, "Slight rain showers"), Map.entry(81, "Moderate rain showers"),
            Map.entry(82, "Violent rain showers"), Map.entry(85, "Slight snow showers"),
            Map.entry(86, "Heavy snow showers"), Map.entry(95, "Thunderstorm"),
            Map.entry(96, "Thunderstorm with slight hail"), Map.entry(99, "Thunderstorm with heavy hail"));

    private final KisanSetuProperties props;
    private final WeatherCacheRepository cacheRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient;

    @Autowired
    public WeatherService(KisanSetuProperties props, WeatherCacheRepository cacheRepository) {
        this(props, cacheRepository, HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build());
    }

    WeatherService(KisanSetuProperties props, WeatherCacheRepository cacheRepository, HttpClient httpClient) {
        this.props = props;
        this.cacheRepository = cacheRepository;
        this.httpClient = httpClient;
    }

    public record Weather(double temperature, String condition, double humidity, double windSpeed,
                          double precipitation, double rainChance, double tempMax, double tempMin) {
    }

    /**
     * Fetch weather for coordinates, honoring the in-database short-TTL cache.
     */
    public Weather getWeather(double latitude, double longitude) {
        BigDecimal lat = round(latitude);
        BigDecimal lng = round(longitude);

        WeatherCache cached = cacheRepository.findByLatitudeAndLongitude(lat, lng).orElse(null);
        if (cached != null && Instant.now().getEpochSecond() - cached.getCachedAt().getEpochSecond() < CACHE_TTL_SECONDS) {
            return deserialize(cached.getPayload());
        }

        Weather weather = fetchFromProvider(latitude, longitude);
        if (weather != null) {
            saveCache(lat, lng, weather);
            return weather;
        }
        if (cached != null) {
            log.warn("Weather provider unavailable; serving stale cache for ({},{})", lat, lng);
            return deserialize(cached.getPayload());
        }
        throw new com.kisansetu.common.exception.ApiException(502, "Weather service temporarily unavailable");
    }

    private Weather fetchFromProvider(double latitude, double longitude) {
        try {
            String url = props.weather().baseUrl()
                    + "/forecast?latitude=" + latitude
                    + "&longitude=" + longitude
                    + "&current=temperature_2m,relative_humidity_2m,precipitation,weather_code,wind_speed_10m"
                    + "&daily=temperature_2m_max,temperature_2m_min,precipitation_probability_max"
                    + "&timezone=auto";
            if (props.weather().apiKey() != null && !props.weather().apiKey().isBlank()) {
                url += "&apikey=" + props.weather().apiKey();
            }
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.warn("Weather provider returned {}", response.statusCode());
                return null;
            }
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode current = root.path("current");
            JsonNode daily = root.path("daily");
            int code = current.path("weather_code").asInt(0);
            return new Weather(
                    current.path("temperature_2m").asDouble(0),
                    WMO_CONDITIONS.getOrDefault(code, "Unknown"),
                    current.path("relative_humidity_2m").asDouble(0),
                    current.path("wind_speed_10m").asDouble(0),
                    current.path("precipitation").asDouble(0),
                    daily.path("precipitation_probability_max").path(0).asDouble(0),
                    daily.path("temperature_2m_max").path(0).asDouble(0),
                    daily.path("temperature_2m_min").path(0).asDouble(0));
        } catch (Exception e) {
            log.warn("Weather fetch failed: {}", e.getMessage());
            return null;
        }
    }

    private void saveCache(BigDecimal lat, BigDecimal lng, Weather weather) {
        try {
            String json = objectMapper.writeValueAsString(weather);
            WeatherCache cache = cacheRepository.findByLatitudeAndLongitude(lat, lng).orElse(new WeatherCache());
            cache.setLatitude(lat);
            cache.setLongitude(lng);
            cache.setPayload(json);
            cache.setCachedAt(Instant.now());
            cacheRepository.save(cache);
        } catch (Exception e) {
            log.warn("Could not persist weather cache: {}", e.getMessage());
        }
    }

    private Weather deserialize(String json) {
        try {
            return objectMapper.readValue(json, Weather.class);
        } catch (Exception e) {
            throw new com.kisansetu.common.exception.ApiException(502, "Weather cache corrupted");
        }
    }

    private BigDecimal round(double value) {
        return BigDecimal.valueOf(value).setScale(4, RoundingMode.HALF_UP);
    }
}