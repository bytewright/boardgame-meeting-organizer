package org.bytewright.bgmo.adapter.persistance.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import jakarta.transaction.Transactional;
import java.util.*;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.SneakyThrows;
import org.bytewright.bgmo.adapter.persistance.entity.AbstractEntity;
import org.bytewright.bgmo.domain.model.data.HasUUID;
import org.bytewright.bgmo.domain.service.data.ModelDao;
import org.mapstruct.MappingTarget;
import org.mapstruct.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;

@Getter
@Transactional
public abstract class BaseEntityMapper<
        DTO_TYPE extends HasUUID, ENTITY_TYPE extends AbstractEntity<UUID>>
    implements ModelDao<DTO_TYPE> {

  protected EntityManager entityManager;

  @Autowired
  public void setEntityManager(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  protected abstract Class<ENTITY_TYPE> getEntityClass();

  @ObjectFactory
  @SneakyThrows
  public ENTITY_TYPE newEntity() {
    return getEntityClass().getDeclaredConstructor().newInstance();
  }

  public abstract void updateEntity(@MappingTarget ENTITY_TYPE currentEntity, DTO_TYPE model);

  @Override
  public Collection<DTO_TYPE> findAll() {
    CriteriaBuilder cb = entityManager.getCriteriaBuilder();
    CriteriaQuery<ENTITY_TYPE> cq = cb.createQuery(getEntityClass());
    Root<ENTITY_TYPE> root = cq.from(getEntityClass());

    // Select all entities
    cq.select(root);
    // Allow implementing DAOs to add query params
    addFindAllQueryRequirements(cb, cq, root);
    cq.orderBy(cb.asc(root.get("id")));
    // Execute the query
    TypedQuery<ENTITY_TYPE> query = entityManager.createQuery(cq);
    List<ENTITY_TYPE> entities = query.getResultList();

    // Convert the list of entities to DTOs
    return entities.stream().map(this::toDto).collect(Collectors.toSet());
  }

  protected void addFindAllQueryRequirements(
      CriteriaBuilder cb, CriteriaQuery<ENTITY_TYPE> cq, Root<ENTITY_TYPE> root) {}

  public abstract DTO_TYPE toDto(ENTITY_TYPE entity);

  private ENTITY_TYPE fromId(UUID uuid) {
    return entityManager.find(getEntityClass(), uuid);
  }

  public Optional<DTO_TYPE> findDtoById(UUID uuid) {
    ENTITY_TYPE entity = fromId(uuid);
    return Optional.ofNullable(toDto(entity));
  }

  public List<ENTITY_TYPE> toEntityList(List<DTO_TYPE> dtos) {
    return Optional.ofNullable(dtos).stream()
        .flatMap(Collection::stream)
        .map(this::updateOrCreateEntityNoPersist)
        .toList();
  }

  public List<DTO_TYPE> toDtoList(List<ENTITY_TYPE> entities) {
    return Optional.ofNullable(entities).stream()
        .flatMap(Collection::stream)
        .map(this::toDto)
        .toList();
  }

  public Set<ENTITY_TYPE> toEntitySet(Set<DTO_TYPE> dtos) {
    return Optional.ofNullable(dtos).stream()
        .flatMap(Collection::stream)
        .map(this::updateOrCreateEntityNoPersist)
        .collect(Collectors.toCollection(HashSet::new));
  }

  public Set<DTO_TYPE> toDtoSet(Set<ENTITY_TYPE> entities) {
    return Optional.ofNullable(entities).stream()
        .flatMap(Collection::stream)
        .map(this::toDto)
        .collect(Collectors.toCollection(HashSet::new));
  }

  public Set<UUID> toIdSet(Set<ENTITY_TYPE> entities) {
    return Optional.ofNullable(entities).stream()
        .flatMap(Collection::stream)
        .map(AbstractEntity::getId)
        .collect(Collectors.toCollection(HashSet::new));
  }

  public List<UUID> toIdList(List<ENTITY_TYPE> entities) {
    return Optional.ofNullable(entities).stream()
        .flatMap(Collection::stream)
        .map(AbstractEntity::getId)
        .toList();
  }

  public List<ENTITY_TYPE> fromIdList(List<UUID> ids) {
    return Optional.ofNullable(ids).stream().flatMap(Collection::stream).map(this::fromId).toList();
  }

  public Set<ENTITY_TYPE> fromIdSet(Set<UUID> ids) {
    return Optional.ofNullable(ids).stream()
        .flatMap(Collection::stream)
        .map(this::fromId)
        .collect(Collectors.toCollection(HashSet::new));
  }

  @Override
  public Optional<DTO_TYPE> find(UUID uuid) {
    return this.findDtoById(uuid);
  }

  @Override
  public boolean exists(UUID modelId) {
    return this.find(modelId).isPresent();
  }

  protected ENTITY_TYPE updateOrCreateEntityNoPersist(DTO_TYPE model) {
    ENTITY_TYPE entity;
    if (model.getId() != null) {
      entity = entityManager.find(getEntityClass(), model.getId());
      if (entity == null) {
        throw new IllegalArgumentException(
            String.format(
                "Model '%s' contained an id for entity class '%s' but no entity with id '%s' could be found!",
                model.getClass().getName(), getEntityClass().getName(), model.getId()));
      }
    } else {
      entity = newEntity();
    }
    updateEntity(entity, model);
    return entity;
  }

  @Override
  public DTO_TYPE createOrUpdate(DTO_TYPE model) {
    ENTITY_TYPE entityType = updateOrCreateEntityNoPersist(model);
    entityManager.persist(entityType);
    return toDto(entityType);
  }

  @Override
  public void delete(UUID uuid) {
    ENTITY_TYPE entity = entityManager.find(getEntityClass(), uuid);
    entityManager.remove(entity);
  }
}
