package io.ebean.migration.ddl;

import java.io.StringReader;
import java.util.List;
import org.testng.annotations.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class DdlParserTest {


  private DdlParser parser = new DdlParser();

  @Test
  public void parse_ignoresEmptyLines() throws Exception {

    List<String> stmts = parser.parse(new StringReader("\n\none;\n\ntwo;\n\n"));
    assertThat(stmts).containsExactly("one;","two;");
  }

  @Test
  public void parse_ignoresComments_whenFirst() throws Exception {

    List<String> stmts = parser.parse(new StringReader("-- comment\ntwo;"));
    assertThat(stmts).containsExactly("two;");
  }

  @Test
  public void parse_ignoresEmptyLines_whenFirst() throws Exception {

    List<String> stmts = parser.parse(new StringReader("\n\n-- comment\ntwo;\n\n"));
    assertThat(stmts).containsExactly("two;");
  }

  @Test
  public void parse_inlineEmptyLines_replacedWithSpace() throws Exception {

    List<String> stmts = parser.parse(new StringReader("\n\n-- comment\none\ntwo;\n\n"));
    assertThat(stmts).containsExactly("one two;");
  }


  @Test
  public void parse_ignoresComments() throws Exception {

    List<String> stmts = parser.parse(new StringReader("one;\n-- comment\ntwo;"));
    assertThat(stmts).containsExactly("one;","two;");
  }

  @Test
  public void parse_ignoresEndOfLineComments() throws Exception {

    List<String> stmts = parser.parse(new StringReader("one; -- comment\ntwo;"));
    assertThat(stmts).containsExactly("one;", "two;");
  }

  @Test
  public void parse_semiInContent() throws Exception {

    List<String> stmts = parser.parse(new StringReader("';jim';\ntwo;"));
    assertThat(stmts).containsExactly("';jim';", "two;");
  }

  @Test
  public void parse_semiInContent_withTailingComments() throws Exception {

    List<String> stmts = parser.parse(new StringReader("insert (';one'); -- aaa\ninsert (';two'); -- bbb"));
    assertThat(stmts).containsExactly("insert (';one');", "insert (';two');");
  }

  @Test
  public void parse_noTailingSemi() throws Exception {

    List<String> stmts = parser.parse(new StringReader("one"));
    assertThat(stmts).containsExactly("one");
  }

  @Test
  public void parse_noTailingSemi_multiLine() throws Exception {

    List<String> stmts = parser.parse(new StringReader("one\ntwo"));
    assertThat(stmts).containsExactly("one two");
  }
}
