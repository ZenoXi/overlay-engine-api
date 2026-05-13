package com.zenox.overlayenginewebapi.service;

import com.zenox.overlayenginewebapi.model.LeaderboardStorage;
import com.zenox.overlayenginewebapi.model.PlayerStorage;
import com.zenox.overlayenginewebapi.model.response.LeaderboardPageResponse;
import com.zenox.overlayenginewebapi.model.response.osuweb.LeaderboardPage;
import com.zenox.overlayenginewebapi.model.response.osuweb.Response;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LeaderboardService {

    @Getter
    @Setter
    private static class ModeData {
        private LeaderboardStorage globalLeaderboard = new LeaderboardStorage();
        private Map<String, LeaderboardStorage> countryLeaderboards = new HashMap<>();
        private PlayerStorage players = new PlayerStorage();
    }

    private final Map<String, ModeData> modes = new HashMap<>();
    private final Object storageLock = new Object();

    private static final long LEADERBOARD_PAGE_CACHE_DURATION = 600;

    private final RawOsuWebApiCallService rawOsuWebApiCallService;

    public Integer getLeaderboardPageCount(String mode, String countryCode) {
        synchronized (storageLock) {
            Integer pageCount = getLeaderboardPageCountFromStorage(mode, countryCode);
            if (pageCount != null) {
                return pageCount;
            }
        }

        Response<LeaderboardPage> leaderboardPageResponse = rawOsuWebApiCallService.getLeaderboardPage(1, mode, countryCode);
        if (leaderboardPageResponse.getStatus() != HttpStatus.OK) {
            return null;
        }

        synchronized (storageLock) {
            return updateLeaderboardPageCountInStorage(mode, countryCode, leaderboardPageResponse.getContent().getTotal());
        }
    }

    public LeaderboardPageResponse getLeaderboardPage(int pageNumber, String mode, String countryCode) {
        synchronized (storageLock) {
            var savedPage = getLeaderboardPageFromStorage(pageNumber, mode, countryCode);
            if (!isPageInvalid(savedPage)) {
                return LeaderboardPageResponse.builder()
                        .pageNumber(pageNumber)
                        .users(savedPage.getUsers().stream()
                                .map(user -> new LeaderboardPageResponse.User(user.getUsername(), user.getPp()))
                                .toList())
                        .build();
            }
        }

        Response<LeaderboardPage> leaderboardPageResponse = rawOsuWebApiCallService.getLeaderboardPage(pageNumber, mode, countryCode);
        if (leaderboardPageResponse.getStatus() != HttpStatus.OK) {
            return null;
        }

        synchronized (storageLock) {
            var page = new LeaderboardStorage.LeaderboardPage();

            leaderboardPageResponse.getContent().getRanking().forEach(user -> {
                var mappedUser = new LeaderboardStorage.LeaderboardPage.User();
                mappedUser.setId(user.getUser().getId());
                mappedUser.setUsername(user.getUser().getUsername());
                mappedUser.setPp(user.getPp());
                page.getUsers().add(mappedUser);
            });

            updateLeaderboardPageInStorage(pageNumber, mode, countryCode, page);
            updateLeaderboardPageCountInStorage(mode, countryCode, leaderboardPageResponse.getContent().getTotal());
        }

        return LeaderboardPageResponse.builder()
                .pageNumber(pageNumber)
                .users(leaderboardPageResponse.getContent().getRanking().stream()
                        .map(ranking -> new LeaderboardPageResponse.User(ranking.getUser().getUsername(), ranking.getPp()))
                        .toList())
                .build();
    }

    private LeaderboardStorage.LeaderboardPage getLeaderboardPageFromStorage(int pageNumber, String mode, String countryCode) {
        ModeData modeData = modes.get(mode);
        if (modeData == null) {
            return null;
        }

        if (countryCode == null) {
            return modeData.getGlobalLeaderboard().getPages().get(pageNumber);
        }

        var countryData = modeData.getCountryLeaderboards().get(countryCode);
        if (countryData == null) {
            return null;
        }
        return countryData.getPages().get(pageNumber);
    }

    private Integer getLeaderboardPageCountFromStorage(String mode, String countryCode) {
        ModeData modeData = modes.get(mode);
        if (modeData == null) {
            return null;
        }

        if (countryCode == null) {
            return modeData.getGlobalLeaderboard().getPageCount();
        }

        var countryData = modeData.getCountryLeaderboards().get(countryCode);
        if (countryData == null) {
            return null;
        }
        return countryData.getPageCount();
    }

    private void updateLeaderboardPageInStorage(int pageNumber, String mode, String countryCode, LeaderboardStorage.LeaderboardPage page) {
        ModeData modeData = modes.computeIfAbsent(mode, s -> new ModeData());

        LeaderboardStorage resolvedStorage;
        if (countryCode == null) {
            resolvedStorage = modeData.getGlobalLeaderboard();
        } else {
            resolvedStorage = modeData.getCountryLeaderboards().computeIfAbsent(countryCode, k -> new LeaderboardStorage());
        }
        resolvedStorage.getPages().put(pageNumber, page);

        // Invalidate all leaderboard pages that contain duplicate users
        LeaderboardStorage resolvedStorageFinal = resolvedStorage;
        page.getUsers().forEach(user -> {
            resolvedStorageFinal.getPages().values().forEach(lbPage -> {
                if (lbPage == page || isPageInvalid(lbPage)) {
                    return;
                }

                if (lbPage.getUsers().stream().anyMatch(lbUser -> lbUser.getId().equals(user.getId()))) {
                    lbPage.invalidate();
                }
            });
        });
    }

    private Integer updateLeaderboardPageCountInStorage(String mode, String countryCode, Integer playerCountFromApi) {
        ModeData modeData = modes.computeIfAbsent(mode, s -> new ModeData());

        LeaderboardStorage resolvedStorage;
        if (countryCode == null) {
            resolvedStorage = modeData.getGlobalLeaderboard();
        } else {
            resolvedStorage = modeData.getCountryLeaderboards().computeIfAbsent(countryCode, k -> new LeaderboardStorage());
        }
        resolvedStorage.updatePageCountFromApiPlayerCount(playerCountFromApi);
        return resolvedStorage.getPageCount();
    }

    private boolean isPageInvalid(LeaderboardStorage.LeaderboardPage page) {
        return page == null || page.getLastUpdatedAt().plusSeconds(LEADERBOARD_PAGE_CACHE_DURATION).isBefore(Instant.now());
    }
}
