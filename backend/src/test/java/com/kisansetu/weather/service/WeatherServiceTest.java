package com.kisansetu.weather.service;

import com.kisansetu.common.exception.ApiException;
import com.kisansetu.config.KisanSetuProperties;
import com.kisansetu.weather.entity.WeatherCache;
import com.kisansetu.weather.repository.WeatherCacheRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WeatherServiceTest {

    private static final BigDecimal LAT = new BigDecimal("16.6917");
    private static final BigDecimal LNG = new BigDecimal("74.2445");

    @Mock
    private WeatherCacheRepository cacheRepository;
    @Mock
    private HttpClient httpClient;
    @Mock
    private HttpResponse<String> httpResponse;

    private KisanSetuProperties props;

    @BeforeEach
    void setUp() {
        props = new KisanSetuProperties(null, null, null,
                new KisanSetuProperties.Weather("open-meteo", "https://api.open-meteo.com/v1", null),
                null, null, null);
    }

    private WeatherService service() {
        return new WeatherService(props, cacheRepository, httpClient);
    }

    private WeatherCache freshCache() {
        WeatherCache cache = new WeatherCache();
        cache.setLatitude(LAT);
        cache.setLongitude(LNG);
        cache.setPayload("{\"temperature\":28.5,\"condition\":\"Clear sky\",\"humidity\":62.0,"
                + "\"windSpeed\":12.3,\"precipitation\":0.0,\"rainChance\":5.0,"
                + "\"tempMax\":32.0,\"tempMin\":22.0}");
        cache.setCachedAt(Instant.now());
        return cache;
    }

    private void stubProvider(String body, int statusCode) throws Exception {
        when(httpResponse.statusCode()).thenReturn(statusCode);
        lenient().when(httpResponse.body()).thenReturn(body);
        when(httpClient.send(any(), any())).thenAnswer(inv -> (HttpResponse<String>) (HttpResponse<?>) httpResponse);
    }

    @Test
    void getWeather_servesFreshCacheWithoutProviderCall() throws Exception {
        when(cacheRepository.findByLatitudeAndLongitude(LAT, LNG)).thenReturn(Optional.of(freshCache()));

        WeatherService.Weather weather = service().getWeather(16.6917, 74.2445);

        assertEquals(28.5, weather.temperature());
        assertEquals("Clear sky", weather.condition());
        verify(httpClient, never()).send(any(), any());
    }

    @Test
    void getWeather_parsesProviderResponseAndCaches() throws Exception {
        String body = "{\"current\":{\"temperature_2m\":25.1,\"relative_humidity_2m\":70,"
                + "\"precipitation\":0.2,\"weather_code\":2,\"wind_speed_10m\":8.4},"
                + "\"daily\":{\"precipitation_probability_max\":[40],"
                + "\"temperature_2m_max\":[30.1],\"temperature_2m_min\":[21.0]}}";
        when(cacheRepository.findByLatitudeAndLongitude(any(), any())).thenReturn(Optional.empty());
        stubProvider(body, 200);

        WeatherService.Weather weather = service().getWeather(16.6917, 74.2445);

        assertEquals(25.1, weather.temperature());
        assertEquals("Partly cloudy", weather.condition());
        assertEquals(40.0, weather.rainChance());
        verify(cacheRepository).save(any(WeatherCache.class));
    }

    @Test
    void getWeather_fallsBackToStaleCacheWhenProviderFails() throws Exception {
        WeatherCache stale = freshCache();
        stale.setCachedAt(Instant.now().minusSeconds(3600));
        when(cacheRepository.findByLatitudeAndLongitude(LAT, LNG)).thenReturn(Optional.of(stale));
        when(httpClient.send(any(), any())).thenThrow(new java.io.IOException("down"));

        WeatherService.Weather weather = service().getWeather(16.6917, 74.2445);

        assertEquals(28.5, weather.temperature());
    }

    @Test
    void getWeather_non200ResponseTreatsProviderAsUnavailable() throws Exception {
        when(cacheRepository.findByLatitudeAndLongitude(any(), any())).thenReturn(Optional.empty());
        stubProvider("{}", 503);

        ApiException ex = assertThrows(ApiException.class, () -> service().getWeather(16.6917, 74.2445));
        assertEquals(502, ex.getStatus());
    }
}