package io.ebean.migration.it;

import io.ebean.DB;
import io.ebean.Database;
import io.ebean.Transaction;
import io.ebean.TxScope;
import io.ebean.migration.JdbcMigration;
import io.ebean.migration.MigrationContext;
import io.ebean.migration.db.MigrationContextDb;
import io.ebeaninternal.server.core.DefaultServer;
import io.ebeaninternal.server.transaction.ExternalJdbcTransaction;
import io.ebeaninternal.server.transaction.TransactionManager;
import org.migration.model.M3;

import javax.persistence.PersistenceException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import static org.assertj.core.api.Assertions.assertThat;
/**
 * Sample migration.
 *
 * @author Roland Praml, FOCONIS AG
 */
public class V1_2_1__test implements JdbcMigration {


  @Override
  public void migrate(MigrationContext context) throws SQLException {
    if (context instanceof MigrationContextDb) {
      // some asserts
      Database db = ((MigrationContextDb) context).database(); // do not use DB.getDefault, as it is not yet registered!

      assertThat(((MigrationContextDb) context).transaction())
        .isNotNull()
        .isSameAs(db.currentTransaction());

      M3 m3 = db.find(M3.class).where().idEq(1).findOne();
      m3.setAcol("Migrate db");
      db.save(m3);

    } else {
      try (PreparedStatement ps = context.connection().prepareStatement("update m3 set acol = ? where id = ?")) {
        ps.setString(1, "Migrate raw");
        ps.setInt(2, 1);
        ps.executeUpdate();
      }
    }
  }


  @Override
  public String toString() {
    return "Dummy jdbc migration";
  }
}
