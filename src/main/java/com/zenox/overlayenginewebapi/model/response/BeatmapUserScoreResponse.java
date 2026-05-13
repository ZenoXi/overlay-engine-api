package com.zenox.overlayenginewebapi.model.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class BeatmapUserScoreResponse {
    private Float pp;
    private Boolean isBest;
}
