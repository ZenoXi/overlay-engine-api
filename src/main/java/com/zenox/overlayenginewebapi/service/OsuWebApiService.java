package com.zenox.overlayenginewebapi.service;

import com.zenox.overlayenginewebapi.model.response.BeatmapUserScoreResponse;
import com.zenox.overlayenginewebapi.model.response.LeaderboardPageResponse;
import com.zenox.overlayenginewebapi.model.response.PlayerDataResponse;
import com.zenox.overlayenginewebapi.model.response.osuweb.BeatmapUserScores;
import com.zenox.overlayenginewebapi.model.response.osuweb.Response;
import com.zenox.overlayenginewebapi.model.response.osuweb.User;
import com.zenox.overlayenginewebapi.model.response.osuweb.UserScore;
import lombok.RequiredArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@CommonsLog
public class OsuWebApiService {

    private final RawOsuWebApiCallService rawOsuWebApiCallService;
    private final LeaderboardService leaderboardService;

    public PlayerDataResponse getPlayerData(String userId, String mode) {
        Response<User> userResponse = rawOsuWebApiCallService.getUser(userId, mode);
        if (userResponse.getStatus() != HttpStatus.OK) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        Response<List<UserScore>> userScoresResponse = rawOsuWebApiCallService.getUserScores(userId, mode);
        if (userScoresResponse.getStatus() != HttpStatus.OK) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return PlayerDataResponse.builder()
                .userId(userResponse.getContent().getId().toString())
                .username(userResponse.getContent().getUsername())
                .pp(userResponse.getContent().getStatistics().getPp())
                .scores(userScoresResponse.getContent().stream()
                        .map(score -> new PlayerDataResponse.Score(1, score.getPp(), score.getWeight().getPp(), score.getBeatmap().getId().toString()))
                        .toList())
                .build();
    }

    public List<LeaderboardPageResponse> getInitialLeaderboard(float pp, String mode, String countryCode) {

        Integer pageCount = leaderboardService.getLeaderboardPageCount(mode, countryCode);
        if (pageCount == null || pageCount < 1) {
            log.warn("No pages found for the leaderboard");
            return List.of();
        }

        int leftBound = 1;
        int rightBound = pageCount;
        int currentPage = (leftBound + rightBound) / 2;

        while (true) {
            //log.info("Searching page " + currentPage);
            LeaderboardPageResponse page = leaderboardService.getLeaderboardPage(currentPage, mode, countryCode);

            boolean ppFound = false;
            for (int i = 0; i < page.getUsers().size() - 1; i++) {
                float pp1 = page.getUsers().get(i).getPp();
                float pp2 = page.getUsers().get(i + 1).getPp();
                if (pp <= pp1 && pp >= pp2) {
                    ppFound = true;
                    break;
                }
            }
            if (ppFound) {
                break;
            }

            float maxPagePP = page.getUsers().get(0).getPp();
            float minPagePP = page.getUsers().get(page.getUsers().size() - 1).getPp();
            float pageDelta = maxPagePP - minPagePP;
            boolean ppInPage = false;
            if (pp > maxPagePP) {
                rightBound = currentPage - 1;
                float totalDelta = pp - maxPagePP;
                currentPage -= pageDelta == 0.0f ? 1 : (int) (totalDelta / pageDelta + 1.0f);
                if (currentPage < leftBound) {
                    currentPage = leftBound;
                }
            } else if (pp < minPagePP) {
                leftBound = currentPage + 1;
                float totalDelta = minPagePP - pp;
                currentPage += pageDelta == 0.0f ? 1 : (int) (totalDelta / pageDelta + 1.0f);
                if (currentPage > rightBound) {
                    currentPage = rightBound;
                }
            } else {
                ppInPage = true;
            }

            if (leftBound >= rightBound || ppInPage) {
                break;
            }
        }

        int startPage = Math.max(currentPage - 1, 1);
        int endPage = Math.min(currentPage + 1, pageCount);
        List<LeaderboardPageResponse> initialLeaderboard = new ArrayList<>();
        for (int i = startPage; i <= endPage; i++) {
            LeaderboardPageResponse page = leaderboardService.getLeaderboardPage(i, mode, countryCode);
            if (page == null) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
            }
            initialLeaderboard.add(page);
        }

