package io.ebean.migration.runner;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

public class ChecksumTest {

  @Test
  void test_calculate() {

    int checkFoo = Checksum.calculate("foo");

    assertThat(Checksum.calculate("foo")).isEqualTo(checkFoo);
    assertThat(Checksum.calculate("Foo")).isNotEqualTo(checkFoo);
  }

}