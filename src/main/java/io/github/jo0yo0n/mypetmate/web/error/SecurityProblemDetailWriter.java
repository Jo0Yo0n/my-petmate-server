package io.github.jo0yo0n.mypetmate.web.error;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;

@Component
public class SecurityProblemDetailWriter {

  private final ObjectMapper objectMapper;

  public SecurityProblemDetailWriter(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  void write(ProblemDetail problemDetail, HttpServletResponse response) throws IOException {

    response.setStatus(problemDetail.getStatus());
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    objectMapper.writeValue(response.getOutputStream(), problemDetail);
  }
}
