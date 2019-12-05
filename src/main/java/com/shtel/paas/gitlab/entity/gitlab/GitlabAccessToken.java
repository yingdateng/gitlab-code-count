package com.shtel.paas.gitlab.entity.gitlab;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Date;

/**
 * Created by chenzhen on 2018/8/27.
 */
@Data
public class GitlabAccessToken {
    public static final String URL = "/oauth/token";

    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("token_type")
    private String tokenType;

    @JsonProperty("refresh_token")
    private String refreshToken;

    private String scope;

    @JsonProperty("created_at")
    private Date createdAt;

    @Override
    public String toString() {
        return "GitlabAccessToken{" +
                "accessToken='" + accessToken + '\'' +
                ", tokenType='" + tokenType + '\'' +
                ", refreshToken='" + refreshToken + '\'' +
                ", scope='" + scope + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}

