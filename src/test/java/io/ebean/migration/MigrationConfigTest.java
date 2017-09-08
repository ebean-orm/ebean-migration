package io.ebean.migration;

import org.testng.annotations.Test;

import java.sql.Connection;
import java.util.Properties;

import static org.testng.Assert.*;

public class MigrationConfigTest {

  @Test
  public void testDefaults() {

    MigrationConfig config = new MigrationConfig();

    assertNull(config.getDbDriver());
    assertNull(config.getDbUrl());
    assertNull(config.getDbUsername());
    assertNull(config.getDbPassword());

    assertEquals(config.getApplySuffix(), ".sql");
    assertEquals(config.getMetaTable(), "db_migration");
    assertNull(config.getRunPlaceholders());
    assertEquals(config.getMigrationPath(), "dbmigration");

    assertNotNull(config.getClassLoader());
    assertNull(config.getRunPlaceholderMap());
  }

  @Test
  public void loadProperties() {

    Properties props = new Properties();
    props.setProperty("dbmigration.username","username");
    props.setProperty("dbmigration.password","password");
    props.setProperty("dbmigration.driver","driver");
    props.setProperty("dbmigration.url","url");
    props.setProperty("dbmigration.metaTable","metaTable");
    props.setProperty("dbmigration.applySuffix","applySuffix");
    props.setProperty("dbmigration.placeholders","placeholders");
    props.setProperty("dbmigration.migrationPath","migrationPath");

    MigrationConfig config = new MigrationConfig();
    config.load(props);

    assertEquals(config.getDbDriver(), "driver");
    assertEquals(config.getDbUrl(), "url");
    assertEquals(config.getDbUsername(), "username");
    assertEquals(config.getDbPassword(), "password");

    assertEquals(config.getApplySuffix(), "applySuffix");
    assertEquals(config.getMetaTable(), "metaTable");
    assertEquals(config.getRunPlaceholders(), "placeholders");
    assertEquals(config.getMigrationPath(), "migrationPath");

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
}