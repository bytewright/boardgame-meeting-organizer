package org.bytewright.bgmo.adapter.persistence.dao.repository;

import java.util.UUID;
import org.bytewright.bgmo.adapter.persistence.entity.meetup.MeetupJoinRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MeetupJoinRequestRepository extends JpaRepository<MeetupJoinRequestEntity, UUID> {}
