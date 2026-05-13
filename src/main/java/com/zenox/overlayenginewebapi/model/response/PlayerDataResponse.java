package com.zenox.overlayenginewebapi.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
@Builder
public class PlayerDataResponse {
    private String userId;
    private String username;
    private float pp;
    private List<Score> scores;

    @Getter
    @AllArgsConstructor
    public static class Score {
        // TODO: remove
        private int rank;
        private float pp;
        private float ppWeighted;
        private String mapId;
    }
}
