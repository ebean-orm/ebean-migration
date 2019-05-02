package io.ebean.migration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 * The version of a migration used so that migrations are processed in order.
 */
public class MigrationVersion implements Comparable<MigrationVersion> {

  private static final Logger logger = LoggerFactory.getLogger(MigrationVersion.class);

  public static final String BOOTINIT_TYPE = "B";

  private static final String INIT_TYPE = "I";

  private static final String REPEAT_TYPE = "R";

  private static final String VERSION_TYPE = "V";

  private static final int[] REPEAT_ORDERING_MIN = {Integer.MIN_VALUE};

  private static final int[] REPEAT_ORDERING_MAX = {Integer.MAX_VALUE};

  private static final boolean[] REPEAT_UNDERSCORES = {false};

  /**
   * The raw version text.
   */
  private final String raw;

  /**
   * The ordering parts.
   */
  private final int[] ordering;

  private final boolean[] underscores;

  private final String comment;

  /**
   * Construct for "repeatable" version.
   */
  private MigrationVersion(String raw, String comment, boolean init) {
    this.raw = raw;
    this.comment = comment;
    this.ordering = init ? REPEAT_ORDERING_MIN : REPEAT_ORDERING_MAX;
    this.underscores = REPEAT_UNDERSCORES;
  }

  /**
   * Construct for "normal" version.
   */
  private MigrationVersion(String raw, int[] ordering, boolean[] underscores, String comment) {
    this.raw = raw;
    this.ordering = ordering;
    this.underscores = underscores;
    this.comment = comment;
  }

  /**
   * Return true if this is a "repeatable" version.
   */
  public boolean isRepeatable() {
    return ordering == REPEAT_ORDERING_MIN || ordering == REPEAT_ORDERING_MAX;
  }

  /**
   * Return true if this is a "repeatable init" verision.
   */
  public boolean isRepeatableInit() {
    return ordering == REPEAT_ORDERING_MIN;
  }

  /**
   * Return true if this is a "repeatable last" verision.
   */
  public boolean isRepeatableLast() {
    return ordering == REPEAT_ORDERING_MAX;
  }

  /**
   * Return the full version.
   */
  public String getFull() {
    return raw;
  }

  public String toString() {
    return raw;
  }

  /**
   * Return the version comment.
   */
  public String getComment() {
    return comment;
  }

  /**
   * Return the version in raw form.
   */
  public String getRaw() {
    return raw;
  }

  /**
   * Return the trimmed version excluding version comment and un-parsable string.
   */
  public String asString() {
    return formattedVersion(false, false);
  }

  /**
   * Return the trimmed version with any underscores replaced with '.'
   */
  public String normalised() {
    return formattedVersion(true, false);
  }

  /**
   * Return the next version based on this version.
   */
  public String nextVersion() {
    return formattedVersion(false, true);
  }

  /**
   * Returns the version part of the string.
   * <p>
   * Normalised means always use '.' delimiters (no underscores).
   * NextVersion means bump/increase the last version number by 1.
   */
  private String formattedVersion(boolean normalised, boolean nextVersion) {

    if (isRepeatable()) {
      return getType();
    }
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < ordering.length; i++) {
      if (i < ordering.length - 1) {
        sb.append(ordering[i]);
        if (normalised) {
          sb.append('.');
        } else {
          sb.append(underscores[i] ? '_' : '.');
        }
      } else {
        sb.append((nextVersion) ? ordering[i] + 1 : ordering[i]);
      }
    }
    return sb.toString();
  }

  @Override
  public int compareTo(MigrationVersion other) {

    int otherLength = other.ordering.length;
    for (int i = 0; i < ordering.length; i++) {
      if (i >= otherLength) {
        // considered greater
        return 1;
      }
      if (ordering[i] != other.ordering[i]) {
        return (ordering[i] > other.ordering[i]) ? 1 : -1;
      }
    }
    if (ordering.length < otherLength) {
      return -1;
    }
    return isRepeatable() ? comment.compareTo(other.comment) : 0;
  }

  /**
   * Parse the raw version string and just return the leading version number;
   */
  public static String trim(String raw) {
    return parse(raw).asString();
  }

  /**
   * Parse the raw version string into a MigrationVersion.
   */
  public static MigrationVersion parse(String raw) {

    if (raw.startsWith("V") || raw.startsWith("v")) {
      raw = raw.substring(1);
    }

    String comment = "";
    String value = raw;
    int commentStart = raw.indexOf("__");
    if (commentStart > -1) {
      // trim off the trailing comment
      comment = raw.substring(commentStart + 2);
      value = value.substring(0, commentStart);
    }

    value = value.replace('_', '.');

    String[] sections = value.split("[\\.-]");

    if (sections[0].startsWith("R") || sections[0].startsWith("r")) {
      // a "repeatable" version (does not have a version number)
      return new MigrationVersion(raw, comment, false);
    }

    if (sections[0].startsWith("I") || sections[0].startsWith("i")) {
      // this script will be executed before all other scripts
      return new MigrationVersion(raw, comment, true);
    }

    boolean[] underscores = new boolean[sections.length];
    int[] ordering = new int[sections.length];

    int delimiterPos = 0;
    int stopIndex = 0;
    for (int i = 0; i < sections.length; i++) {
      try {
        ordering[i] = Integer.parseInt(sections[i]);
        stopIndex++;

        delimiterPos += sections[i].length();
        underscores[i] = (delimiterPos < raw.length() - 1 && raw.charAt(delimiterPos) == '_');
        delimiterPos++;
      } catch (NumberFormatException e) {
        // stop parsing
        logger.warn("The migrationscript '{}' contains non numeric version part. "
          + "This may lead to misordered version scripts. NumberFormatException {}", raw, e.getMessage());
        break;
      }
    }

    int[] actualOrder = Arrays.copyOf(ordering, stopIndex);
    boolean[] actualUnderscores = Arrays.copyOf(underscores, stopIndex);

    return new MigrationVersion(raw, actualOrder, actualUnderscores, comment);
  }

  /**
   * Return the version type (I, R or V).
   */
  public String getType() {
    if (ordering == REPEAT_ORDERING_MIN) {
      return INIT_TYPE;

    } else if (ordering == REPEAT_ORDERING_MAX) {
      return REPEAT_TYPE;

    } else {
      return VERSION_TYPE;
    }
  }
}
