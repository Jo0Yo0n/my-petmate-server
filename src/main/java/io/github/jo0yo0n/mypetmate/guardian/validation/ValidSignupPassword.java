package io.github.jo0yo0n.mypetmate.guardian.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = SignupPasswordValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidSignupPassword {

  String message() default
      "영문 대문자, 영문 소문자, 숫자, 허용 특수문자(! @ # $ % ^ & *)를 각각 하나 이상 포함해야 하며, 다른 특수문자와 공백은 사용할 수 없습니다.";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
