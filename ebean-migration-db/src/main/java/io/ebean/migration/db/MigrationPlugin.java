package io.ebean.migration.db;

import io.ebean.migration.MigrationConfig;
import io.ebean.plugin.Plugin;
import io.ebean.plugin.SpiServer;

/**
 * @author Roland Praml, FOCONIS AG
 */
public class MigrationPlugin implements Plugin {
  private MigrationConfig config = new MigrationConfig();
  private SpiServer server;

  @Override
  public void configure(SpiServer server) {
    config.setName(server.name());
    config.load(server.config().getProperties());
    this.server = server;
    if (server.config().isRunMigration() && config.isPluginRun()) {
      throw new UnsupportedOperationException("You cannot enable both 'migration.run'  and 'migration.plugin.run'");
    }
  }

  @Override
  public void online(boolean online) {
    if (online && config.isPluginRun()) {
      new MigrationRunnerDb(config, server).run();
    }
  }

  @Override
  public void shutdown() {

  }
}
