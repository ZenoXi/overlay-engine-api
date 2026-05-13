package com.zenox.overlayenginewebapi.model.response.osuweb;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public class Response<T> {

    public Response(T content) {
        this(HttpStatus.OK, "", content);
    }

    HttpStatus status;
    String error;
    T content;
}
