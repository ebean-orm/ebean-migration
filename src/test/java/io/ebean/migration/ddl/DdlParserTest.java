package io.ebean.migration.ddl;

import org.testng.annotations.Test;

import java.io.FileNotFoundException;
import java.io.FileReader;
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
  public void parse_sqlserver_create_procs() throws FileNotFoundException {

    FileReader fr = new FileReader("src/test/resources/dbmig_sqlserver/I__create_procs.sql");
    final List<String> statements = parser.parse(fr);

    assertThat(statements).hasSize(11);
    assertThat(statements.get(0)).isEqualTo("if not exists (select name  from sys.types where name = 'ebean_bigint_tvp')\n" +
      "create type ebean_bigint_tvp as table (c1 bigint)");

    assertThat(statements.get(6)).isEqualTo("if not exists (select name  from sys.types where name = 'ebean_nvarchar_tvp')\n" +
      "create type ebean_nvarchar_tvp as table (c1 nvarchar(max))");

    assertThat(statements.get(8)).isEqualTo("CREATE OR ALTER PROCEDURE usp_ebean_drop_default_constraint @tableName nvarchar(255), @columnName nvarchar(255)\n" +
      "AS SET NOCOUNT ON\n" +
      "declare @tmp nvarchar(1000)\n" +
      "BEGIN\n" +
      "  select @tmp = t1.name from sys.default_constraints t1\n" +
      "    join sys.columns t2 on t1.object_id = t2.default_object_id\n" +
      "    where t1.parent_object_id = OBJECT_ID(@tableName) and t2.name = @columnName;\n" +
      "\n" +
      "  if @tmp is not null EXEC('alter table ' + @tableName +' drop constraint ' + @tmp);\n" +
      "END");
  }


  @Test
  public void parse_postgres_procs() throws FileNotFoundException {

    FileReader fr = new FileReader("src/test/resources/dbmig_postgres/ex1.sql");
    final List<String> statements = parser.parse(fr);

    assertThat(statements).hasSize(3);
    assertThat(statements.get(0)).isEqualTo("create or replace function hx_link_history_version() returns trigger as $$\n" +
      "begin\n" +
      "\n" +
      "end;\n" +
      "$$ LANGUAGE plpgsql;");

    assertThat(statements.get(1)).isEqualTo("create trigger hx_link_history_upd\n" +
      "    before update or delete on hx_link\n" +
      "    for each row execute procedure hx_link_history_version();");

    assertThat(statements.get(2)).isEqualTo("create or replace function hi_link_history_version() returns trigger as $$\n" +
      "begin\n" +
      "end;\n" +
      "$$ LANGUAGE plpgsql;");
  }


  @Test
  public void parse_postgres_createAll() throws FileNotFoundException {

    FileReader fr = new FileReader("src/test/resources/dbmig_postgres/pg-create-all.sql");
    final List<String> statements = parser.parse(fr);

    assertThat(statements).hasSize(1013);
  }

  @Test
  public void parse_mysql_createAll() throws FileNotFoundException {

    FileReader fr = new FileReader("src/test/resources/dbmig_mysql/mysql-create-all.sql");
    final List<String> statements = parser.parse(fr);
    assertThat(statements).hasSize(1010);
  }

  @Test
  public void parse_mysql_dropAll() throws FileNotFoundException {

    FileReader fr = new FileReader("src/test/resources/dbmig_mysql/mysql-drop-all.sql");
    final List<String> statements = parser.parse(fr);
    assertThat(statements).hasSize(997);
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
