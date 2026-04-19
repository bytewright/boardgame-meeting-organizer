package org.bytewright.bgmo.adapter.persistence.migrations;

import java.sql.PreparedStatement;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import liquibase.change.custom.CustomTaskChange;
import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CustomChangeException;
import liquibase.resource.ResourceAccessor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/** Sample liquibase java migration to add first admin to site. */
public class AddAdminTask implements CustomTaskChange {

  @Override
  public void execute(Database database) throws CustomChangeException {

    // 1. Pull the plain text password from the environment
    String rawPassword = System.getenv("APP_ADMIN_PASSWORD");

    if (rawPassword == null || rawPassword.isBlank()) {
      rawPassword = "admin";
    }

    BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    String encodedPw = encoder.encode(rawPassword);
    JdbcConnection connection = (JdbcConnection) database.getConnection();
    try (PreparedStatement ps =
        connection.prepareStatement(
            "INSERT INTO registered_users (id, loginName, displayName, passwordHash, status, role, created_at) "
                + "VALUES (?,?, ?, ?, ?, ?, ?)")) {
      ps.setObject(1, generateV7());
      ps.setString(2, "admin");
      ps.setString(3, "admin");
      ps.setString(4, encodedPw);
      ps.setString(5, "ACTIVE");
      ps.setString(6, "ADMIN");
      ps.setObject(7, OffsetDateTime.ofInstant(Instant.now(), ZoneOffset.UTC));
      ps.executeUpdate();
    } catch (Exception e) {
      throw new CustomChangeException("Could not add admin user", e);
    }
  }

  public static UUID generateV7() {
    long timestamp = System.currentTimeMillis();
    long mostSigBits = (timestamp << 16) | 0x7000L | (new java.util.Random().nextLong() & 0x0FFFL);
    long leastSigBits =
        (new java.util.Random().nextLong() & 0x3FFFFFFFFFFFFFFFL) | 0x8000000000000000L;
    return new UUID(mostSigBits, leastSigBits);
  }

  @Override
  public String getConfirmationMessage() {
    return "Admin user added.";
  }

  @Override
  public void setUp() {}

  @Override
  public void setFileOpener(ResourceAccessor resourceAccessor) {}

  @Override
  public liquibase.exception.ValidationErrors validate(Database database) {
    return null;
  }
}
