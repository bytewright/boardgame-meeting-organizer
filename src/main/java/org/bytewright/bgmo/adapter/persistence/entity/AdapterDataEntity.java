package org.bytewright.bgmo.adapter.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;
import org.bytewright.bgmo.domain.model.data.HasUUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(
    name = "adapter_datas",
    uniqueConstraints =
        @UniqueConstraint(
            name = "UC_ADAPTER_DATA_IDENTIFIER",
            columnNames = {"adapter_name", "data_identifier"}))
@Getter
@Setter
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class AdapterDataEntity extends AbstractEntity<UUID> implements HasUUID {

  @Id
  @UuidGenerator(style = UuidGenerator.Style.TIME)
  @Column(nullable = false, updatable = false)
  private UUID id;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant tsCreation;

  @LastModifiedDate
  @Column(name = "modified_at")
  private Instant tsModified;

  @Column(name = "adapter_name", nullable = false, length = 1024)
  private String adapterName;

  @Column(name = "data_identifier", nullable = false, length = 2048)
  private String dataIdentifier;

  @Column(columnDefinition = "jsonb")
  @JdbcTypeCode(SqlTypes.JSON)
  private String data;
}
