package com.zenox.overlayenginewebapi.model.response.osuweb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class BeatmapUserScore {
    private Score score;

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Score {
        private Float pp;
        private Beatmap beatmap;

        @Getter
        @Setter
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Beatmap {
            private Integer ranked;
        }
    }
}
