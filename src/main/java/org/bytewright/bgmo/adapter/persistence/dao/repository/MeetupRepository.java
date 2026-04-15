package org.bytewright.bgmo.adapter.persistence.dao.repository;

import java.util.UUID;
import org.bytewright.bgmo.adapter.persistence.entity.meetup.MeetupEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MeetupRepository extends JpaRepository<MeetupEntity, UUID> {}
