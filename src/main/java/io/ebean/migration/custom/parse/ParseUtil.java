package io.ebean.migration.custom.parse;


/**
 * Utility functions for parser.
 *
 * @author Roland Praml, FOCONIS AG
 */
class ParseUtil {

  /**
   * Unescapes a string that contains standard Java escape sequences.
   * <ul>
   * <li><strong>\b \f \n \r \t \" \'</strong> : BS, FF, NL, CR, TAB, double and single quote.</li>
   * <li><strong>\X \XX \X&#88;X</strong> : Octal character specification (0 - 377, 0x00 - 0xFF).</li>
   * <li><strong>\ uXXXX</strong> : Hexadecimal based Unicode character.</li>
   * </ul>
   *
   * @param st
   *            A string optionally containing standard java escape sequences.
   * @return The translated string.
   * @see "https://gist.github.com/uklimaschewski/6741769"
   */
  public static String deQuoteString(final String st) throws ParseException {

      if (st.length() < 2 || st.charAt(0) != '"' || st.charAt(st.length()-1) != '"') {
        throw new ParseException(st + " is not a valid image");
      }
      StringBuilder sb = new StringBuilder(st.length());


      for (int i = 1; i < st.length() - 1; i++) { // start at 1, end at len-1 to skip leading and trailing qutes
          char ch = st.charAt(i);
          if (ch != '\\') {
              sb.append(ch);
          } else {
              char nextChar = (i == st.length() - 1) ? '\\' : st.charAt(i + 1);
              if (nextChar >= '0' && nextChar <= '7') {
                  // Octal escape?

                  StringBuilder code = new StringBuilder(4);
                  code.append(nextChar);
                  i++;
                  if ((i < st.length() - 1) && st.charAt(i + 1) >= '0' && st.charAt(i + 1) <= '7') {
                      code.append(st.charAt(i + 1));
                      i++;
                      if ((i < st.length() - 1) && st.charAt(i + 1) >= '0' && st.charAt(i + 1) <= '7') {
                          code.append(st.charAt(i + 1));
                          i++;
                      }
                  }
                  sb.append((char) Integer.parseInt(code.toString(), 8));
              } else if (nextChar == 'u') {
                  // Hex Unicode: u????
                  if (i >= st.length() - 5) {
                      // not enough chars to consume
                      sb.append('u');
                      i++;
                  } else {
                      char[] hex = new char[4];
                      hex[0] = st.charAt(i + 2);
                      hex[1] = st.charAt(i + 3);
                      hex[2] = st.charAt(i + 4);
                      hex[3] = st.charAt(i + 5);
                      int code = Integer.parseInt(new String(hex), 16);
                      sb.append(Character.toChars(code));
                      i += 5;
                  }
              } else {
                  switch (nextChar) {
                  case 'b':
                      sb.append('\b');
                      break;
                  case 'f':
                      sb.append('\f');
                      break;
                  case 'n':
                      sb.append('\n');
                      break;
                  case 'r':
                      sb.append('\r');
                      break;
                  case 't':
                      sb.append('\t');
                      break;
                  default: // means especially  " ' \
                      sb.append(nextChar);
                      break;
                  }
                  i++;
              }
          }
      }
      return sb.toString();
  }
}
