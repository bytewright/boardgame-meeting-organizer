package org.bytewright.bgmo.adapter.persistance.entity.user;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.*;
import org.bytewright.bgmo.adapter.persistance.entity.AbstractEntity;
import org.bytewright.bgmo.domain.model.user.ContactInfoType;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "user_contact_infos")
@Getter
@Setter
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class ContactInfoEntity extends AbstractEntity<UUID> {
  @Id
  @UuidGenerator(style = UuidGenerator.Style.TIME)
  @Column(nullable = false, updatable = false)
  private UUID id;

  @Enumerated(EnumType.STRING)
  private ContactInfoType type;

  // todo jsonb?
  @Column(nullable = false, columnDefinition = "text")
  private String jsonData;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(nullable = false, updatable = false)
  @ToString.Exclude
  private RegisteredUserEntity user;
}
