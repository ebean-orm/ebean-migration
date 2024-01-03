package io.ebean.migration.it;

import io.ebean.DB;
import io.ebean.Database;
import io.ebean.DatabaseFactory;
import io.ebean.config.DatabaseConfig;
import io.ebean.migration.MigrationConfig;
import io.ebean.migration.MigrationRunner;
import io.ebean.migration.db.MigrationRunnerDb;
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
    try {
      config.setName(db.name());
      config.load(db.pluginApi().config().getProperties());
      new MigrationRunnerDb(config).run(db);


      M3 m3 = DB.find(M3.class).where().idEq(1).findOne();
      assertThat(m3.getAcol()).isEqualTo("Migrate db PreCommit");
    } finally {
      db.shutdown(true, false);
    }
  }

  @Test
  public void testWithRunner() {

    // NOTE: This can be done by AutoMigration (or MigrationPlugin) later
    DatabaseConfig dbCfg = new DatabaseConfig();
    dbCfg.setName("h2");
    dbCfg.loadFromProperties();
    dbCfg.setRunMigration(true);
    Database db = DatabaseFactory.create(dbCfg);
    try {

      M3 m3 = DB.find(M3.class).where().idEq(1).findOne();
      assertThat(m3.getAcol()).isEqualTo("Migrate raw");
    } finally {
      db.shutdown(true, false);
    }

  }

  @Test
  public void testNoMigration() {
    Database db = DB.getDefault();
    try {

      M3 m3 = DB.find(M3.class).where().idEq(1).findOne();
      assertThat(m3.getAcol()).isEqualTo("Migrate db PreCommit");
    } finally {
      db.shutdown(true, false);
    }
  }
  @Test
  public void testWithPlugin() {
    DatabaseConfig dbCfg = new DatabaseConfig();
    dbCfg.setName("h2");
    dbCfg.loadFromProperties();
    dbCfg.getProperties().setProperty("ebean.h2.migration.autoRun", "true");
    dbCfg.setDefaultServer(true);
    Database db = DatabaseFactory.create(dbCfg);
    try {

      M3 m3 = DB.find(M3.class).where().idEq(1).findOne();
      assertThat(m3.getAcol()).isEqualTo("Migrate db PreCommit");
    } finally {
      db.shutdown(true, false);
    }
  }
}
