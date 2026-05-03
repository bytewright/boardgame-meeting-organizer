package org.bytewright.bgmo.adapter.persistence.entity.meetup;

import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.*;
import lombok.*;
import lombok.Builder;
import org.bytewright.bgmo.adapter.persistence.entity.AbstractEntity;
import org.bytewright.bgmo.adapter.persistence.entity.GameEntity;
import org.bytewright.bgmo.adapter.persistence.entity.user.RegisteredUserEntity;
import org.bytewright.bgmo.domain.model.data.HasUUID;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "meetups")
@Getter
@Setter
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class MeetupEntity extends AbstractEntity<UUID> implements HasUUID {
  @Id
  @UuidGenerator(style = UuidGenerator.Style.TIME)
  @Column(nullable = false, updatable = false)
  private UUID id;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant tsCreation;

  @LastModifiedDate
  @Column(name = "modified_at")
  @Nullable
  private Instant tsModified;

  @Column(nullable = false, length = 2048)
  private String title;

  @Column(columnDefinition = "text")
  @Nullable
  private String description;

  @Column(nullable = false)
  private ZonedDateTime eventDate;

  @Column(nullable = false)
  private ZonedDateTime registrationClosing;

  @Column(nullable = false)
  private int durationHours;

  @Column(nullable = false)
  private Integer joinSlots;

  @Column(nullable = false)
  private boolean unlimitedSlots;

  @Column(nullable = false)
  private boolean allowAnonSignup;

  @Column(nullable = false)
  private boolean canceled;

  @ManyToOne(optional = false)
  @JoinColumn(nullable = false)
  private RegisteredUserEntity creator;

  @OneToMany(
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      mappedBy = MeetupJoinRequestEntity_.MEETUP)
  @ToString.Exclude
  @Builder.Default
  @Setter(AccessLevel.NONE)
  // hibernate 7 is validating this at compile time, apparently before MetaModel is ready.
  // That's why @OrderBy is using plain text
  @OrderBy("tsCreation ASC")
  private List<MeetupJoinRequestEntity> joinRequests = new ArrayList<>();

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
      name = "meetup_offered_games",
      joinColumns = @JoinColumn(name = "game_id"),
      inverseJoinColumns = @JoinColumn(name = "meeting_id"))
  @ToString.Exclude
  @Builder.Default
  @Setter(AccessLevel.NONE)
  private List<GameEntity> offeredGames = new ArrayList<>();
}
