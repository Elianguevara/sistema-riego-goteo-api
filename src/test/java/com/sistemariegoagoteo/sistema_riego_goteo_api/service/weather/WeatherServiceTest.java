package com.sistemariegoagoteo.sistema_riego_goteo_api.service.weather;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.weather.WeatherResponse;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Farm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WeatherServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private WeatherService weatherService;

    private Farm farm;

    @BeforeEach
    void setUp() {
        // Inyectar properties simulando @Value
        ReflectionTestUtils.setField(weatherService, "apiKey", "test-api-key");
        ReflectionTestUtils.setField(weatherService, "apiUrl", "http://api.openweathermap.org/data/2.5/weather");

        farm = new Farm();
        farm.setId(1);
        farm.setLatitude(new java.math.BigDecimal("-32.8895"));
        farm.setLongitude(new java.math.BigDecimal("-68.8458"));
    }

    @Test
    void getCurrentWeather_Success() {
        WeatherResponse mockResponse = new WeatherResponse();
        // Set properties if needed for assertions

        when(restTemplate.getForObject(anyString(), eq(WeatherResponse.class))).thenReturn(mockResponse);

        WeatherResponse result = weatherService.getCurrentWeather(farm);

        // assertNotNull(result);
        verify(restTemplate).getForObject(
                contains("lat=-32.8895&lon=-68.8458&appid=test-api-key&units=metric&lang=es"),
                eq(WeatherResponse.class));
    }

    @Test
    void getCurrentWeather_NoCoordinates_ThrowsException() {
        Farm invalidFarm = new Farm();
        invalidFarm.setId(2);
        // Sin lat/lon

        assertThrows(IllegalStateException.class, () -> weatherService.getCurrentWeather(invalidFarm));
        verify(restTemplate, never()).getForObject(anyString(), any());
    }
}
