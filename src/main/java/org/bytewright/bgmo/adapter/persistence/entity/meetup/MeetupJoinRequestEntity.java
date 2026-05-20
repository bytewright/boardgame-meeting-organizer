package org.bytewright.bgmo.adapter.persistence.entity.meetup;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;
import org.bytewright.bgmo.adapter.persistence.entity.AbstractEntity;
import org.bytewright.bgmo.adapter.persistence.entity.user.RegisteredUserEntity;
import org.bytewright.bgmo.domain.model.RequestState;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(
    name = "meetup_joins",
    uniqueConstraints =
        @UniqueConstraint(
            name = "UC_MEETUP_JOIN_USER",
            columnNames = {"meetup_id", "user_id"}))
@Getter
@Setter
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class MeetupJoinRequestEntity extends AbstractEntity<UUID> {

  @Id
  @UuidGenerator(style = UuidGenerator.Style.TIME)
  @Column(nullable = false, updatable = false)
  private UUID id;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant tsCreation;

  @Column(columnDefinition = "text")
  private String comment;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  @Builder.Default
  private RequestState requestState = RequestState.OPEN;

  @Column(nullable = false, columnDefinition = "jsonb")
  @JdbcTypeCode(SqlTypes.JSON)
  private String payload;

  @ManyToOne(optional = false)
  @JoinColumn(nullable = false)
  private MeetupEntity meetup;

  @ManyToOne @JoinColumn private RegisteredUserEntity user;
}