        return initialLeaderboard;
    }

    public LeaderboardPageResponse getLeaderboardPage(int pageNumber, String mode, String countryCode) {
        LeaderboardPageResponse page = leaderboardService.getLeaderboardPage(pageNumber, mode, countryCode);
        if (page == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return page;
    }

    public BeatmapUserScoreResponse getBeatmapUserScore(String beatmapId, String userId, String mode, String scoreId) {
        Response<BeatmapUserScores> beatmapUserScoresResponse = rawOsuWebApiCallService.getBeatmapUserScores(beatmapId, userId, mode);
        if (beatmapUserScoresResponse.getStatus() != HttpStatus.OK) {
            return null;
        }

        var scores = beatmapUserScoresResponse.getContent().getScores();
        if (scoreId != null && (scores.isEmpty() || scores.stream().noneMatch(score -> scoreId.equals(score.getId().toString())))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        var sortedScores = scores.stream()
                .filter(score -> mode.equals(score.getMode()))
                .sorted((score1, score2) -> {
                    // Sort descending by pp, then score
                    // null pp values go last, score can't be null

                    int ppComp;
                    if (score1.getPp() != null && score2.getPp() == null) {
                        ppComp = 1;
                    } else if (score1.getPp() == null && score2.getPp() != null) {
                        ppComp = -1;
                    } else if (score1.getPp() == null) {
                        ppComp = 0;
                    } else {
                        ppComp = score1.getPp().compareTo(score2.getPp());
                    }
                    ppComp = -ppComp;
                    if (ppComp != 0) {
                        return ppComp;
                    }

                    return -score1.getScore().compareTo(score2.getScore());
                })
                .toList();

        if (sortedScores.isEmpty()) {
            return null;
        }

        String bestScoreId = sortedScores.get(0).getId().toString();
        return sortedScores.stream()
                .filter(score -> scoreId == null || scoreId.equals(score.getId().toString()))
                .map(score -> BeatmapUserScoreResponse.builder()
                        .pp(score.getPp())
                        .isBest(bestScoreId.equals(score.getId().toString()))
                        .build())
                .findFirst()
                .orElse(null);

//        if (scoreId == null || scoreId.equals(sortedScores.get(0).getId().toString())) {
//            return BeatmapUserScoreResponse.builder()
//                    .pp(sortedScores.get(0).getPp())
//                    .isBest(true)
//                    .build();
//        }
//
//        return sortedScores.stream()
//                .filter(score -> scoreId.equals(score.getId().toString()))
//                .map(score -> BeatmapUserScoreResponse.builder()
//                        .pp(score.getPp())
//                        .isBest(false)
//                        .build())
//                .findFirst()
//                .orElse(null);

//        if (scoreId.equals(sortedScores.get(0).getId().toString())) {
//            return BeatmapUserScoreResponse.builder()
//                    .pp(score.getPp())
//                    .build() sortedScores.get(0)
//        }
//
//        return beatmapUserScoresResponse.getContent().getScores().stream()
//                .filter(score -> mode.equals(score.getMode()) && (scoreId == null || scoreId.equals(score.getId().toString())))
//                .sorted((score1, score2) -> {
//                    // Sort descending by pp, then score
//                    // null pp values go last, score can't be null
//
//                    int ppComp;
//                    if (score1.getPp() != null && score2.getPp() == null) {
//                        ppComp = 1;
//                    } else if (score1.getPp() == null && score2.getPp() != null) {
//                        ppComp = -1;
//                    } else if (score1.getPp() == null) {
//                        ppComp = 0;
//                    } else {
//                        ppComp = score1.getPp().compareTo(score2.getPp());
//                    }
//                    ppComp = -ppComp;
//                    if (ppComp != 0) {
//                        return ppComp;
//                    }
//
//                    return -score1.getScore().compareTo(score2.getScore());
//                })
//                .map(score -> BeatmapUserScoreResponse.builder()
//                        .pp(score.getPp())
//                        .build())
//                .findFirst()
//                .orElse(null);
        //Comparator;
        //Comparator.reverseOrder()


//        Response<BeatmapUserScore> beatmapUserScoreResponse = rawOsuWebApiCallService.getBeatmapUserScore(beatmapId, userId, mode);
//        if (beatmapUserScoreResponse.getStatus() == HttpStatus.NOT_FOUND || beatmapUserScoreResponse.getContent().getScore().getBeatmap().getRanked() != 1) {
//            return null;
//        }
//
//        return BeatmapUserScoreResponse.builder()
//                .pp(beatmapUserScoreResponse.getContent().getScore().getPp())
//                .build();
    }
}
