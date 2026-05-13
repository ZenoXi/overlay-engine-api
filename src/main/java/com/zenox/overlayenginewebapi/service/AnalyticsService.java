package com.zenox.overlayenginewebapi.service;

import com.zenox.overlayenginewebapi.model.Event;
import lombok.RequiredArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
@RequiredArgsConstructor
@CommonsLog
public class AnalyticsService {

    @Value("${analytics-enabled:false}")
    private boolean analyticsEnabled;
    @Value("${posthog.endpoint-url}")
    private String endpointUrl;
    @Value("${posthog.api-path}")
    private String apiPath;
    @Value("${posthog.api-key}")
    private String apiKey;

    public void notifyEvent(Event event) {
        try {
            if (!analyticsEnabled) {
                return;
            }

            String body = String.format("{\"api_key\":\"%s\",\"event\":\"%s\",\"distinct_id\":\"%s\"}", apiKey, event.name(), event.userId());

            HttpRequest eventRequest = HttpRequest.newBuilder()
                    .uri(URI.create(endpointUrl + apiPath))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .header("Content-Type", "application/json")
                    .build();

            HttpClient.newHttpClient().send(eventRequest, HttpResponse.BodyHandlers.discarding());
        } catch (IOException | InterruptedException e) {
            log.error("Exception while sending event", e);
        }
    }
}
