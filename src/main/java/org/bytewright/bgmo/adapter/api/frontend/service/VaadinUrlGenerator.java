package org.bytewright.bgmo.adapter.api.frontend.service;

import java.net.URL;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.bytewright.bgmo.domain.model.MeetupEvent;
import org.bytewright.bgmo.domain.service.BgmoProperties;
import org.bytewright.bgmo.domain.service.UrlGenerator;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class VaadinUrlGenerator implements UrlGenerator {
  private final BgmoProperties bgmoProperties;

  @SneakyThrows
  @Override
  public URL getUrlFor(MeetupEvent meetup) {
    return bgmoProperties.getBaseUrl().resolve("meetup/%s".formatted(meetup.getId())).toURL();
  }
}
