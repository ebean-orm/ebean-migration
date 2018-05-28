package io.ebean.migration;

import java.sql.Connection;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import io.ebean.migration.ddl.CustomStatementHandler;

/**
 * Example javascript handler
 *
 * @author Roland Praml, FOCONIS AG
 *
 */
public class JavaScriptHandler implements CustomStatementHandler {
  private ScriptEngine engine;

  /**
   * Constructor.
   *
   */
  public JavaScriptHandler() {
    ScriptEngineManager manager = new ScriptEngineManager();
     engine = manager.getEngineByName("javascript");
  }

  @Override
  public boolean handle(String stmt, Connection c) {
    if (!stmt.startsWith("javascript:")) {
      return false;
    }
    stmt = stmt.substring(11);

    final Bindings bindings = engine.createBindings();
    bindings.put("connection", c);
    try {
      engine.eval(stmt, bindings);
    } catch (ScriptException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }

    return true;
  }

}
