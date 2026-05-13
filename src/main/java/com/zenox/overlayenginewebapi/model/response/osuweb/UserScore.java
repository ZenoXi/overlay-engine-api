package com.zenox.overlayenginewebapi.model.response.osuweb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserScore {
    private Float pp;
    private Beatmap beatmap;
    private Weight weight;

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Beatmap {
        private Integer id;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Weight {
        private Float pp;
    }
}
