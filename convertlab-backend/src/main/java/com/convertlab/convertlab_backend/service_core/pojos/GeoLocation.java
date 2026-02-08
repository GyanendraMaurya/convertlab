package com.convertlab.convertlab_backend.service_core.pojos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeoLocation {
    private String country;
    private String countryCode;
    private String city;
    private String region;
    private String timezone;

    // For error handling
    private boolean success;
    private String errorMessage;

    public static GeoLocation failed(String errorMessage) {
        return GeoLocation.builder()
                .success(false)
                .errorMessage(errorMessage)
                .build();
    }

    public static GeoLocation unknown() {
        return GeoLocation.builder()
                .success(true)
                .country("Unknown")
                .countryCode("XX")
                .city("Unknown")
                .build();
    }
}