package io.github.jo0yo0n.mypetmate.guardian.dto;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;

@JsonTest
class RefreshRequestJsonTest {

  @Autowired private ObjectMapper objectMapper;

  @Test
  void rejectsUnknownRefreshJsonField() {
    String json =
        """
            {"refreshToken":"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA","unexpected":true}
            """;

    assertThatThrownBy(() -> objectMapper.readValue(json, RefreshRequest.class))
        .isInstanceOf(JsonProcessingException.class);
  }
}
