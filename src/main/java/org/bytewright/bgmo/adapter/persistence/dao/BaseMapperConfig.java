package org.bytewright.bgmo.adapter.persistence.dao;

import org.bytewright.bgmo.adapter.persistence.BaseEntityRepository;
import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.MapperConfig;
import org.mapstruct.ReportingPolicy;

@MapperConfig(
    componentModel = "spring",
    injectionStrategy = InjectionStrategy.SETTER,
    unmappedTargetPolicy = ReportingPolicy.WARN,
    unmappedSourcePolicy = ReportingPolicy.WARN,
    uses = {BaseEntityRepository.class},
    collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED)
public interface BaseMapperConfig {}
