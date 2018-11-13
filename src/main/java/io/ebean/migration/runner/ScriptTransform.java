package io.ebean.migration.runner;

import java.util.HashMap;
import java.util.Map;

/**
 * Transforms a SQL script given a map of key/value substitutions.
 */
public class ScriptTransform {

  /**
   * Transform just ${table} with the table name.
   */
  public static String replace(String key, String value, String script) {
    return script.replace(key, value);
  }

  private final Map<String,String> placeholders = new HashMap<>();

  ScriptTransform(Map<String,String> map) {
    for (Map.Entry<String, String> entry : map.entrySet()) {
      placeholders.put(wrapKey(entry.getKey()), entry.getValue());
    }
  }

  /**
   * Build and return a ScriptTransform that replaces placeholder values in DDL scripts.
   */
  public static ScriptTransform build(String runPlaceholders, Map<String, String> runPlaceholderMap) {
    Map<String, String> map = PlaceholderBuilder.build(runPlaceholders, runPlaceholderMap);
    return new ScriptTransform(map);
  }

  private String wrapKey(String key) {
    return "${"+key+"}";
  }

  /**
   * Transform the script replacing placeholders in the form <code>${key}</code> with <code>value</code>.
   */
  public String transform(String source) {

    for (Map.Entry<String, String> entry : placeholders.entrySet()) {
      source = source.replace(entry.getKey(), entry.getValue());
    }
    return source;
  }
}
