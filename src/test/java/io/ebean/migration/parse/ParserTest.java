/*
 * Licensed Materials - Property of FOCONIS AG
 * (C) Copyright FOCONIS AG.
 */

package io.ebean.migration.parse;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.testng.annotations.Test;

import io.ebean.migration.custom.parse.JavaCall;
import io.ebean.migration.custom.parse.ParseException;

/**
 * TODO.
 *
 * @author Roland Praml, FOCONIS AG
 *
 */
public class ParserTest {

  @Test
  public void testParserIntArgs() throws ParseException {
    JavaCall cmd = JavaCall.parse("io.ebean.Util.func(1,2,3)");
    assertThat(cmd.getName()).isEqualTo("io.ebean.Util.func");
    assertThat(cmd.getArguments()).containsExactly(1, 2, 3);
  }

  @Test
  public void testParserNoArgs() throws ParseException {
    JavaCall cmd = JavaCall.parse("io.ebean.Util.func()");

    assertThat(cmd.getArguments()).isEmpty();

    cmd = JavaCall.parse("io.ebean.Util.func");
    assertThat(cmd.getName()).isEqualTo("io.ebean.Util.func");
    assertThat(cmd.getArguments()).isEmpty();
  }

  @Test
  public void testParserVaroiusArgs() throws ParseException {
    JavaCall cmd = JavaCall.parse("io.ebean.Util.func ( 1, null,  \"Hello \\n\\u0057orld\" )");
    assertThat(cmd.getArguments()).containsExactly(1, null, "Hello \nWorld");

    cmd = JavaCall.parse("io.ebean.Util.func ( 17:45:21, 2017-01-01T12:34:00+02:00,  2003-01-01 )");
    assertThat(cmd.getArguments()).containsExactly(LocalTime.parse("17:45:21"),
        OffsetDateTime.parse("2017-01-01T12:34:00+02:00"), LocalDate.parse("2003-01-01"));

  }

  @Test
  public void testParserMapList() throws ParseException {
    JavaCall cmd = JavaCall.parse("my.func ( { \"one\": 1, \"two\": 2 }, [1,2], {}, [] )");
    Map<Object, Object> arg1 = new HashMap<>();
    arg1.put("one", 1);
    arg1.put("two", 2);
    List<Integer> arg2 = Arrays.asList(1, 2);
    assertThat(cmd.getArguments()).containsExactly(arg1, arg2, Collections.emptyMap(), Collections.emptyList());

  }

}
