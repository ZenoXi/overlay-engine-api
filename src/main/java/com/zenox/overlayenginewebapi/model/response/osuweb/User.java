package com.zenox.overlayenginewebapi.model.response.osuweb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class User {
    private Integer id;
    private String username;
    @JsonProperty("country_code")
    private String countryCode;
    private Statistics statistics;

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Statistics {
        @JsonProperty("global_rank")
        private Integer globalRank;
        @JsonProperty("country_rank")
        private Integer countryRank;
        private Float pp;
    }
}
