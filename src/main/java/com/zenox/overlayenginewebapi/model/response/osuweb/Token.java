package com.zenox.overlayenginewebapi.model.response.osuweb;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class Token {
    @JsonProperty("expires_in")
    private Long expiresIn;
    @JsonProperty("access_token")
    private String secret;
}
