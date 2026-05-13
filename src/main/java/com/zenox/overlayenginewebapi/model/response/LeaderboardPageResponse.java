package com.zenox.overlayenginewebapi.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
@Builder
public class LeaderboardPageResponse {
    private Integer pageNumber;
    private List<User> users;

    @Getter
    @AllArgsConstructor
    public static class User {
        private String username;
        private float pp;
    }
}
