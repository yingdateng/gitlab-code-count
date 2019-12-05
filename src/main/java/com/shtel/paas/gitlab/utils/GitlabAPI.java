package com.shtel.paas.gitlab.utils;


import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shtel.paas.gitlab.entity.gitlab.AuthMethod;
import com.shtel.paas.gitlab.entity.gitlab.GitlabAccessToken;
import com.shtel.paas.gitlab.entity.gitlab.GitlabHTTPRequestor;
import com.shtel.paas.gitlab.entity.gitlab.TokenType;
import lombok.Data;

import java.io.IOException;
import java.net.Proxy;
import java.net.URL;

/**
 * Gitlab API
 */
@Data
public class GitlabAPI {

    public static final ObjectMapper MAPPER = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private static final String API_NAMESPACE = "/api/v4";
    private final String hostUrl;
    private final String apiToken;
    private final TokenType tokenType;
    private AuthMethod authMethod;

    private boolean ignoreCertificateErrors = false;
    private Proxy proxy;
    private int requestTimeout = 0;
    private String userAgent = GitlabAPI.class.getCanonicalName() + "/" + System.getProperty("java.version");


    private GitlabAPI(String hostUrl, String apiToken, TokenType tokenType, AuthMethod method) {
        this.hostUrl = hostUrl.endsWith("/") ? hostUrl.replaceAll("/$", "") : hostUrl;
        this.apiToken = apiToken;
        this.tokenType = tokenType;
        this.authMethod = method;
    }

    public static GitlabAPI connect(String hostUrl, String apiToken, TokenType tokenType, AuthMethod method) {
        return new GitlabAPI(hostUrl, apiToken, tokenType, method);
    }

    public GitlabHTTPRequestor dispatch() {
        return new GitlabHTTPRequestor(this).authenticate(apiToken, tokenType, authMethod).method("POST");
    }

    public static GitlabAccessToken getAccessToken(String hostUrl, String grantType, String username, String password) throws IOException {
        String tailUrl = GitlabAccessToken.URL;
        GitlabAPI api = connect(hostUrl, null, null, null);
        return api.dispatch()
                .with("grant_type", grantType)
                .with("username", username)
                .with("password", password)
                .toAT(tailUrl, GitlabAccessToken.class);
    }

    public URL getUrl(String tailAPIUrl) throws IOException {
        if (!tailAPIUrl.startsWith("/")) {
            tailAPIUrl = "/" + tailAPIUrl;
        }

        return new URL(hostUrl + tailAPIUrl);
    }
}
