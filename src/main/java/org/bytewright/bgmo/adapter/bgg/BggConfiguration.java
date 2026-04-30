package org.bytewright.bgmo.adapter.bgg;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.DefaultUriBuilderFactory;

@Configuration
public class BggConfiguration {

  @Value("${bgmo.bgg.api.base-url:https://boardgamegeek.com/xmlapi2}")
  private String bggBaseUrl;

  /**
   * Dedicated RestClient for the BoardGameGeek XML API. Named "bggRestClient" so Spring resolves it
   * unambiguously when other RestClient beans exist (e.g. Vaadin internals).
   */
  @Bean
  public RestClient bggRestClient() {
    DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory(bggBaseUrl);
    return RestClient.builder()
        .uriBuilderFactory(factory)
        .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_XML_VALUE)
        .build();
  }
}
