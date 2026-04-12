package org.bytewright.bgmo.adapter.persistance.dao.repository;

import org.bytewright.bgmo.adapter.persistance.entity.meetup.MeetupJoinRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MeetupJoinRequestRepository
    extends JpaRepository<MeetupJoinRequestEntity, MeetupJoinRequestEntity.MeetupJoinKey> {}
