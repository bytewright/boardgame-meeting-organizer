package org.bytewright.bgmo.adapter.persistence.dao.repository;

import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.stream.Stream;
import org.bytewright.bgmo.adapter.persistence.entity.meetup.MeetupEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MeetupRepository extends JpaRepository<MeetupEntity, UUID> {
  Stream<MeetupEntity> findByCreator_Id(UUID id);

  Stream<MeetupEntity> findByEventDateAfterAndCanceledFalseOrderByEventDateAsc(
      ZonedDateTime eventDate);
}
