package io.github.jo0yo0n.mypetmate.guardian.validation.activity;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = SearchingActivityRequiredFieldsValidator.class)
public @interface RequiresActivityFieldsWhenSearching {

  String message() default "유효한 활동 지역 또는 가능 시간을 입력해 주세요.";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
