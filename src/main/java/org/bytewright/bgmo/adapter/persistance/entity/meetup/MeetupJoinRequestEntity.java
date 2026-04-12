package org.bytewright.bgmo.adapter.persistance.entity.meetup;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;
import lombok.*;
import org.bytewright.bgmo.adapter.persistance.entity.user.RegisteredUserEntity;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "meetup_joins")
@Getter
@Setter
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class MeetupJoinRequestEntity {
  @EmbeddedId private MeetupJoinKey meetupJoinKey;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant tsCreation;

  @ManyToOne
  @MapsId("meetupId")
  @JoinColumn(name = "meetup_id")
  private MeetupEntity meetup;

  @ManyToOne
  @MapsId("userId")
  @JoinColumn(name = "user_id")
  private RegisteredUserEntity user;

  @Getter
  @Setter
  @Builder
  @ToString
  @Embeddable
  @NoArgsConstructor
  @AllArgsConstructor
  public static class MeetupJoinKey implements Serializable {

    @Column(nullable = false, updatable = false)
    private UUID meetupId;

    @Column(nullable = false, updatable = false)
    private UUID userId;
  }
}
