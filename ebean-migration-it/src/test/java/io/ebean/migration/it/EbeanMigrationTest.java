package io.ebean.migration.it;

import io.ebean.DB;
import io.ebean.Database;
import io.ebean.DatabaseFactory;
import io.ebean.config.DatabaseConfig;
import io.ebean.migration.MigrationConfig;
import io.ebean.migration.MigrationRunner;
import org.migration.model.M3;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Roland Praml, FOCONIS AG
 */
public class EbeanMigrationTest {

  @Test
  public void testEbeanServerJdbcMig() {

    // NOTE: This can be done by AutoMigration (or MigrationPlugin) later
    MigrationConfig config = new MigrationConfig();
    Database db = DB.getDefault();
    config.setName(db.name());
    config.load(db.pluginApi().config().getProperties());
    //new MigrationRunner(config).run(db);


    M3 m3 = DB.find(M3.class).findOne();
    assertThat(m3.getId()).isEqualTo(1);
    assertThat(m3.getAcol()).isEqualTo("Hello Migration");

  }

  @Test
  public void testWithRunner() {

    // NOTE: This can be done by AutoMigration (or MigrationPlugin) later
    DatabaseConfig dbCfg = new DatabaseConfig();
    dbCfg.setName("h2");
    dbCfg.loadFromProperties();
    dbCfg.setRunMigration(true);
    DatabaseFactory.create(dbCfg);

    M3 m3 = DB.find(M3.class).findOne();
    assertThat(m3.getId()).isEqualTo(1);
    assertThat(m3.getAcol()).isEqualTo("text with ; sign");

  }
}
