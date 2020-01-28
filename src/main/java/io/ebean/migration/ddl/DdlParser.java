package io.ebean.migration.ddl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses string content into separate SQL/DDL statements.
 */
public class DdlParser {

  private final DdlAutoCommit ddlAutoCommit;

  private final StatementsSeparator parse = new StatementsSeparator();

  private final List<String> statements = new ArrayList<>();
  private final List<String> statementsNonTrans = new ArrayList<>();

  public DdlParser(DdlAutoCommit ddlAutoCommit) {
    this.ddlAutoCommit = ddlAutoCommit;
  }

  void push(String sql) {
    if (ddlAutoCommit.transactional(sql)) {
      statements.add(sql);
    } else {
      statementsNonTrans.add(sql);
    }
  }

  /**
   * Break up the sql in reader into a list of statements using the semi-colon and $$ delimiters;
   */
  public List<String> parse(Reader reader) {
    try {
      BufferedReader br = new BufferedReader(reader);
      String s;
      while ((s = br.readLine()) != null) {
        parse.nextLine(s);
      }
      parse.endOfContent();
      return statements;
    } catch (IOException e) {
      throw new DdlRunnerException(e);
    }
  }

  /**
   * Return the non-transactional statements which run later with auto commit true.
   */
  public List<String> getNonTransactional() {
    return statementsNonTrans;
  }

  /**
   * Local utility used to detect the end of statements / separate statements.
   * This is often just the semicolon character but for trigger/procedures this
   * detects the $$ demarcation used in the history DDL generation for MySql and
   * Postgres.
   */
  class StatementsSeparator {

    private static final String EOL = "\n";

    private static final String GO = "GO";
    private static final String PROCEDURE = " PROCEDURE ";

    private boolean trimDelimiter;
    private boolean inDbProcedure;

    private StringBuilder sb = new StringBuilder();

    private int lineCount;
    private int quoteCount;

    void lineContainsDollars(String line) {
      if (inDbProcedure) {
        if (trimDelimiter) {
          line = line.replace("$$", "");
        }
        endOfStatement(line);
      } else {
        // MySql style delimiter needs to be trimmed/removed
        trimDelimiter = line.equals("delimiter $$");
        if (!trimDelimiter) {
          sb.append(line).append(EOL);
        }
        inDbProcedure = true;
      }
    }

    void endOfStatement(String line) {
      // end of Db procedure
      sb.append(line);
      push(sb.toString().trim());
      newBuffer();
    }

    private void newBuffer() {
      quoteCount = 0;
      lineCount = 0;
      inDbProcedure = false;
      sb = new StringBuilder();
    }

    /**
     * Process the next line of the script.
     */
    void nextLine(String line) {

      if (line.trim().equals(GO)) {
        endOfStatement("");
        return;
      }

      if (line.contains("$$")) {
        lineContainsDollars(line);
        return;
      }

      if (inDbProcedure) {
        sb.append(line).append(EOL);
        return;
      }

      if (sb.length() == 0 && (line.isEmpty() || line.startsWith("--"))) {
        // ignore leading empty lines and sql comments
        return;
      }

      if (lineCount == 0 && isStartDbProcedure(line)) {
        inDbProcedure = true;
      }

      lineCount++;
      quoteCount += countQuotes(line);
      if (hasOddQuotes()) {
        // must continue
        sb.append(line).append(EOL);
        return;
      }

      int semiPos = line.lastIndexOf(';');
      if (semiPos == -1) {
        sb.append(line).append(EOL);

      } else if (semiPos == line.length() - 1) {
        // semicolon at end of line
        endOfStatement(line);

      } else {
        // semicolon in middle of line
        String remaining = line.substring(semiPos + 1).trim();
        if (!remaining.startsWith("--")) {
          // remaining not an inline sql comment so keep going ...
          sb.append(line).append(EOL);
          return;
        }

        String preSemi = line.substring(0, semiPos + 1);
        endOfStatement(preSemi);
      }
    }

    /**
     * Return true if the start of DB procedure is detected.
     */
    private boolean isStartDbProcedure(String line) {
      return line.length() > 26 && line.substring(0, 26).toUpperCase().contains(PROCEDURE);
    }

    /**
     * Return true if the count of quotes is odd.
     */
    private boolean hasOddQuotes() {
      return quoteCount % 2 == 1;
    }

    /**
     * Return the count of single quotes in the content.
     */
    private int countQuotes(String content) {
      int count = 0;
      for (int i = 0; i < content.length(); i++) {
        if (content.charAt(i) == '\'') {
          count++;
        }
      }
      return count;
    }

    /**
     * Append trailing non-terminated content as an extra statement.
     */
    void endOfContent() {
      String remaining = sb.toString().trim();
      if (remaining.length() > 0) {
        push(remaining);
        newBuffer();
      }
    }
  }
}
