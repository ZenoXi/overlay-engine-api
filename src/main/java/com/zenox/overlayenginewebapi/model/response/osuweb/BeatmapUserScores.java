package com.zenox.overlayenginewebapi.model.response.osuweb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class BeatmapUserScores {
    private List<Score> scores;

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Score {
        private Long id;
        private String mode;
        private Float pp;
        private Long score;
    }
}
