package tn.esprit.spring.baladna.transport.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import tn.esprit.spring.baladna.transport.dto.OpenMeteoResponse;
import tn.esprit.spring.baladna.transport.dto.WeatherInfo;
import tn.esprit.spring.baladna.transport.entity.Station;
import tn.esprit.spring.baladna.transport.entity.WeatherCondition;

import java.net.URI;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class WeatherService {

    private static final String OPEN_METEO_URL = "https://api.open-meteo.com/v1/forecast";

    private final RestTemplate restTemplate;
    private final WeatherMapper weatherMapper;

    public WeatherInfo getWeatherForDeparture(Station station, LocalDateTime departureDateTime) {
        if (station == null || station.getLatitude() == null || station.getLongitude() == null || departureDateTime == null) {
            return defaultWeather();
        }

        LocalDate date = departureDateTime.toLocalDate();

        URI uri = UriComponentsBuilder.fromUriString(OPEN_METEO_URL)
                .queryParam("latitude", station.getLatitude())
                .queryParam("longitude", station.getLongitude())
                .queryParam("hourly", "weather_code,temperature_2m,wind_speed_10m,precipitation")
                .queryParam("timezone", "auto")
                .queryParam("start_date", date)
                .queryParam("end_date", date)
                .build()
                .toUri();

        try {
            OpenMeteoResponse response = restTemplate.getForObject(uri, OpenMeteoResponse.class);

            int bestIndex = findClosestHourIndex(response, departureDateTime);
            if (bestIndex < 0 || response == null || response.getHourly() == null) {
                return defaultWeather();
            }

            OpenMeteoResponse.HourlyData hourly = response.getHourly();

            Integer code = safeGet(hourly.getWeatherCode(), bestIndex);
            Double temp = safeGet(hourly.getTemperature2m(), bestIndex);
            Double wind = safeGet(hourly.getWindSpeed10m(), bestIndex);
            Double precip = safeGet(hourly.getPrecipitation(), bestIndex);

            return WeatherInfo.builder()
                    .weatherCode(code)
                    .condition(weatherMapper.mapWeatherCode(code))
                    .temperature(temp)
                    .windSpeed(wind)
                    .precipitation(precip)
                    .build();

        } catch (Exception exception) {
            log.error("Open-Meteo error for station {} at {}", station.getName(), departureDateTime, exception);
            return defaultWeather();
        }
    }

    private int findClosestHourIndex(OpenMeteoResponse response, LocalDateTime departureDateTime) {
        if (response == null || response.getHourly() == null || response.getHourly().getTime() == null || response.getHourly().getTime().isEmpty()) {
            return -1;
        }

        List<String> times = response.getHourly().getTime();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

        int bestIndex = -1;
        long smallestDifference = Long.MAX_VALUE;

        for (int index = 0; index < times.size(); index++) {
            try {
                LocalDateTime candidateTime = LocalDateTime.parse(times.get(index), formatter);
                long difference = Math.abs(Duration.between(candidateTime, departureDateTime).toMinutes());

                if (difference < smallestDifference) {
                    smallestDifference = difference;
                    bestIndex = index;
                }
            } catch (Exception parseException) {
                log.warn("Unable to parse Open-Meteo time value: {}", times.get(index));
            }
        }

        return bestIndex;
    }

    private <T> T safeGet(List<T> list, int index) {
        if (list == null || index < 0 || index >= list.size()) {
            return null;
        }
        return list.get(index);
    }

    private WeatherInfo defaultWeather() {
        return WeatherInfo.builder()
                .weatherCode(null)
                .condition(WeatherCondition.SUNNY)
                .temperature(null)
                .windSpeed(null)
                .precipitation(null)
                .build();
    }
}