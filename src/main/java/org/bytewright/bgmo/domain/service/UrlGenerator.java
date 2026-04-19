package org.bytewright.bgmo.domain.service;

import java.net.URL;
import org.bytewright.bgmo.domain.model.MeetupEvent;

public interface UrlGenerator {
  URL getUrlFor(MeetupEvent meetup);
}
