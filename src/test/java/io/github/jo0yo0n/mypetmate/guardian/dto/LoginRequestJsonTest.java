package io.github.jo0yo0n.mypetmate.guardian.dto;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;

@JsonTest
class LoginRequestJsonTest {

  @Autowired private ObjectMapper objectMapper;

  @Test
  void rejectsUnknownLoginJsonField() {
    String json =
        """
            {"email":"guardian@example.com","password":"StringPass123!","unexpected":true}
            """;

    assertThatThrownBy(() -> objectMapper.readValue(json, LoginRequest.class))
        .isInstanceOf(JsonProcessingException.class);
  }
}
