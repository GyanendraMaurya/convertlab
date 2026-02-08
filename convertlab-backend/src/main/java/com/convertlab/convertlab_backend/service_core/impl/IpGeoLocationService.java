package com.convertlab.convertlab_backend.service_core.impl;

import com.convertlab.convertlab_backend.service_core.GeoLocationService;
import com.convertlab.convertlab_backend.service_core.pojos.GeoLocation;
import com.convertlab.convertlab_backend.service_util.GeoLocationUtil;
import com.convertlab.convertlab_backend.service_util.IpUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Log4j2
@RequiredArgsConstructor
@Service
public class IpGeoLocationService implements GeoLocationService {
    private final ObjectMapper objectMapper;

    // Simple in-memory cache: IP -> GeoLocation
    private final Map<String, GeoLocation> locationCache = new ConcurrentHashMap<>();

    @Value("${geo.location.api.url}")
    private String geoApiUrl;


    @Value("${geo.location.api.key}")
    private String apiKey;

    private static final Duration TIMEOUT = Duration.ofSeconds(3);

    /**
     * Resolve IP address to geographic location asynchronously
     *
     * @param ipAddress The IP address to resolve
     * @return CompletableFuture with GeoLocation data
     */
    @Async
    public CompletableFuture<GeoLocation> resolveLocationAsync(String ipAddress) {
        return CompletableFuture.supplyAsync(() -> resolveLocation(ipAddress));
    }

    @Override
    public Map<String, GeoLocation> getLocationCache() {
        return locationCache;
    }

    @Override
    public void clearCache() {
        log.info("Clearing geolocation cache ({} entries)", locationCache.size());
        locationCache.clear();
    }

    @Override
    public int getCacheSize() {
        return locationCache.size();
    }

    /**
     * Resolve IP address to geographic location (synchronous)
     * Checks cache first, then calls API if needed
     *
     * @param ipAddress The IP address to resolve
     * @return GeoLocation data
     */
    public GeoLocation resolveLocation(String ipAddress) {
        if (GeoLocationUtil.isLocalOrPrivateIp(ipAddress)) {
            log.debug("Skipping geolocation for local/private IP: {}", ipAddress);
            return GeoLocation.unknown();
        }

        // Check cache first
        if (locationCache.containsKey(ipAddress)) {
            log.debug("Geolocation cache hit for IP: {}", ipAddress);
            return locationCache.get(ipAddress);
        }

        // Call API
        try {
            log.debug("Fetching geolocation for IP: {}", ipAddress);
            GeoLocation location = fetchFromApi(ipAddress);

            // Cache successful results
            if (location.isSuccess()) {
                locationCache.put(ipAddress, location);
                log.info("Geolocation resolved for IP {}: {}, {}",
                        IpUtil.maskIp(ipAddress), location.getCity(), location.getCountry());
            }

            return location;

        } catch (Exception e) {
            log.error("Failed to resolve geolocation for IP: {}", ipAddress, e);
            return GeoLocation.failed(e.getMessage());
        }
    }

    /**
     * Fetch location data from ipgeolocation.io
     */
    private GeoLocation fetchFromApi(String ipAddress) throws Exception {
        HttpResponse<String> response;
        try (HttpClient client = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .build()) {

            String uri = geoApiUrl + "?apiKey=" + apiKey + "&ip=" + ipAddress+ "&fields=location.country_name,location.country_code2,location.city";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(uri))
                    .timeout(TIMEOUT)
                    .GET()
                    .build();

            response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());
        }

        if (response.statusCode() != 200) {
            throw new RuntimeException("API returned status: " + response.statusCode() + " message" + objectMapper.readTree(response.body()));
        }

        return parseApiResponse(response.body());
    }

    /**
     * Parse JSON response from ip-api.com
     * <pre>
     * Example response:
     * {
     *      "location":
     *          {
     *              "country_name": "India"
     *              "country_code2": "IN",
     *              "city": "Mumbai"
     *          }
     * }
     * </pre>
     */
    private GeoLocation parseApiResponse(String jsonResponse) throws Exception {
        JsonNode root = objectMapper.readTree(jsonResponse);
        JsonNode location = root.path("location");
        return GeoLocation.builder()
                .success(true)
                .country(location.path("country_name").asString("Unknown"))
                .countryCode(location.path("country_code2").asString("XX"))
                .city(location.path("city").asString("Unknown"))
                .build();
    }
}
