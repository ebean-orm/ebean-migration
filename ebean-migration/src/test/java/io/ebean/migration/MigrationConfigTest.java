package io.ebean.migration;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.util.Properties;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class MigrationConfigTest {

  @Test
  public void testDefaults() {

    MigrationConfig config = new MigrationConfig();

    assertNull(config.getDbUrl());
    assertNull(config.getDbUsername());
    assertNull(config.getDbPassword());

    assertEquals(config.getMetaTable(), "db_migration");
    assertNull(config.getRunPlaceholders());
    assertEquals(config.getMigrationPath(), "dbmigration");

    assertNotNull(config.getClassLoader());
    assertNull(config.getRunPlaceholderMap());
  }

  @Test
  public void loadProperties_ebean_migration() {

    Properties props = new Properties();
    props.setProperty("ebean.migration.username","username");
    props.setProperty("ebean.migration.password","password");
    props.setProperty("ebean.migration.schema","fooSchema");
    props.setProperty("ebean.migration.createSchemaIfNotExists","false");
    props.setProperty("ebean.migration.driver","driver");
    props.setProperty("ebean.migration.url","url");
    props.setProperty("ebean.migration.metaTable","metaTable");
    props.setProperty("ebean.migration.placeholders","placeholders");
    props.setProperty("ebean.migration.migrationPath","migrationPath");
    props.setProperty("ebean.migration.patchResetChecksumOn", "1.1,1.2");

    assertLoadedProperties(props);
  }

  @Test
  public void loadProperties() {

    Properties props = new Properties();
    props.setProperty("dbmigration.username","username");
    props.setProperty("dbmigration.password","password");
    props.setProperty("dbmigration.schema","fooSchema");
    props.setProperty("dbmigration.createSchemaIfNotExists","false");
    props.setProperty("dbmigration.driver","driver");
    props.setProperty("dbmigration.url","url");
    props.setProperty("dbmigration.metaTable","metaTable");
    props.setProperty("dbmigration.placeholders","placeholders");
    props.setProperty("dbmigration.migrationPath","migrationPath");
    props.setProperty("dbmigration.patchResetChecksumOn", "1.1,1.2");

    assertLoadedProperties(props);
  }

  private void assertLoadedProperties(Properties props) {
    MigrationConfig config = new MigrationConfig();
    config.load(props);

    assertEquals(config.getDbUrl(), "url");
    assertEquals(config.getDbUsername(), "username");
    assertEquals(config.getDbPassword(), "password");
    assertEquals(config.getDbSchema(), "fooSchema");
    assertEquals(config.isCreateSchemaIfNotExists(), false);

    assertEquals(config.getMetaTable(), "metaTable");
    assertEquals(config.getRunPlaceholders(), "placeholders");
    assertEquals(config.getMigrationPath(), "migrationPath");
    assertThat(config.getPatchResetChecksumOn()).contains("1.1", "1.2");
  }

  @Test
  public void loadProperties_withName() {

    Properties props = new Properties();
    props.setProperty("ebean.mydb.migration.username","username");
    props.setProperty("ebean.mydb.migration.migrationPath","migrationPath");
    props.setProperty("ebean.migration.password","password");
    props.setProperty("ebean.migration.schema","fooSchema");
    props.setProperty("dbmigration.url","url");
    props.setProperty("dbmigration.metaTable","metaTable");

    MigrationConfig config = new MigrationConfig();
    config.setName("mydb");
    config.load(props);

    assertEquals(config.getDbUrl(), "url");
    assertEquals(config.getDbUsername(), "username");
    assertEquals(config.getDbPassword(), "password");
    assertEquals(config.getDbSchema(), "fooSchema");

    assertEquals(config.getDbUrl(), "url");
    assertEquals(config.getMetaTable(), "metaTable");
  }

  @Test
  public void createConnection() {

    Properties props = new Properties();
    props.setProperty("dbmigration.username","sa");
    props.setProperty("dbmigration.password","");
    props.setProperty("dbmigration.driver","org.h2.Driver");
    props.setProperty("dbmigration.url","jdbc:h2:mem:createConn");

    MigrationConfig config = new MigrationConfig();
    config.load(props);

    Connection connection = config.createConnection();
    assertNotNull(connection);
  }

  @Test
  public void setResetChecksumVersionsOn() {

    MigrationConfig config = new MigrationConfig();
    config.setPatchResetChecksumOn("1.3,2.1,3.4.5,R__foo_bar");
    config.setPatchInsertOn("2.1,R__foo_bar");

    Set<String> resetVersions = config.getPatchResetChecksumOn();
    assertThat(resetVersions).contains("1.3", "2.1", "3.4.5", "foo_bar");
    assertThat(resetVersions).hasSize(4);

    Set<String> insertVersions = config.getPatchInsertOn();
    assertThat(insertVersions).contains("2.1", "foo_bar");
    assertThat(insertVersions).hasSize(2);
  }
}
