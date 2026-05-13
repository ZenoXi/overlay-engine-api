package com.zenox.overlayenginewebapi.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.zenox.overlayenginewebapi.model.response.osuweb.BeatmapUserScore;
import com.zenox.overlayenginewebapi.model.response.osuweb.BeatmapUserScores;
import com.zenox.overlayenginewebapi.model.response.osuweb.LeaderboardPage;
import com.zenox.overlayenginewebapi.model.response.osuweb.Response;
import com.zenox.overlayenginewebapi.model.response.osuweb.User;
import com.zenox.overlayenginewebapi.model.response.osuweb.UserScore;
import lombok.RequiredArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

@Service
@CommonsLog
@RequiredArgsConstructor
public class RawOsuWebApiCallService {

    private final TokenService tokenService;

    @Value("${osuweb.endpoint-url}")
    private String endpointUrl;
    @Value("${osuweb.api-path}")
    private String apiPath;

    public Response<User> getUser(String userId, String mode) {
        String token = tokenService.tryGetToken();
        if (token == null) {
            return new Response<>(HttpStatus.UNAUTHORIZED, "", null);
        }

        try {
            log.trace("osu!web getUser[userId=" + userId + ",mode=" + mode + "]");

            HttpRequest userRequest = HttpRequest.newBuilder()
                    .uri(createUriForPath("/users/" + userId + "/" + mode))
                    .GET()
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .header("Authorization", "Bearer " + token)
                    .build();

            HttpResponse<String> userResponse = HttpClient.newHttpClient().send(userRequest, HttpResponse.BodyHandlers.ofString());
            if (userResponse.statusCode() != HttpStatus.OK.value()) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
            }

            ObjectMapper mapper = new ObjectMapper();
            return new Response<>(mapper.readValue(userResponse.body(), User.class));
        } catch (IOException | InterruptedException e) {
            log.error("Exception while getting user", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public Response<List<UserScore>> getUserScores(String userId, String mode) {
        String token = tokenService.tryGetToken();
        if (token == null) {
            return new Response<>(HttpStatus.UNAUTHORIZED, "", null);
        }

        try {
            log.trace("osu!web getUserScores[userId=" + userId + ",mode=" + mode + "]");

            List<UserScore> allUserScores = new ArrayList<>();

            int loopLimit = 10;
            for (int i = 1; i <= loopLimit; i++) {
                HttpRequest scoresRequest = HttpRequest.newBuilder()
                        .uri(createUriForPath("/users/" + userId + "/scores/best?limit=100&offset=" + allUserScores.size() + "&mode=" + mode))
                        .GET()
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .header("Authorization", "Bearer " + token)
                        .build();
                HttpResponse<String> scoresResponse = HttpClient.newHttpClient().send(scoresRequest, HttpResponse.BodyHandlers.ofString());
                if (scoresResponse.statusCode() != HttpStatus.OK.value()) {
                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
                }

                ObjectMapper mapper = new ObjectMapper();
                CollectionType userScoresType = mapper.getTypeFactory().constructCollectionType(List.class, UserScore.class);
                List<UserScore> userScores = mapper.readValue(scoresResponse.body(), userScoresType);
                allUserScores.addAll(userScores);

                if (userScores.isEmpty()) {
                    break;
                }
            }
            return new Response<>(allUserScores);
        } catch (IOException | InterruptedException e) {
            log.error("Exception while getting user scores", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public Response<LeaderboardPage> getLeaderboardPage(int pageNumber, String mode, String countryCode) {
        String token = tokenService.tryGetToken();
        if (token == null) {
            return new Response<>(HttpStatus.UNAUTHORIZED, "", null);
        }

        try {
            log.trace("osu!web getLeaderboardPage[pageNumber=" + pageNumber + ",mode=" + mode + ",countryCode" + (countryCode != null ? countryCode : "null") + "]");

            HttpRequest leaderboardPageRequest = HttpRequest.newBuilder()
                    .uri(createUriForPath("/rankings/" + mode + "/performance?cursor[page]=" + pageNumber + (countryCode != null ? "&country=" + countryCode : "")))
                    .GET()
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .header("Authorization", "Bearer " + token)
                    .build();
            HttpResponse<String> leaderboardPageResponse = HttpClient.newHttpClient().send(leaderboardPageRequest, HttpResponse.BodyHandlers.ofString());
            if (leaderboardPageResponse.statusCode() != HttpStatus.OK.value()) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
            }

            ObjectMapper mapper = new ObjectMapper();
            return new Response<>(mapper.readValue(leaderboardPageResponse.body(), LeaderboardPage.class));
        } catch (IOException | InterruptedException e) {
            log.error("Exception while getting leaderboard page", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public Response<BeatmapUserScore> getBeatmapUserScore(String beatmapId, String userId, String mode) {
        String token = tokenService.tryGetToken();
        if (token == null) {
            return new Response<>(HttpStatus.UNAUTHORIZED, "", null);
        }

        try {
            log.trace("osu!web getBeatmapUserScore[beatmapId=" + beatmapId + ",userId=" + userId + ",mode" + mode + "]");

            HttpRequest beatmapUserScoreRequest = HttpRequest.newBuilder()
                    .uri(createUriForPath("/beatmaps/" + beatmapId + "/scores/users/" + userId + "?mode=" + mode))
                    .GET()
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .header("Authorization", "Bearer " + token)
                    .build();
            HttpResponse<String> beatmapUserScoreResponse = HttpClient.newHttpClient().send(beatmapUserScoreRequest, HttpResponse.BodyHandlers.ofString());
            if (beatmapUserScoreResponse.statusCode() == HttpStatus.NOT_FOUND.value()) {
                return new Response<>(HttpStatus.NOT_FOUND, "", null);
            } else if (beatmapUserScoreResponse.statusCode() != HttpStatus.OK.value()) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
            }

            ObjectMapper mapper = new ObjectMapper();
            return new Response<>(mapper.readValue(beatmapUserScoreResponse.body(), BeatmapUserScore.class));
        } catch (IOException | InterruptedException e) {
            log.error("Exception while getting user score on beatmap", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public Response<BeatmapUserScores> getBeatmapUserScores(String beatmapId, String userId, String mode) {
        String token = tokenService.tryGetToken();
        if (token == null) {
            return new Response<>(HttpStatus.UNAUTHORIZED, "", null);
        }

        try {
            log.trace("osu!web getBeatmapUserScores[beatmapId=" + beatmapId + ",userId=" + userId + ",mode" + mode + "]");

            HttpRequest beatmapUserScoresRequest = HttpRequest.newBuilder()
                    .uri(createUriForPath("/beatmaps/" + beatmapId + "/scores/users/" + userId + "/all?mode=" + mode))
                    .GET()
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .header("Authorization", "Bearer " + token)
                    .build();
            HttpResponse<String> beatmapUserScoresResponse = HttpClient.newHttpClient().send(beatmapUserScoresRequest, HttpResponse.BodyHandlers.ofString());
            if (beatmapUserScoresResponse.statusCode() != HttpStatus.OK.value()) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
            }

            ObjectMapper mapper = new ObjectMapper();
            return new Response<>(mapper.readValue(beatmapUserScoresResponse.body(), BeatmapUserScores.class));
        } catch (IOException | InterruptedException e) {
            log.error("Exception while getting user scores on beatmap", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private URI createUriForPath(String path) {
        return URI.create(endpointUrl + apiPath + path);
    }
}
