package tn.esprit.spring.baladna.transport.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import tn.esprit.spring.baladna.transport.dto.OpenMeteoResponse;
import tn.esprit.spring.baladna.transport.dto.WeatherInfo;
import tn.esprit.spring.baladna.transport.entity.Station;
import tn.esprit.spring.baladna.transport.entity.WeatherCondition;

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
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
                .queryParam("hourly", "weather_code,temperature_2m,windspeed_10m,precipitation")
                .queryParam("timezone", "auto")
                .queryParam("start_date", date)
                .queryParam("end_date", date)
                .build()
                .toUri();

        try {
            OpenMeteoResponse response = restTemplate.getForObject(uri, OpenMeteoResponse.class);
            int bestIndex = findClosestHourIndex(response, departureDateTime);

            if (bestIndex < 0) {
                return defaultWeather();
            }

            OpenMeteoResponse.HourlyData hourly = response.getHourly();

            Integer code = safeGet(hourly.getWeather_code(), bestIndex);
            Double temp = safeGet(hourly.getTemperature_2m(), bestIndex);
            Double wind = safeGet(hourly.getWindspeed_10m(), bestIndex);
            Double precip = safeGet(hourly.getPrecipitation(), bestIndex);

            return WeatherInfo.builder()
                    .weatherCode(code)
                    .condition(weatherMapper.mapWeatherCode(code))
                    .temperature(temp)
                    .windSpeed(wind)
                    .precipitation(precip)
                    .build();
        } catch (Exception exception) {
            return defaultWeather();
        }
    }

    private int findClosestHourIndex(OpenMeteoResponse response, LocalDateTime departureDateTime) {
        if (response == null || response.getHourly() == null) {
            return -1;
        }

        List<String> times = response.getHourly().getTime();
        if (times == null || times.isEmpty()) {
            return -1;
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
        int bestIndex = 0;
        long smallestDifference = Long.MAX_VALUE;

        for (int index = 0; index < times.size(); index++) {
            LocalDateTime candidateTime = LocalDateTime.parse(times.get(index), formatter);
            long difference = Math.abs(java.time.Duration.between(candidateTime, departureDateTime).toMinutes());

            if (difference < smallestDifference) {
                smallestDifference = difference;
                bestIndex = index;
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