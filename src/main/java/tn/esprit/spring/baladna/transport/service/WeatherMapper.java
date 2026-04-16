package tn.esprit.spring.baladna.transport.service;

import org.springframework.stereotype.Component;
import tn.esprit.spring.baladna.transport.entity.WeatherCondition;

@Component
public class WeatherMapper {

    public WeatherCondition mapWeatherCode(Integer code) {
        if (code == null) {
            return WeatherCondition.SUNNY;
        }

        if (code == 95 || code == 96 || code == 99) {
            return WeatherCondition.STORM;
        }

        if (code == 45 || code == 48 || code == 71 || code == 73 || code == 75 || code == 77) {
            return WeatherCondition.SANDSTORM;
        }

        if ((code >= 51 && code <= 67) || (code >= 80 && code <= 82)) {
            return WeatherCondition.RAIN;
        }

        return WeatherCondition.SUNNY;
    }
}
