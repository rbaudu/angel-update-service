package com.angel.update.collector;

import com.angel.update.service.CacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Collecteur de données météorologiques
 */
@Component
@ConditionalOnProperty(name = "angel.collectors.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class WeatherCollector extends BaseCollector {
    
    private final CacheService cacheService;
    private final RestTemplate restTemplate = new RestTemplate();
    
    @Value("${angel.collectors.weather.api-key:demo}")
    private String apiKey;
    
    @Value("${angel.collectors.mock-mode:false}")
    private boolean mockMode;
    
    // Principales villes par pays
    private final Map<String, List<String>> majorCities = Map.of(
            "FR", List.of("Paris", "Lyon", "Marseille", "Toulouse", "Nice"),
            "US", List.of("New York", "Los Angeles", "Chicago", "Houston", "Phoenix"),
            "GB", List.of("London", "Birmingham", "Manchester", "Glasgow", "Liverpool"),
            "DE", List.of("Berlin", "Hamburg", "Munich", "Cologne", "Frankfurt")
    );
    
    @Override
    public String getCollectorName() {
        return "WeatherCollector";
    }
    
    @Override
    public String getContentType() {
        return "weather";
    }
    
    @Override
    public void collect() throws Exception {
        collectWeather();
    }
    
    /**
     * Collecte programmée des données météo (toutes les 15 minutes)
     */
    @Scheduled(cron = "${angel.collectors.schedule.weather:0 */15 * * * *}")
    public void collectWeather() {
        if (!isEnabled()) {
            log.debug("Weather collector is disabled");
            return;
        }
        
        log.info("Starting weather data collection...");
        
        try {
            // Collecter pour les principaux pays
            for (String countryCode : majorCities.keySet()) {
                collectWeatherForCountry(countryCode);
            }
            
            updateCollectorStatus(com.angel.update.model.CollectorStatus.Status.ACTIVE, "Weather collection completed successfully");
            log.info("Weather collection completed successfully");
            
        } catch (Exception e) {
            updateCollectorStatus(com.angel.update.model.CollectorStatus.Status.ERROR, "Weather collection failed: " + e.getMessage());
            log.error("Error during weather collection", e);
        }
    }
    
    /**
     * Collecte les données météo pour un pays
     */
    private void collectWeatherForCountry(String countryCode) {
        List<String> cities = majorCities.get(countryCode);
        if (cities == null) return;
        
        for (String city : cities) {
            try {
                WeatherData weatherData;
                
                if (mockMode) {
                    weatherData = generateMockWeather(city, countryCode);
                } else {
                    weatherData = fetchWeatherFromAPI(city, countryCode);
                }
                
                if (weatherData != null) {
                    // Mettre en cache
                    cacheService.cacheWeather(countryCode, getRegionForCity(city, countryCode), weatherData);
                    
                    // Sauvegarder en base
                    saveWeatherData(weatherData, countryCode);
                    
                    log.debug("Collected weather data for {}, {}", city, countryCode);
                }
                
            } catch (Exception e) {
                log.error("Error collecting weather for {}, {}", city, countryCode, e);
            }
        }
    }
    
    /**
     * Récupère les données météo depuis l'API
     */
    private WeatherData fetchWeatherFromAPI(String city, String countryCode) {
        String url = buildWeatherApiUrl(city, countryCode);
        
        try {
            WeatherApiResponse response = restTemplate.getForObject(url, WeatherApiResponse.class);
            
            if (response != null) {
                return mapToWeatherData(response, city, countryCode);
            }
            
        } catch (Exception e) {
            log.warn("Failed to fetch weather from API for {}: {}", city, url, e);
        }
        
        return null;
    }
    
    /**
     * Génère des données météo simulées
     */
    private WeatherData generateMockWeather(String city, String countryCode) {
        // Génération de données aléatoires réalistes
        double temperature = 15 + (Math.random() * 20); // 15-35°C
        double humidity = 40 + (Math.random() * 50); // 40-90%
        int windSpeed = (int) (Math.random() * 30); // 0-30 km/h
        
        String[] conditions = {"clear", "cloudy", "rainy", "sunny", "partly_cloudy"};
        String condition = conditions[(int) (Math.random() * conditions.length)];
        
        return WeatherData.builder()
                .city(city)
                .countryCode(countryCode)
                .temperature(temperature)
                .feelsLike(temperature + (Math.random() * 4 - 2)) // ±2°C
                .humidity((int) humidity)
                .pressure(1013 + (int) (Math.random() * 40 - 20)) // ±20 hPa
                .windSpeed(windSpeed)
                .windDirection((int) (Math.random() * 360))
                .visibility((int) (5 + Math.random() * 15)) // 5-20 km
                .uvIndex((int) (Math.random() * 11))
                .condition(condition)
                .description(getWeatherDescription(condition))
                .timestamp(LocalDateTime.now())
                .forecast(generateMockForecast(5))
                .build();
    }
    
    private List<ForecastDay> generateMockForecast(int days) {
        return java.util.stream.IntStream.range(0, days)
                .mapToObj(i -> ForecastDay.builder()
                        .date(LocalDateTime.now().plusDays(i))
                        .minTemp(10 + Math.random() * 15)
                        .maxTemp(20 + Math.random() * 15)
                        .condition(getRandomCondition())
                        .precipitationChance((int) (Math.random() * 100))
                        .build())
                .toList();
    }
    
    private void saveWeatherData(WeatherData weatherData, String countryCode) {
        // Implémentation de la sauvegarde
        log.debug("Saving weather data for {}, {}", weatherData.getCity(), countryCode);
    }
    
    private WeatherData mapToWeatherData(WeatherApiResponse response, String city, String countryCode) {
        // Mapping depuis la réponse API - à adapter selon l'API utilisée
        return WeatherData.builder()
                .city(city)
                .countryCode(countryCode)
                .temperature(response.getMain().getTemp())
                .feelsLike(response.getMain().getFeelsLike())
                .humidity(response.getMain().getHumidity())
                .pressure(response.getMain().getPressure())
                .condition(response.getWeather().get(0).getMain().toLowerCase())
                .description(response.getWeather().get(0).getDescription())
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    private String buildWeatherApiUrl(String city, String countryCode) {
        return String.format("https://api.openweathermap.org/data/2.5/weather?q=%s,%s&appid=%s&units=metric", 
                city, countryCode, apiKey);
    }
    
    private String getRegionForCity(String city, String countryCode) {
        // Mapping basique ville -> région
        if ("FR".equals(countryCode) && "Paris".equals(city)) {
            return "IDF";
        }
        return null; // National par défaut
    }
    
    private String getWeatherDescription(String condition) {
        return switch (condition) {
            case "clear" -> "Ciel dégagé";
            case "cloudy" -> "Nuageux";
            case "rainy" -> "Pluvieux";
            case "sunny" -> "Ensoleillé";
            case "partly_cloudy" -> "Partiellement nuageux";
            default -> "Conditions variables";
        };
    }
    
    private String getRandomCondition() {
        String[] conditions = {"clear", "cloudy", "rainy", "sunny", "partly_cloudy"};
        return conditions[(int) (Math.random() * conditions.length)];
    }
    
    /**
     * Classes pour les données météo
     */
    public static class WeatherData {
        private String city;
        private String countryCode;
        private double temperature;
        private double feelsLike;
        private int humidity;
        private int pressure;
        private int windSpeed;
        private int windDirection;
        private int visibility;
        private int uvIndex;
        private String condition;
        private String description;
        private LocalDateTime timestamp;
        private List<ForecastDay> forecast;
        
        public static Builder builder() {
            return new Builder();
        }
        
        // Getters
        public String getCity() { return city; }
        public String getCountryCode() { return countryCode; }
        public double getTemperature() { return temperature; }
        public double getFeelsLike() { return feelsLike; }
        public int getHumidity() { return humidity; }
        public int getPressure() { return pressure; }
        public int getWindSpeed() { return windSpeed; }
        public int getWindDirection() { return windDirection; }
        public int getVisibility() { return visibility; }
        public int getUvIndex() { return uvIndex; }
        public String getCondition() { return condition; }
        public String getDescription() { return description; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public List<ForecastDay> getForecast() { return forecast; }
        
        public static class Builder {
            private final WeatherData data = new WeatherData();
            
            public Builder city(String city) { data.city = city; return this; }
            public Builder countryCode(String countryCode) { data.countryCode = countryCode; return this; }
            public Builder temperature(double temperature) { data.temperature = temperature; return this; }
            public Builder feelsLike(double feelsLike) { data.feelsLike = feelsLike; return this; }
            public Builder humidity(int humidity) { data.humidity = humidity; return this; }
            public Builder pressure(int pressure) { data.pressure = pressure; return this; }
            public Builder windSpeed(int windSpeed) { data.windSpeed = windSpeed; return this; }
            public Builder windDirection(int windDirection) { data.windDirection = windDirection; return this; }
            public Builder visibility(int visibility) { data.visibility = visibility; return this; }
            public Builder uvIndex(int uvIndex) { data.uvIndex = uvIndex; return this; }
            public Builder condition(String condition) { data.condition = condition; return this; }
            public Builder description(String description) { data.description = description; return this; }
            public Builder timestamp(LocalDateTime timestamp) { data.timestamp = timestamp; return this; }
            public Builder forecast(List<ForecastDay> forecast) { data.forecast = forecast; return this; }
            
            public WeatherData build() { return data; }
        }
    }
    
    public static class ForecastDay {
        private LocalDateTime date;
        private double minTemp;
        private double maxTemp;
        private String condition;
        private int precipitationChance;
        
        public static Builder builder() {
            return new Builder();
        }
        
        public static class Builder {
            private final ForecastDay day = new ForecastDay();
            
            public Builder date(LocalDateTime date) { day.date = date; return this; }
            public Builder minTemp(double minTemp) { day.minTemp = minTemp; return this; }
            public Builder maxTemp(double maxTemp) { day.maxTemp = maxTemp; return this; }
            public Builder condition(String condition) { day.condition = condition; return this; }
            public Builder precipitationChance(int chance) { day.precipitationChance = chance; return this; }
            
            public ForecastDay build() { return day; }
        }
    }
    
    /**
     * Classes pour la désérialisation API (exemple OpenWeatherMap)
     */
    public static class WeatherApiResponse {
        private Main main;
        private List<Weather> weather;
        
        public Main getMain() { return main; }
        public void setMain(Main main) { this.main = main; }
        public List<Weather> getWeather() { return weather; }
        public void setWeather(List<Weather> weather) { this.weather = weather; }
        
        public static class Main {
            private double temp;
            private double feelsLike;
            private int humidity;
            private int pressure;
            
            public double getTemp() { return temp; }
            public void setTemp(double temp) { this.temp = temp; }
            public double getFeelsLike() { return feelsLike; }
            public void setFeelsLike(double feelsLike) { this.feelsLike = feelsLike; }
            public int getHumidity() { return humidity; }
            public void setHumidity(int humidity) { this.humidity = humidity; }
            public int getPressure() { return pressure; }
            public void setPressure(int pressure) { this.pressure = pressure; }
        }
        
        public static class Weather {
            private String main;
            private String description;
            
            public String getMain() { return main; }
            public void setMain(String main) { this.main = main; }
            public String getDescription() { return description; }
            public void setDescription(String description) { this.description = description; }
        }
    }
}