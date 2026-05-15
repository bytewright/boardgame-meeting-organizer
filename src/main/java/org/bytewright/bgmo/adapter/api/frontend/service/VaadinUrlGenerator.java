package org.bytewright.bgmo.adapter.api.frontend.service;

import java.net.URL;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.bytewright.bgmo.domain.model.MeetupEvent;
import org.bytewright.bgmo.domain.model.event.ModelUpdatedEvents;
import org.bytewright.bgmo.domain.service.SiteManagementService;
import org.bytewright.bgmo.domain.service.UrlGenerator;
import org.bytewright.bgmo.domain.service.data.MeetupDao;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class VaadinUrlGenerator implements UrlGenerator {
  private final SiteManagementService siteManagementService;
  private final MeetupDao meetupDao;

  @EventListener
  public void onMeetupCreatedEvent(ModelUpdatedEvents.MeetupCreated event) {
    URL meetup = getUrlFor(meetupDao.findOrThrow(event.id()));
    log.info("New Meeting is available at: {}", meetup);
  }

  @SneakyThrows
  @Override
  public URL getUrlFor(MeetupEvent meetup) {
    return siteManagementService
        .getBaseUrl()
        .resolve("meetup/%s".formatted(meetup.getId()))
        .toURL();
  }
}
