package io.github.jo0yo0n.mypetmate;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MyPetmateServerApplicationTests {

  @Test
  void applicationEntryPointExists() {
    assertThat(MyPetmateServerApplication.class).isNotNull();
  }
}
