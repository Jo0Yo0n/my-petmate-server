package io.github.jo0yo0n.mypetmate.web.error;

import io.github.jo0yo0n.mypetmate.error.BusinessException;
import io.github.jo0yo0n.mypetmate.error.ErrorCode;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.lang.NonNull;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  public GlobalExceptionHandler() {}

  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<Object> handleBusinessException(
      @NonNull BusinessException ex, @NonNull WebRequest request) {

    ErrorCode errorCode = ex.getErrorCode();
    ProblemDetail problemDetail =
        ProblemDetailFactory.create(
            errorCode, ex.getDetail(), getRequestURI(request), getRequestId(request));

    return new ResponseEntity<>(problemDetail, HttpHeaders.EMPTY, problemDetail.getStatus());
  }

  @Override
  public ResponseEntity<Object> handleMethodArgumentNotValid(
      @NonNull MethodArgumentNotValidException ex,
      @NonNull HttpHeaders headers,
      @NonNull HttpStatusCode status,
      @NonNull WebRequest request) {

    return createResponseEntity(ex, headers, request);
  }

  @Override
  public ResponseEntity<Object> handleHandlerMethodValidationException(
      @NonNull HandlerMethodValidationException ex,
      @NonNull HttpHeaders headers,
      @NonNull HttpStatusCode status,
      @NonNull WebRequest request) {

    if (!ex.isForReturnValue()) {
      return createResponseEntity(ex, headers, request);
    } else {
      ProblemDetail problemDetail =
          ProblemDetailFactory.create(
              ErrorCode.INTERNAL_SERVER_ERROR,
              "요청을 처리하는 중 오류가 발생했습니다.",
              getRequestURI(request),
              getRequestId(request));

      return new ResponseEntity<>(problemDetail, headers, problemDetail.getStatus());
    }
  }

  @Override
  public ResponseEntity<Object> handleHttpMessageNotReadable(
      @NonNull HttpMessageNotReadableException ex,
      @NonNull HttpHeaders headers,
      @NonNull HttpStatusCode status,
      @NonNull WebRequest request) {

    ProblemDetail problemDetail =
        ProblemDetailFactory.create(
            ErrorCode.MALFORMED_REQUEST,
            "요청값을 확인해 주세요.",
            getRequestURI(request),
            getRequestId(request));

    return new ResponseEntity<>(problemDetail, headers, problemDetail.getStatus());
  }

  @Override
  public ResponseEntity<Object> handleTypeMismatch(
      @NonNull TypeMismatchException ex,
      @NonNull HttpHeaders headers,
      @NonNull HttpStatusCode status,
      @NonNull WebRequest request) {

    String field = Objects.requireNonNullElse(ex.getPropertyName(), "unknown");

    ProblemDetail problemDetail =
        ProblemDetailFactory.create(
            ErrorCode.VALIDATION_FAILED,
            "요청값 형식을 확인해 주세요.",
            getRequestURI(request),
            getRequestId(request),
            List.of(new ApiFieldError(field, "올바른 형식으로 입력해 주세요.")));

    return new ResponseEntity<>(problemDetail, headers, problemDetail.getStatus());
  }

  @Override
  public ResponseEntity<Object> handleMissingServletRequestParameter(
      @NonNull MissingServletRequestParameterException ex,
      @NonNull HttpHeaders headers,
      @NonNull HttpStatusCode status,
      @NonNull WebRequest request) {

    ProblemDetail problemDetail =
        ProblemDetailFactory.create(
            ErrorCode.VALIDATION_FAILED,
            "필수 요청값을 입력해 주세요.",
            getRequestURI(request),
            getRequestId(request),
            List.of(new ApiFieldError(ex.getParameterName(), "필수값입니다.")));

    return new ResponseEntity<>(problemDetail, headers, problemDetail.getStatus());
  }

  @Override
  public ResponseEntity<Object> handleHttpMediaTypeNotSupported(
      @NonNull HttpMediaTypeNotSupportedException ex,
      @NonNull HttpHeaders headers,
      @NonNull HttpStatusCode status,
      @NonNull WebRequest request) {

    ProblemDetail problemDetail =
        ProblemDetailFactory.create(
            ErrorCode.UNSUPPORTED_MEDIA_TYPE,
            "지원하지 않는 요청 Content-Type입니다.",
            getRequestURI(request),
            getRequestId(request));

    return new ResponseEntity<>(problemDetail, headers, problemDetail.getStatus());
  }

  @Override
  public ResponseEntity<Object> handleHttpRequestMethodNotSupported(
      @NonNull HttpRequestMethodNotSupportedException ex,
      @NonNull HttpHeaders headers,
      @NonNull HttpStatusCode status,
      @NonNull WebRequest request) {

    ProblemDetail problemDetail =
        ProblemDetailFactory.create(
            ErrorCode.METHOD_NOT_ALLOWED,
            "이 endpoint에서 지원하지 않는 HTTP method입니다.",
            getRequestURI(request),
            getRequestId(request));

    return new ResponseEntity<>(problemDetail, headers, problemDetail.getStatus());
  }

  @Override
  public ResponseEntity<Object> handleHttpMediaTypeNotAcceptable(
      @NonNull HttpMediaTypeNotAcceptableException ex,
      @NonNull HttpHeaders headers,
      @NonNull HttpStatusCode status,
      @NonNull WebRequest request) {

    ProblemDetail problemDetail =
        ProblemDetailFactory.create(
            ErrorCode.NOT_ACCEPTABLE,
            "요청한 응답 media type을 지원하지 않습니다.",
            getRequestURI(request),
            getRequestId(request));

    return new ResponseEntity<>(problemDetail, headers, problemDetail.getStatus());
  }

  @Override
  public ResponseEntity<Object> handleNoHandlerFoundException(
      @NonNull NoHandlerFoundException ex,
      @NonNull HttpHeaders headers,
      @NonNull HttpStatusCode status,
      @NonNull WebRequest request) {

    ProblemDetail problemDetail =
        ProblemDetailFactory.create(
            ErrorCode.RESOURCE_NOT_FOUND,
            "요청한 리소스를 찾을 수 없습니다.",
            getRequestURI(request),
            getRequestId(request));

    return new ResponseEntity<>(problemDetail, headers, problemDetail.getStatus());
  }

  @Override
  public ResponseEntity<Object> handleNoResourceFoundException(
      @NonNull NoResourceFoundException ex,
      @NonNull HttpHeaders headers,
      @NonNull HttpStatusCode status,
      @NonNull WebRequest request) {

    ProblemDetail problemDetail =
        ProblemDetailFactory.create(
            ErrorCode.RESOURCE_NOT_FOUND,
            "요청한 리소스를 찾을 수 없습니다.",
            getRequestURI(request),
            getRequestId(request));

    return new ResponseEntity<>(problemDetail, headers, problemDetail.getStatus());
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Object> handleUnexpectableException(
      @NonNull Exception ex, @NonNull WebRequest request) {

    ProblemDetail problemDetail =
        ProblemDetailFactory.create(
            ErrorCode.INTERNAL_SERVER_ERROR,
            "요청을 처리하는 중 오류가 발생했습니다.",
            getRequestURI(request),
            getRequestId(request));

    log.error(
        "event=unexpected_error code={} requestId={}",
        ErrorCode.INTERNAL_SERVER_ERROR.getCode(),
        getRequestId(request));
    return new ResponseEntity<>(problemDetail, HttpHeaders.EMPTY, problemDetail.getStatus());
  }

  private ResponseEntity<Object> createResponseEntity(
      Exception ex, HttpHeaders headers, WebRequest request) {

    List<ApiFieldError> apiFieldErrors;
    if (ex instanceof MethodArgumentNotValidException methodArgumentNotValidException) {
      apiFieldErrors = extractErrors(methodArgumentNotValidException);
    } else {
      HandlerMethodValidationException handlerMethodValidationException =
          (HandlerMethodValidationException) ex;
      apiFieldErrors = extractErrors(handlerMethodValidationException);
    }

    ProblemDetail problemDetail =
        ProblemDetailFactory.create(
            ErrorCode.VALIDATION_FAILED,
            "요청값을 확인해 주세요.",
            getRequestURI(request),
            getRequestId(request),
            apiFieldErrors);

    return new ResponseEntity<>(problemDetail, headers, problemDetail.getStatus());
  }

  private List<ApiFieldError> extractErrors(MethodArgumentNotValidException ex) {
    return ex.getBindingResult().getFieldErrors().stream()
        .map(fieldError -> new ApiFieldError(fieldError.getField(), fieldError.getDefaultMessage()))
        .toList();
  }

  private List<ApiFieldError> extractErrors(HandlerMethodValidationException ex) {
    return ex.getParameterValidationResults().stream()
        .flatMap(
            result -> {

              // @Valid DTO 내부 field validation
              if (result instanceof ParameterErrors errors) {
                return errors.getFieldErrors().stream()
                    .map(error -> new ApiFieldError(error.getField(), error.getDefaultMessage()));
              }

              // @RequestParam, @Min 등 validation
              String parameterName =
                  Objects.requireNonNullElse(
                      result.getMethodParameter().getParameterName(), "unknown");

              return result.getResolvableErrors().stream()
                  .map(error -> new ApiFieldError(parameterName, error.getDefaultMessage()));
            })
        .toList();
  }

  private String getRequestId(WebRequest request) {
    return (String) request.getAttribute("requestId", RequestAttributes.SCOPE_REQUEST);
  }

  private URI getRequestURI(WebRequest request) {

    ServletWebRequest servletWebRequest = (ServletWebRequest) request;
    return URI.create(servletWebRequest.getRequest().getRequestURI());
  }
}
