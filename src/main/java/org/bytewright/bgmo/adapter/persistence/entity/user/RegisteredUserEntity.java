package org.bytewright.bgmo.adapter.persistence.entity.user;

import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;
import lombok.*;
import org.bytewright.bgmo.adapter.persistence.entity.AbstractEntity;
import org.bytewright.bgmo.adapter.persistence.entity.GameEntity;
import org.bytewright.bgmo.adapter.persistence.entity.GameEntity_;
import org.bytewright.bgmo.domain.model.data.HasUUID;
import org.bytewright.bgmo.domain.model.user.UserRole;
import org.bytewright.bgmo.domain.model.user.UserStatus;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(
    name = "registered_users",
    uniqueConstraints =
        @UniqueConstraint(
            name = "UC_USER_LOGIN_NAME",
            columnNames = {RegisteredUserEntity_.LOGIN_NAME}))
@Getter
@Setter
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class RegisteredUserEntity extends AbstractEntity<UUID> implements HasUUID {
  @Id
  @UuidGenerator(style = UuidGenerator.Style.TIME)
  @Column(nullable = false, updatable = false)
  private UUID id;

  @Column(nullable = false)
  private String loginName;

  @Column(nullable = false, length = 1024)
  private String displayName;

  @Column(nullable = false)
  private String passwordHash;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant tsCreation;

  @LastModifiedDate
  @Column(name = "modified_at")
  private Instant tsModified;

  @Column(name = "last_login")
  private Instant tsLastLogin;

  @Column(columnDefinition = "text")
  private String registrationIntroText;

  @Column private Locale preferredLocale;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  @Builder.Default
  private UserStatus status = UserStatus.PENDING_APPROVAL;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  @Builder.Default
  private UserRole role = UserRole.USER;

  @Nullable @Column private UUID primaryContactId;

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = GameEntity_.OWNER)
  @ToString.Exclude
  @Builder.Default
  @Setter(AccessLevel.NONE)
  private Set<GameEntity> ownedGames = new HashSet<>();

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = ContactInfoEntity_.USER)
  @ToString.Exclude
  @Builder.Default
  @Setter(AccessLevel.NONE)
  private Set<ContactInfoEntity> contactInfos = new HashSet<>();
}
