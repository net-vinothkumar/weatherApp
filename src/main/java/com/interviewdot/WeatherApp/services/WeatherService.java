package com.interviewdot.WeatherApp.services;

import com.interviewdot.WeatherApp.dto.WeatherAverageDTO;
import com.interviewdot.WeatherApp.dto.WeatherMapDTO;
import com.interviewdot.WeatherApp.dto.WeatherMapTimeDTO;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import springfox.documentation.spring.web.json.Json;
import com.alicp.jetcache.anno.Cached;


@Service
public class WeatherService {
    private final String URI = "http://api.openweathermap.org/data/2.5/forecast";
    private final String API_ID = "YOUR_API_ID";

    private final RestTemplate restTemplate;

    public WeatherService(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder.build();
    }

    @Cached(expire = 10, timeUnit = TimeUnit.MINUTES)
    public ResponseEntity<?> weatherForecastAverage(String city) {
        List<WeatherAverageDTO> result = new ArrayList<WeatherAverageDTO>();
        try {
            WeatherMapDTO weatherMap = this.restTemplate.getForObject(this.url(city), WeatherMapDTO.class);

            for (LocalDate reference = LocalDate.now();
                 reference.isBefore(LocalDate.now().plusDays(4));
                 reference = reference.plusDays(1)) {
                final LocalDate ref = reference;
                List<WeatherMapTimeDTO> collect = weatherMap.getList().stream()
                        .filter(x -> x.getDt().toLocalDate().equals(ref)).collect(Collectors.toList());
                if (!CollectionUtils.isEmpty(collect)) {
                    result.add(this.average(collect));
                }

            }
        } catch (HttpClientErrorException e) {
            return new ResponseEntity<>(new Json(e.getResponseBodyAsString()), e.getStatusCode());
        }

        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    private WeatherAverageDTO average(List<WeatherMapTimeDTO> list) {
        WeatherAverageDTO result = new WeatherAverageDTO();

        for (WeatherMapTimeDTO item : list) {
            result.setDate(item.getDt().toLocalDate());
            result.plusMap(item);
        }

        result.totalize();

        return result;
    }

    private String url(String city) {
        return String.format(URI.concat("?q=%s").concat("&appid=%s").concat("&units=metric"), city, API_ID);
    }
}
