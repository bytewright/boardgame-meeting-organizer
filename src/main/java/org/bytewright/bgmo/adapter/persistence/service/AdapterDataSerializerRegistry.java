package org.bytewright.bgmo.adapter.persistence.service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.bytewright.bgmo.domain.model.data.AdapterData;
import org.bytewright.bgmo.domain.service.data.AdapterDataSerializer;
import org.bytewright.bgmo.domain.service.data.AdapterDataSerializer.AdapterDataRecord;
import org.springframework.stereotype.Component;

@Component
public class AdapterDataSerializerRegistry {

  private final Map<String, AdapterDataSerializer<? extends AdapterData>> byAdapterName;

  public AdapterDataSerializerRegistry(List<AdapterDataSerializer<?>> serializers) {
    this.byAdapterName =
        serializers.stream()
            .collect(Collectors.toMap(AdapterDataSerializer::getAdapterName, Function.identity()));
  }

  @SuppressWarnings("unchecked")
  public <TYPE extends AdapterData> TYPE toConcreteType(AdapterDataRecord record) {
    var adapterDataSerializer = resolve(record.adapterName());
    return (TYPE) adapterDataSerializer.toConcreteType(record);
  }

  @SuppressWarnings("unchecked")
  public <TYPE extends AdapterData> AdapterDataRecord fromConcreteType(TYPE model) {
    var serializer = (AdapterDataSerializer<TYPE>) resolve(model.getAdapterName());
    return serializer.fromConcreteType(model);
  }

  private AdapterDataSerializer<? extends AdapterData> resolve(String adapterName) {
    AdapterDataSerializer<? extends AdapterData> serializer = byAdapterName.get(adapterName);
    if (serializer == null) {
      throw new IllegalStateException(
          "No AdapterDataSerializer registered for adapter: " + adapterName);
    }
    return serializer;
  }
}
