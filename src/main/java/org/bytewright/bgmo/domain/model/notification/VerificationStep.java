package org.bytewright.bgmo.domain.model.notification;

import jakarta.annotation.Nullable;
import lombok.Builder;

/**
 * A single step in the messenger account linking tutorial.
 *
 * @param messageKey i18n key for the step description text
 * @param pictureUrl optional URL to a tutorial screenshot; assumed valid if present
 */
@Builder
public record VerificationStep(String messageKey, @Nullable String pictureUrl) {}
