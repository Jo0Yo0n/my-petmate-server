package io.github.jo0yo0n.mypetmate.guardian.validation.activity;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Constraint(validatedBy = NoOverlappingTimeSlotsValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface NoOverlappingTimeSlots {

  String message() default "같은 요일의 활동 시간대가 겹칩니다.";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
