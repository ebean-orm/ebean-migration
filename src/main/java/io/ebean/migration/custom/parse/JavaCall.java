package io.ebean.migration.custom.parse;

import java.util.List;

/**
 * Produced by the JavaCallParser.
 *
 * it contains the class/bean name and the arguments.
 *
 * Note:<br/>
 * Numeric arguments are instances of Integer or Double<br/>
 * Date/Time/DateTime arguments are instances of LocalDate/LocalTime/OffsetDateTime<br/>
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
