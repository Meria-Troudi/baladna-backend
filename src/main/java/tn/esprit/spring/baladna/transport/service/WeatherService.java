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
            return WeatherInfo.builder()
                    .weatherCode(null)
                    .condition(WeatherCondition.SUNNY)
                    .build();
        }

        LocalDate date = departureDateTime.toLocalDate();
        URI uri = UriComponentsBuilder.fromUriString(OPEN_METEO_URL)
                .queryParam("latitude", station.getLatitude())
                .queryParam("longitude", station.getLongitude())
                .queryParam("hourly", "weather_code")
                .queryParam("timezone", "auto")
                .queryParam("start_date", date)
                .queryParam("end_date", date)
                .build()
                .toUri();

        try {
            OpenMeteoResponse response = restTemplate.getForObject(uri, OpenMeteoResponse.class);
            Integer code = extractClosestWeatherCode(response, departureDateTime);

            return WeatherInfo.builder()
                    .weatherCode(code)
                    .condition(weatherMapper.mapWeatherCode(code))
                    .build();
        } catch (Exception exception) {
            return WeatherInfo.builder()
                    .weatherCode(null)
                    .condition(WeatherCondition.SUNNY)
                    .build();
        }
    }

    private Integer extractClosestWeatherCode(OpenMeteoResponse response, LocalDateTime departureDateTime) {
        if (response == null || response.getHourly() == null) {
            return null;
        }

        List<String> times = response.getHourly().getTime();
        List<Integer> weatherCodes = response.getHourly().getWeather_code();

        if (times == null || weatherCodes == null || times.isEmpty() || weatherCodes.isEmpty()) {
            return null;
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
        int bestIndex = 0;
        long smallestDifference = Long.MAX_VALUE;

        for (int index = 0; index < Math.min(times.size(), weatherCodes.size()); index++) {
            LocalDateTime candidateTime = LocalDateTime.parse(times.get(index), formatter);
            long difference = Math.abs(java.time.Duration.between(candidateTime, departureDateTime).toMinutes());

            if (difference < smallestDifference) {
                smallestDifference = difference;
                bestIndex = index;
            }
        }

        return weatherCodes.get(bestIndex);
    }
}
