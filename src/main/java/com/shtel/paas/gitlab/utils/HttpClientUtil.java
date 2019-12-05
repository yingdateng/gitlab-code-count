package com.shtel.paas.gitlab.utils;

import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriTemplate;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * httpClient
 */
public class HttpClientUtil {

    /**
     * 格式化url
     *
     * @param url
     * @param uriVariables
     * @param getParameters
     * @return
     */
    public static String parseUrl(String url, HashMap uriVariables, HashMap<String, String> getParameters) {

        URI expanded = new UriTemplate(url).expand(uriVariables);
        try {
            url = URLDecoder.decode(expanded.toString(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            System.err.println("服务器异常：{}" + e.getMessage());
        }

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
        if (getParameters != null) {
            for (Map.Entry<String, String> entry : getParameters.entrySet()) {
                builder.queryParam(entry.getKey(), entry.getValue());
            }
        }
        String uri = builder.build().toString();

        return uri;
    }

    /**
     * 设置编码
     * @return
     */
    public static RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getMessageConverters().set(1, new StringHttpMessageConverter(StandardCharsets.UTF_8));
        return restTemplate;
    }
}
