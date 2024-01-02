package io.ebean.migration.it;

import io.ebean.Database;
import io.ebean.Transaction;
import io.ebean.TxScope;
import io.ebean.migration.JdbcMigration;
import io.ebean.migration.MigrationContext;
import io.ebeaninternal.api.ScopeTrans;
import io.ebeaninternal.api.ScopedTransaction;
import io.ebeaninternal.api.SpiTransaction;
import io.ebeaninternal.server.core.DefaultServer;
import io.ebeaninternal.server.transaction.ExternalJdbcTransaction;
import io.ebeaninternal.server.transaction.TransactionManager;
import jakarta.persistence.PersistenceException;
import org.migration.model.M3;

import java.sql.Connection;

/**
 * Sample migration.
 *
 * @author Roland Praml, FOCONIS AG
 */
public class V1_2_1__test implements JdbcMigration {


  @Override
  public void migrate(MigrationContext context) {
    Database db = context.database();
    try (Transaction txn = beginExternalTransaction(context.database(), context.connection())) {


      M3 m3 = db.find(M3.class).findOne();
      m3.setAcol("Hello Migration");
      db.save(m3);
    }
  }

  private Transaction beginExternalTransaction(Database database, Connection connection) {
    DefaultServer defaultServer = (DefaultServer) database;
    TransactionManager transactionManager = (io.ebeaninternal.server.transaction.TransactionManager) defaultServer.transactionManager();
    ExternalJdbcTransaction txn = new ExternalJdbcTransaction(true, connection, transactionManager) {
      @Override
      public void end() throws PersistenceException {
        transactionManager.externalRemoveTransaction();
      }
    };
    return transactionManager.externalBeginTransaction(txn, TxScope.required());
  }

  @Override
  public String toString() {
    return "Dummy jdbc migration";
  }
}
