package io.github.jo0yo0n.mypetmate.error;

import java.util.List;

public class FieldValidationException extends BusinessException {

  private final List<FieldError> fieldErrors;

  public FieldValidationException(String field, String reason) {
    this(reason, List.of(new FieldError(field, reason)));
  }

  public FieldValidationException(String detail, List<FieldError> fieldErrors) {
    super(ErrorCode.VALIDATION_FAILED, detail);
    this.fieldErrors = List.copyOf(fieldErrors);
  }

  public List<FieldError> getFieldErrors() {
    return fieldErrors;
  }

  public record FieldError(String field, String reason) {}
}
