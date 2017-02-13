package io.ebean.dbmigration.runner;

import java.util.HashMap;
import java.util.Map;

/**
 * Transforms a SQL script given a map of key/value substitutions.
 */
class ScriptTransform {

  /**
   * Transform just ${table} with the table name.
   */
  public static String table(String catalog, String schema, String tableName, String script) {
    script = script.replace("${table}", tableName);
    if (schema != null && !schema.isEmpty()) {
      // handle catalog and schema prefix for sqlserver
      String tmp = schema + ".";

      if (catalog != null && !catalog.isEmpty()) {
        tmp = catalog + "." + tmp;
      }
      return script.replace("${catalog_schema}", tmp);
    }
    return script.replace("${catalog_schema}", "");
  }

  private final Map<String,String> placeholders = new HashMap<>();

  ScriptTransform(Map<String,String> map) {
    for (Map.Entry<String, String> entry : map.entrySet()) {
      placeholders.put(wrapKey(entry.getKey()), entry.getValue());
    }
  }

  private String wrapKey(String key) {
    return "${"+key+"}";
  }

  /**
   * Transform the script replacing placeholders in the form <code>${key}</code> with <code>value</code>.
   */
  String transform(String source) {

    for (Map.Entry<String, String> entry : placeholders.entrySet()) {
      source = source.replace(entry.getKey(), entry.getValue());
    }
    return source;
  }
}
