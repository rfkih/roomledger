package com.roomledger.app.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.client.*;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.logging.Logger;

@Configuration
public class RestClientConfig {
    private static final Logger log = Logger.getLogger(RestClientConfig.class.getName());

    @Bean
    public HttpClient httpClient(@Value("${rest.client.connect-timeout:5s}") Duration timeout) {
        return HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).connectTimeout(timeout).build();
    }

    @Bean
    public ClientHttpRequestInterceptor loggingInterceptor() {
        return (request, body, execution) -> {
            log.info(() -> "Request " + request.getMethod() + " " + request.getURI());
            log.info(() -> "Request Headers: " + request.getHeaders());
            log.info(() -> "Request Body: " + (body.length > 0 ? new String(body, StandardCharsets.UTF_8) : "<empty>"));

            ClientHttpResponse response = execution.execute(request, body);

            String resp = StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8);
            log.info(() -> {
                try {
                    return "Response Status: " + response.getStatusCode().value();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            log.info(() -> "Response Headers: " + response.getHeaders());
            log.info(() -> "Response Body: " + resp);

            return response;
        };
    }

    @Bean
    public RestClient genericRestClient(RestClient.Builder builder,
                                        HttpClient httpClient,
                                        ClientHttpRequestInterceptor loggingInterceptor) {
        ClientHttpRequestFactory base = new JdkClientHttpRequestFactory(httpClient);
        ClientHttpRequestFactory buffering = new BufferingClientHttpRequestFactory(base);

        return builder
                .requestFactory(buffering)
                .requestInterceptors(list -> list.add(loggingInterceptor))
                .build();
    }

    @Bean
    public RestClient xenditRestClient(RestClient.Builder builder,
                                       HttpClient httpClient,
                                       ClientHttpRequestInterceptor loggingInterceptor,
                                       @Value("${xendit.secret}") String secretKey) {
        ClientHttpRequestFactory base = new JdkClientHttpRequestFactory(httpClient);
        ClientHttpRequestFactory buffering = new BufferingClientHttpRequestFactory(base);

        return builder
                .requestFactory(buffering)
                .requestInterceptors(list -> list.add(loggingInterceptor))
                .baseUrl("https://api.xendit.co")
                .defaultHeaders(h -> {
                    h.setBasicAuth(secretKey, "");
                    h.setAccept(List.of(MediaType.APPLICATION_JSON));
                    h.add("api-version", "2024-11-11");
                })
                .build();
    }
}
