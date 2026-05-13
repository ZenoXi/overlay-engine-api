package com.zenox.overlayenginewebapi.model;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class LeaderboardStorage {

    @Getter
    @Setter
    public static class LeaderboardPage {

        @Getter
        @Setter
        public static class User {
            private Integer id;
            private String username;
            private float pp;
            private Integer profileRank;
        }

        private Instant lastUpdatedAt = Instant.now();
        private List<User> users = new ArrayList<>();

        public void invalidate() {
            lastUpdatedAt = Instant.MIN;
        }
    }

    //List<LeaderboardPage> pages;
    Integer pageCount = null;
    Map<Integer, LeaderboardPage> pages = new HashMap<>();

    public void updatePageCountFromApiPlayerCount(int playerCountFromApi) {
        pageCount = (playerCountFromApi - 1) / 50 + 1;
    }
}
