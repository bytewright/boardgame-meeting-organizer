package org.bytewright.bgmo.domain.service.data;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.bytewright.bgmo.domain.model.MeetupEvent;
import org.bytewright.bgmo.domain.model.MeetupEventLocation;

public interface MeetupDao extends ModelDao<MeetupEvent> {
  Set<MeetupEventLocation> findAllLocationsByOrganizer(UUID userId);

  Stream<MeetupEvent> findNotExpired(ZonedDateTime now);

  List<MeetupEvent> findAllByOrganizer(UUID currentUserId);
}
