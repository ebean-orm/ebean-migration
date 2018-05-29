package io.ebean.migration.custom;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import io.ebean.migration.custom.parse.JavaCall;

/**
 * Runs a static method for java migration. This is very similar to {@link JavaDbMigration}, but runs on a static method.
 *
 * @author Roland Praml, FOCONIS AG
 */
public class RunStaticMethodHandler implements CustomCommandHandler {

  @Override
  public void handle(Connection conn, String cmdString) throws SQLException {

    JavaCall call = JavaCall.parse(cmdString);

    int pos = call.getName().lastIndexOf('.');
    if (pos == -1) {
      throw new IllegalArgumentException(call.getName() + " no valid identifier");
    }

    String className = call.getName().substring(0, pos);
    String methodName = call.getName().substring(pos + 1);

    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    if (classLoader == null) {
      classLoader = getClass().getClassLoader();
    }
    try {
      Class<?> cls = Class.forName(className, true, classLoader);
      // Method signature is always method(Connection c, List args)
      Method method = cls.getMethod(methodName, Connection.class, List.class);
      method.invoke(null, conn, call.getArguments());
    } catch (ClassNotFoundException | NoSuchMethodException | SecurityException | IllegalAccessException
        | IllegalArgumentException | InvocationTargetException e) {
      throw new IllegalArgumentException("Cannot run " + cmdString, e);
    }

  }
}
