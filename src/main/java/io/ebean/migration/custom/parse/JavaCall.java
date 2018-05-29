/*
 * Licensed Materials - Property of FOCONIS AG
 * (C) Copyright FOCONIS AG.
 */

package io.ebean.migration.custom.parse;

import java.util.List;

/**
 * Produced by the JavaCallParser.
 *
 * it contains the class/bean name and the arguments.
 *
 * @author Roland Praml, FOCONIS AG
 *
 */
public class JavaCall {

  private String name;

  private List<Object> arguments;

  /**
   * Constructor - invoked from the parser.
   */
  JavaCall(String name, List<Object> arguments) throws ParseException {
    this.name = name;
    this.arguments = arguments;
  }

  public String getName() {
    return name;
  }

  public List<Object> getArguments() {
    return arguments;
  }

  public static JavaCall parse(String cmd) {
    try {
      return new JavaCallParser(cmd).parse();
    } catch (ParseException e) {
      throw new IllegalArgumentException("Could not parse " + cmd, e);
    }
  }
}
