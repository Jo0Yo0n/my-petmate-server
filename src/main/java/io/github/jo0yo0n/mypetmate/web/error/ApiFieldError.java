package io.github.jo0yo0n.mypetmate.web.error;

import org.springframework.lang.NonNull;

public record ApiFieldError(@NonNull String field, String reason) {

  public ApiFieldError {

    int fieldCodePointsCount = field.codePointCount(0, field.length());
    if (fieldCodePointsCount > 200) {
      field = field.substring(0, field.offsetByCodePoints(0, 200));
    }

    if (reason == null || reason.isEmpty()) {
      reason = "유효하지 않은 값입니다.";
    }

    int reasonCodePointCount = reason.codePointCount(0, reason.length());
    if (reasonCodePointCount > 500) {
      reason = reason.substring(0, reason.offsetByCodePoints(0, 500));
    }
  }
}
