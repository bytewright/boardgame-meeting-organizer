package org.bytewright.bgmo.adapter.notification.email_smpt.model;

/** The fully rendered subject and body for one outgoing email. */
public record RenderedEmail(String subject, String body) {}
