package io.ebean.migration.ddl;

import org.testng.annotations.Test;

import java.io.StringReader;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class DdlParserTest {


  private DdlParser parser = new DdlParser();

  @Test
  public void parse_functionWithInlineComments() {

    String plpgsql = "create or replace function _partition_meta_initdateoverride(\n" +
      "  meta         partition_meta,    -- the meta data to modify\n" +
      "  initdate     date)              -- the initdate to override the period_start\n" +
      "  returns partition_meta\n" +
      "language plpgsql\n" +
      "as $$\n" +
      "begin\n" +
      "  meta.period_start = initdate;\n" +
      "  return meta;\n" +
      "end;\n" +
      "$$;";

    List<String> stmts = parser.parse(new StringReader(plpgsql));
    assertThat(stmts).containsExactly(plpgsql);
  }

  @Test
  public void parse_ignoresEmptyLines() {

    List<String> stmts = parser.parse(new StringReader("\n\none;\n\ntwo;\n\n"));
    assertThat(stmts).containsExactly("one;","two;");
  }

  @Test
  public void parse_ignoresComments_whenFirst() {

    List<String> stmts = parser.parse(new StringReader("-- comment\ntwo;"));
    assertThat(stmts).containsExactly("two;");
  }

  @Test
  public void parse_ignoresEmptyLines_whenFirst() {

    List<String> stmts = parser.parse(new StringReader("\n\n-- comment\ntwo;\n\n"));
    assertThat(stmts).containsExactly("two;");
  }

  @Test
  public void parse_inlineEmptyLines_replacedWithSpace() {

    List<String> stmts = parser.parse(new StringReader("\n\n-- comment\none\ntwo;\n\n"));
    assertThat(stmts).containsExactly("one\ntwo;");
  }


  @Test
  public void parse_ignoresComments() {

    List<String> stmts = parser.parse(new StringReader("one;\n-- comment\ntwo;"));
    assertThat(stmts).containsExactly("one;","two;");
  }

  @Test
  public void parse_ignoresEndOfLineComments() {

    List<String> stmts = parser.parse(new StringReader("one; -- comment\ntwo;"));
    assertThat(stmts).containsExactly("one;", "two;");
  }

  @Test
  public void parse_semiInContent_noLinefeedInContent() {

    List<String> stmts = parser.parse(new StringReader("insert ('one;x');"));
    assertThat(stmts).containsExactly("insert ('one;x');");
  }

  @Test
  public void parse_semiNewLineInContent_withLinefeedInContent2() {

    List<String> stmts = parser.parse(new StringReader("insert ('on;e\n');"));
    assertThat(stmts).containsExactly("insert ('on;e\n');");
  }

  @Test
  public void parse_semiNewLineInContent_withLinefeedInContent3() {

    List<String> stmts = parser.parse(new StringReader("insert ('one;\n');"));
    assertThat(stmts).containsExactly("insert ('one;\n');");
  }

  @Test
  public void parse_semiInContent() {

    List<String> stmts = parser.parse(new StringReader("';jim';\ntwo;"));
    assertThat(stmts).containsExactly("';jim';", "two;");
  }

  @Test
  public void parse_semiInContent_withTailingComments() {

    List<String> stmts = parser.parse(new StringReader("insert (';one'); -- aaa\ninsert (';two'); -- bbb"));
    assertThat(stmts).containsExactly("insert (';one');", "insert (';two');");
  }

  @Test
  public void parse_semiInContent_withLinefeedInContent() {

    List<String> stmts = parser.parse(new StringReader("insert ('one;\n');"));
    assertThat(stmts).containsExactly("insert ('one;\n');");
  }

  @Test
  public void parse_noTailingSemi() {

    List<String> stmts = parser.parse(new StringReader("one"));
    assertThat(stmts).containsExactly("one");
  }

  @Test
  public void parse_noTailingSemi_multiLine() {

    List<String> stmts = parser.parse(new StringReader("one\ntwo"));
    assertThat(stmts).containsExactly("one\ntwo");
  }
}
