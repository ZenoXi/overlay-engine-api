package com.zenox.overlayenginewebapi.model.response.osuweb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class LeaderboardPage {
    private Integer total;
    private List<Ranking> ranking;

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Ranking {
        private Float pp;
        private User user;

        @Getter
        @Setter
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class User {
            private Integer id;
            private String username;
        }
    }
}
