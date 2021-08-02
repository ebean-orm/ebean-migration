package io.ebean.migration.runner;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;

/**
 * Calculates the checksum for the given string content.
 */
class Checksum {

  /**
   * Returns the checksum of the string content.
   */
  static int calculate(String str) {
    final CRC32 crc32 = new CRC32();
    BufferedReader bufferedReader = new BufferedReader(new StringReader(str));
    try {
      String line;
      while ((line = bufferedReader.readLine()) != null) {
        final byte[] lineBytes = line.getBytes(StandardCharsets.UTF_8);
        crc32.update(lineBytes, 0, lineBytes.length);
      }
    } catch (IOException e) {
      throw new RuntimeException("Failed to calculate checksum", e);
    }
    return (int) crc32.getValue();
  }
}
