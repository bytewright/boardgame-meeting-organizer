package org.bytewright.bgmo.adapter.persistence.entity;

import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.*;
import org.bytewright.bgmo.adapter.persistence.converter.StringListConverter;
import org.bytewright.bgmo.adapter.persistence.entity.user.RegisteredUserEntity;
import org.bytewright.bgmo.domain.model.data.HasUUID;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "games")
@Getter
@Setter
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class GameEntity extends AbstractEntity<UUID> implements HasUUID {

  @Id
  @UuidGenerator(style = UuidGenerator.Style.TIME)
  @Column(nullable = false, updatable = false)
  private UUID id;

  @Column(nullable = false, length = 1024)
  private String name;

  @Column(columnDefinition = "TEXT")
  @Nullable
  private String description;

  @Column(nullable = false)
  private int minPlayers;

  @Column(nullable = false)
  private int maxPlayers;

  @Column @Nullable private Integer optimalPlayers;

  @Column(nullable = false)
  private int playTimeMinutesPerPlayer;

  @Column @Nullable private Long bggId;

  @Column
  @Convert(converter = StringListConverter.class)
  @ToString.Exclude
  @Builder.Default
  @Setter(AccessLevel.NONE)
  private List<String> urls = new ArrayList<>();

  @Column @Nullable private Double complexity;

  @Column @Nullable private String artworkLink;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(nullable = false, updatable = false)
  @ToString.Exclude
  private RegisteredUserEntity owner;
}
