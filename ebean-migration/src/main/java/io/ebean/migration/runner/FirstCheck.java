package io.ebean.migration.runner;

import io.ebean.migration.MigrationConfig;
import io.ebean.migration.MigrationContext;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * First initial check to see if migrations exist and exactly match.
 */
final class FirstCheck {

  final MigrationConfig config;
  final MigrationPlatform platform;
  final MigrationContext context;
  final String schema;
  final String table;
  final String sqlTable;
  boolean tableKnownToExist;
  private int count;

  FirstCheck(MigrationConfig config, MigrationContext context, MigrationPlatform platform) {
    this.config = config;
    this.platform = platform;
    this.context = context;
    this.schema = config.getDbSchema();
    this.table = config.getMetaTable();
    this.sqlTable = schema != null ? schema + '.' + table : table;
  }

  MigrationTable initTable(boolean checkStateOnly) {
    return new MigrationTable(this, checkStateOnly);
  }

  boolean fastModeCheck(List<LocalMigrationResource> versions) {
    try {
      final List<MigrationMetaRow> rows = fastRead();
      tableKnownToExist = !rows.isEmpty();
      if (rows.size() != versions.size() + 1) {
        // difference in count of migrations
        return false;
      }
      final Map<String, Integer> dbChecksums = dbChecksumMap(rows);
      for (LocalMigrationResource local : versions) {
        Integer dbChecksum = dbChecksums.get(local.key());
        if (dbChecksum == null) {
          // no match, unexpected missing migration
          return false;
        }
        int localChecksum = checksumFor(local);
        if (localChecksum != dbChecksum) {
          // no match, perhaps repeatable migration change
          return false;
        }
      }
      // successful fast check
      count = versions.size();
      return true;
    } catch (SQLException e) {
      // probably migration table does not exist
      return false;
    }
  }

  private static Map<String, Integer> dbChecksumMap(List<MigrationMetaRow> rows) {
    return rows.stream().collect(Collectors.toMap(MigrationMetaRow::version, MigrationMetaRow::checksum));
  }

  private int checksumFor(LocalMigrationResource local) {
    if (local instanceof LocalUriMigrationResource) {
      return ((LocalUriMigrationResource) local).checksum();
    } else if (local instanceof LocalDdlMigrationResource) {
      return Checksum.calculate(local.content());
    } else {
      return ((LocalJdbcMigrationResource) local).checksum();
    }
  }

  List<MigrationMetaRow> fastRead() throws SQLException {
    return platform.fastReadMigrations(sqlTable, context.connection());
  }

  int count() {
    return count;
  }
}
