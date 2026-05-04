package org.bytewright.bgmo.adapter.bgg;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.DefaultUriBuilderFactory;

@Configuration
public class BggConfiguration {

  /**
   * Dedicated RestClient for the BoardGameGeek XML API. Named "bggRestClient" so Spring resolves it
   * unambiguously when other RestClient beans exist (e.g. Vaadin internals).
   */
  @Bean
  public RestClient bggRestClient(BggAdapterProperties bggAdapterProperties) {
    DefaultUriBuilderFactory factory =
        new DefaultUriBuilderFactory(bggAdapterProperties.getBggBaseUrl());
    return RestClient.builder()
        .uriBuilderFactory(factory)
        .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_XML_VALUE)
        .build();
  }
}
