package io.ebean.migration.runner;

import org.testng.annotations.Test;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

public class ChecksumTest {

  @Test
  public void test_calculate() throws Exception {

    int checkFoo = Checksum.calculate("foo");

    assertThat(Checksum.calculate("foo")).isEqualTo(checkFoo);
    assertThat(Checksum.calculate("Foo")).isNotEqualTo(checkFoo);
  }

}