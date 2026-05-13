package com.zenox.overlayenginewebapi.model;

import com.zenox.overlayenginewebapi.model.response.osuweb.User;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
public class PlayerStorage {

    @Getter
    @Setter
    public static class Player {
        private Integer id;
        private String username;
        private String countryCode;
        private Integer globalRank;
        private Integer countryRank;
        private Float pp;

        public static Player fromUser(User user) {
            var player = new Player();
            player.setId(user.getId());
            player.setUsername(user.getUsername());
            player.setCountryCode(user.getCountryCode());
            player.setGlobalRank(user.getStatistics().getGlobalRank());
            player.setCountryRank(user.getStatistics().getCountryRank());
            player.setPp(user.getStatistics().getPp());
            return player;
        }
    }

    List<Player> players;
}
