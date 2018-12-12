package io.ebean.migration;

import org.testng.annotations.Test;

import java.sql.Connection;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

public class DbNameUtilTest {

  /**
   * Run manually against Postgres database.
   */
  @Test(enabled = false)
  public void normalise_when_realPostgres() throws SQLException {

    MigrationConfig config = createConfig();
    // assumes DB unit exists on our local postgres database
    config.setDbUrl("jdbc:postgresql://127.0.0.1:5432/unit");

    try (Connection connection = config.createConnection()) {
      String platformName = DbNameUtil.normalise(connection);

      assertThat(platformName).isEqualTo("postgres");
    } catch (SQLException e) {
      throw e;
    }
  }

  /**
   * Run manually against cockroach database.
   */
  @Test(enabled = false)
  public void normalise_when_cockroach() throws SQLException {

    MigrationConfig config = createConfig();
    // assumes DB unit exists on our local cockroach database
    config.setDbUrl("jdbc:postgresql://127.0.0.1:26257/unit");

    try (Connection connection = config.createConnection()) {
      String platformName = DbNameUtil.normalise(connection);
      assertThat(platformName).isEqualTo("cockroach");
    } catch (SQLException e) {
      throw e;
    }
  }

  /**
   * Assumes a DB user unit exists on the databases.
   */
  private MigrationConfig createConfig() {
    MigrationConfig config = new MigrationConfig();
    config.setDbUsername("unit");
    config.setDbPassword("unit");
    config.setDbDriver("org.postgresql.Driver");
    return config;
  }
}
