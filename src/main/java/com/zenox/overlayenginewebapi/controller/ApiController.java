package com.zenox.overlayenginewebapi.controller;

import com.zenox.overlayenginewebapi.model.Event;
import com.zenox.overlayenginewebapi.model.response.BeatmapUserScoreResponse;
import com.zenox.overlayenginewebapi.model.response.LeaderboardPageResponse;
import com.zenox.overlayenginewebapi.model.response.PlayerDataResponse;
import com.zenox.overlayenginewebapi.service.AnalyticsService;
import com.zenox.overlayenginewebapi.service.OsuWebApiService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ApiController {

    private final OsuWebApiService osuWebApiService;
    private final AnalyticsService analyticsService;

    @GetMapping("/playerData")
    public PlayerDataResponse getPlayerData(@RequestParam("userId") String userId, @RequestParam("mode") String mode, HttpServletRequest request) {
        String requestUserId = UUID.nameUUIDFromBytes(request.getRemoteAddr().getBytes()).toString();
        analyticsService.notifyEvent(new Event("/playerData", requestUserId));
        return osuWebApiService.getPlayerData(userId, mode);
    }

    @GetMapping("/initialLeaderboard")
    public List<LeaderboardPageResponse> getInitialLeaderboard(@RequestParam("pp") Float pp, @RequestParam("mode") String mode, @RequestParam("country") Optional<String> countryCode, HttpServletRequest request) {
        String requestUserId = UUID.nameUUIDFromBytes(request.getRemoteAddr().getBytes()).toString();
        analyticsService.notifyEvent(new Event("/initialLeaderboard", requestUserId));
        return osuWebApiService.getInitialLeaderboard(pp == null ? 0.0f : pp, mode, countryCode.orElse(null));
    }

    @GetMapping("/leaderboardPage")
    public LeaderboardPageResponse getLeaderboardPage(@RequestParam("pageNumber") Integer pageNumber, @RequestParam("mode") String mode, @RequestParam("country") Optional<String> countryCode, HttpServletRequest request) {
        String requestUserId = UUID.nameUUIDFromBytes(request.getRemoteAddr().getBytes()).toString();
        analyticsService.notifyEvent(new Event("/leaderboardPage", requestUserId));
        if (pageNumber == null || pageNumber < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page number is not provided or invalid");
        }
        return osuWebApiService.getLeaderboardPage(pageNumber, mode, countryCode.orElse(null));
    }

    @GetMapping("/beatmapUserScore")
    public BeatmapUserScoreResponse getBeatmapUserScore(@RequestParam("beatmapId") String beatmapId, @RequestParam("userId") String userId, @RequestParam("mode") String mode, @RequestParam("scoreId") Optional<String> scoreId, HttpServletRequest request) {
        String requestUserId = UUID.nameUUIDFromBytes(request.getRemoteAddr().getBytes()).toString();
        analyticsService.notifyEvent(new Event("/beatmapUserScore", requestUserId));
        return osuWebApiService.getBeatmapUserScore(beatmapId, userId, mode, scoreId.orElse(null));
    }
}
