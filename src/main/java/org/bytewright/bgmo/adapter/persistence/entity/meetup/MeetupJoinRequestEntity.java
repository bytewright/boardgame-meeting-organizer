package org.bytewright.bgmo.adapter.persistence.entity.meetup;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;
import org.bytewright.bgmo.adapter.persistence.entity.AbstractEntity;
import org.bytewright.bgmo.adapter.persistence.entity.user.RegisteredUserEntity;
import org.bytewright.bgmo.domain.model.RequestState;
import org.hibernate.annotations.UuidGenerator;
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
public class MeetupJoinRequestEntity extends AbstractEntity<UUID> {

  @Id
  @UuidGenerator(style = UuidGenerator.Style.TIME)
  @Column(nullable = false, updatable = false)
  private UUID id;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant tsCreation;

  @Column(nullable = false)
  private String displayName;

  @Column private UUID anonToken;
  @Column private String contactInfo;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  @Builder.Default
  private RequestState requestState = RequestState.OPEN;

  @ManyToOne(optional = false, cascade = CascadeType.ALL)
  @JoinColumn(nullable = false)
  private MeetupEntity meetup;

  @ManyToOne @JoinColumn private RegisteredUserEntity user;
}
