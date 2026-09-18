package io.github.jo0yo0n.mypetmate.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.jo0yo0n.mypetmate.config.RequestIdConfig;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(TestController.class)
@Import(RequestIdConfig.class)
public class RequestIdFilterMvcTest {

  @Autowired MockMvc mockMvc;

  @DisplayName("[M1-REQ-01] generatesDistinctRequestIdsForSuccessfulAndMvcErrorResponses")
  @WithMockUser
  @Test
  void generatesDistinctRequestIdsForSuccessfulAndMvcErrorResponses() throws Exception {

    Set<String> requestIdSet = new HashSet<>();

    requestIdSet.add(generateMvcRequest(true, "200"));
    requestIdSet.add(generateMvcRequest(true, "200"));
    requestIdSet.add(generateMvcRequest(false, "abc"));
    requestIdSet.add(generateMvcRequest(false, "abc"));

    assertThat(requestIdSet.size()).isEqualTo(4);
  }

  String generateMvcRequest(boolean isOk, String paramValue) throws Exception {
    return mockMvc
        .perform(get("/test/request-id").param("count", paramValue))
        .andExpectAll(
            isOk ? status().isOk() : status().isBadRequest(),
            header()
                .string(
                    "My-Petmate-Request-Id", matchesPattern("^req-[0-7][0-9A-HJKMNP-TV-Z]{25}$")))
        .andReturn()
        .getResponse()
        .getHeader("My-Petmate-Request-Id");
  }
}

@RestController
class TestController {

  @GetMapping("/test/request-id")
  ResponseEntity<Void> getTestController(@RequestParam int count) {

    return ResponseEntity.ok().build();
  }
}
