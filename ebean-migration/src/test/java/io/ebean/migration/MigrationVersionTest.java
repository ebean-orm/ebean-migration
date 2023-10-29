package io.ebean.migration;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MigrationVersionTest {


  @Test
  void sort() {

    List<MigrationVersion> list = new ArrayList<>();
    list.add(MigrationVersion.parse("1.1__point"));
    list.add(MigrationVersion.parse("3.0__three"));
    list.add(MigrationVersion.parse("1.0__init"));
    list.add(MigrationVersion.parse("R__beta"));
    list.add(MigrationVersion.parse("R__alpha"));

    Collections.sort(list);

    assertThat(list.get(0).comment()).isEqualTo("init");
    assertThat(list.get(1).comment()).isEqualTo("point");
    assertThat(list.get(2).comment()).isEqualTo("three");
    assertThat(list.get(3).comment()).isEqualTo("alpha");
    assertThat(list.get(4).comment()).isEqualTo("beta");
  }

  @Test
  void test_subversions() {

    MigrationVersion v141 = MigrationVersion.parse("V1.4.1__comment");
    MigrationVersion v14 = MigrationVersion.parse("V1.4__comment");
    assertThat(v141).isGreaterThan(v14);
    assertThat(v14).isLessThan(v141);
  }

  @Test
  void test_parse_hyphenSnapshot() {

    MigrationVersion version = MigrationVersion.parse("0.1.1-SNAPSHOT");
    assertThat(version.normalised()).isEqualTo("0.1.1");
    assertThat(version.comment()).isEqualTo("");
    assertThat(version.type()).isEqualTo("V");
  }

  @Test
  void test_parse_hyphenSnapshot_when_underscores() {

    MigrationVersion version = MigrationVersion.parse("0_1_1-SNAPSHOT__Foo");
    assertThat(version.normalised()).isEqualTo("0.1.1");
    assertThat(version.comment()).isEqualTo("Foo");
  }

  @Test
  void test_parse_when_repeatable() {

    MigrationVersion version = MigrationVersion.parse("R__Foo");
    assertThat(version.comment()).isEqualTo("Foo");
    assertThat(version.isRepeatable()).isTrue();
    assertThat(version.normalised()).isEqualTo("R");
    assertThat(version.type()).isEqualTo("R");
  }

  @Test
  void test_parse_when_init() {

    MigrationVersion version = MigrationVersion.parse("I__Foo");
    assertThat(version.comment()).isEqualTo("Foo");
    assertThat(version.isRepeatable()).isTrue();
    assertThat(version.normalised()).isEqualTo("I");
    assertThat(version.type()).isEqualTo("I");
  }

  @Test
  void test_parse_when_repeatable_R1() {

    MigrationVersion version = MigrationVersion.parse("R1__Foo");
    assertThat(version.comment()).isEqualTo("Foo");
    assertThat(version.normalised()).isEqualTo("R");
    assertThat(version.isRepeatable()).isTrue();
    assertThat(version.type()).isEqualTo("R");
  }

  @Test
  void test_parse_when_repeatable_case() {

    MigrationVersion version = MigrationVersion.parse("r__Foo");
    assertThat(version.isRepeatable()).isTrue();
    assertThat(version.comment()).isEqualTo("Foo");
    assertThat(version.normalised()).isEqualTo("R");
    assertThat(version.type()).isEqualTo("R");
  }

  @Test
  void test_parse_when_v_prefix() {

    MigrationVersion version = MigrationVersion.parse("v1_0__Foo");
    assertThat(version.isRepeatable()).isFalse();
    assertThat(version.comment()).isEqualTo("Foo");
    assertThat(version.normalised()).isEqualTo("1.0");
    assertThat(version.asString()).isEqualTo("1_0");
    assertThat(version.raw()).isEqualTo("1_0__Foo");
    assertThat(version.type()).isEqualTo("V");
  }

  @Test
  void test_parse_when_sql_suffix() {

    MigrationVersion version = MigrationVersion.parse("v1_0__Foo.sql");
    assertThat(version.isRepeatable()).isFalse();
    assertThat(version.comment()).isEqualTo("Foo");
    assertThat(version.normalised()).isEqualTo("1.0");
    assertThat(version.asString()).isEqualTo("1_0");
    assertThat(version.raw()).isEqualTo("1_0__Foo");
    assertThat(version.type()).isEqualTo("V");
  }

  @Test
  void repeatable_compareTo() {

    MigrationVersion foo = MigrationVersion.parse("R__Foo");
    MigrationVersion bar = MigrationVersion.parse("R__Bar");
    assertThat(foo.compareTo(bar)).isGreaterThan(0);
    assertThat(bar.compareTo(foo)).isLessThan(0);

    MigrationVersion bar2 = MigrationVersion.parse("R__Bar");
    assertThat(bar.compareTo(bar2)).isEqualTo(0);
  }

  @Test
  void repeatable_init_compareTo() {

    MigrationVersion foo = MigrationVersion.parse("R__Foo");
    MigrationVersion goo = MigrationVersion.parse("I__Goo");
    assertThat(foo.compareTo(goo)).isGreaterThan(0);
    assertThat(goo.compareTo(foo)).isLessThan(0);
  }

  @Test
  void repeatable_compareTo_when_caseDifferent() {

    MigrationVersion none = MigrationVersion.parse("R__");
    MigrationVersion bar = MigrationVersion.parse("R__Bar");
    MigrationVersion bar2 = MigrationVersion.parse("R__bar");
    assertThat(none.compareTo(bar)).isLessThan(0);
    assertThat(bar.compareTo(bar2)).isLessThan(0);
    assertThat(none.compareTo(bar2)).isLessThan(0);
  }

  @Test
  void test_parse_getComment(){

    assertThat(MigrationVersion.parse("1.1.1_2__Foo").comment()).isEqualTo("Foo");
    assertThat(MigrationVersion.parse("1.1.1.2__junk").comment()).isEqualTo("junk");
    assertThat(MigrationVersion.parse("1.1_1.2_foo").comment()).isEqualTo("");
    assertThat(MigrationVersion.parse("1.1_1.2_d").comment()).isEqualTo("");
    assertThat(MigrationVersion.parse("1.1_1.2_").comment()).isEqualTo("");
    assertThat(MigrationVersion.parse("1.1_1.2").comment()).isEqualTo("");
  }

  @Test
  void test_nextVersion_expect_preserveUnderscores() {

    assertThat(MigrationVersion.parse("2").nextVersion()).isEqualTo("3");
    assertThat(MigrationVersion.parse("1.0").nextVersion()).isEqualTo("1.1");
    assertThat(MigrationVersion.parse("2.0.b34").nextVersion()).isEqualTo("2.1");
    assertThat(MigrationVersion.parse("1.1.1_2__Foo").nextVersion()).isEqualTo("1.1.1_3");
    assertThat(MigrationVersion.parse("1.1.1.2_junk").nextVersion()).isEqualTo("1.1.1.3");
    assertThat(MigrationVersion.parse("1_2.3_4__Foo").nextVersion()).isEqualTo("1_2.3_5");
    assertThat(MigrationVersion.parse("1_2.3_4_").nextVersion()).isEqualTo("1_2.3_5");
    assertThat(MigrationVersion.parse("1_2_3_4__Foo").nextVersion()).isEqualTo("1_2_3_5");
  }

  @Test
  void test_normalised_expect_periods() {

    assertThat(MigrationVersion.parse("2").normalised()).isEqualTo("2");
    assertThat(MigrationVersion.parse("1.0").normalised()).isEqualTo("1.0");
    assertThat(MigrationVersion.parse("2.0.b34").normalised()).isEqualTo("2.0");
    assertThat(MigrationVersion.parse("1.1.1_2__Foo").normalised()).isEqualTo("1.1.1.2");
    assertThat(MigrationVersion.parse("1.1.1.2_junk").normalised()).isEqualTo("1.1.1.2");
    assertThat(MigrationVersion.parse("1_2.3_4__Foo").normalised()).isEqualTo("1.2.3.4");
    assertThat(MigrationVersion.parse("1_2.3_4_").normalised()).isEqualTo("1.2.3.4");
    assertThat(MigrationVersion.parse("1_2_3_4__Foo").normalised()).isEqualTo("1.2.3.4");
  }

  @Test
  void test_compareTo_isEqual() {

    MigrationVersion v0 = MigrationVersion.parse("1.1.1_2__Foo");
    MigrationVersion v1 = MigrationVersion.parse("1.1.1.2_junk");
    MigrationVersion v2 = MigrationVersion.parse("1.1_1.2_foo");
    MigrationVersion v3 = MigrationVersion.parse("1.1_1.2__foo");

    MigrationVersion v4 = MigrationVersion.parse("1.1_1.2");

    assertThat(v0.compareTo(v1)).isEqualTo(0);
    assertThat(v1.compareTo(v0)).isEqualTo(0);
    assertThat(v1.compareTo(v2)).isEqualTo(0);

    assertThat(v0.compareTo(v3)).isEqualTo(0);
    assertThat(v3.compareTo(v0)).isEqualTo(0);

    assertThat(v4.compareTo(v2)).isEqualTo(0);
  }

  @Test
  void test_compareTo() {

    MigrationVersion v0 = MigrationVersion.parse("1.1.1.1_junk");
    MigrationVersion v1 = MigrationVersion.parse("1.1.1.2_junk");
    MigrationVersion v2 = MigrationVersion.parse("1.1_1.3_junk");
    MigrationVersion v3 = MigrationVersion.parse("1.2_1.2_junk");
    MigrationVersion v4 = MigrationVersion.parse("2.1_1.2_junk");

    assertThat(v1.compareTo(v0)).isEqualTo(1);

    assertThat(v1.compareTo(v2)).isEqualTo(-1);
    assertThat(v1.compareTo(v3)).isEqualTo(-1);
    assertThat(v1.compareTo(v4)).isEqualTo(-1);
  }
}
