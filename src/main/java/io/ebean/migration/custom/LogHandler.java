package io.ebean.migration.custom;

import java.sql.Connection;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple handler to write to the log.
 *
 * @author Roland Praml, FOCONIS AG
 *
 */
public class LogHandler implements CustomCommandHandler {

  private static final Logger logger = LoggerFactory.getLogger(LogHandler.class);

  @Override
  public void handle(Connection conn, String cmdString) throws SQLException {
    logger.info(cmdString);
  }
}
