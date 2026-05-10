package org.bytewright.bgmo.domain.service.data;

import java.util.Set;
import java.util.UUID;
import org.bytewright.bgmo.domain.model.MeetupEvent;
import org.bytewright.bgmo.domain.model.MeetupEventLocation;

public interface MeetupDao extends ModelDao<MeetupEvent> {
  Set<MeetupEventLocation> findAllLocationsByOrganizer(UUID userId);
}
