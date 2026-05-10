package org.bytewright.bgmo.domain.model;

import org.bytewright.bgmo.domain.service.AdapterSettingsProvider;

public record AdapterInfoAndSettings(
    AdapterSettingsProvider.AdapterInfo adapterInfo, AdapterSettings adapterSettings) {}
