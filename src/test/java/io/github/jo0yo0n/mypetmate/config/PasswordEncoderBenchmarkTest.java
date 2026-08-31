package io.github.jo0yo0n.mypetmate.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

@Tag("manual")
class PasswordEncoderBenchmarkTest {

  private static final int WARMUP_ITERATIONS = 3;
  private static final int MEASUREMENT_ITERATIONS = 100;

  @Test
  void measuresPasswordVerificationTime() {
    PasswordEncoder passwordEncoder = new PasswordConfig().passwordEncoder();
    String password = "StrongPass123!";
    String encodedPassword = passwordEncoder.encode(password);

    for (int index = 0; index < WARMUP_ITERATIONS; index++) {
      assertThat(passwordEncoder.matches(password, encodedPassword)).isTrue();
    }

    long[] elapsedNanos = new long[MEASUREMENT_ITERATIONS];
    for (int index = 0; index < MEASUREMENT_ITERATIONS; index++) {
      long startedAt = System.nanoTime();
      assertThat(passwordEncoder.matches(password, encodedPassword)).isTrue();
      elapsedNanos[index] = System.nanoTime() - startedAt;
    }

    Arrays.sort(elapsedNanos);
    int upperMiddle = MEASUREMENT_ITERATIONS / 2;
    int lowerMiddle = upperMiddle - 1;
    long medianNanos = (elapsedNanos[lowerMiddle] + elapsedNanos[upperMiddle]) / 2;
    long p95Nanos = elapsedNanos[(int) (MEASUREMENT_ITERATIONS * 0.95) - 1];

    System.out.printf(
        "BCrypt matches (%d samples): median=%d ms, p95=%d ms%n",
        MEASUREMENT_ITERATIONS, nanosToMillis(medianNanos), nanosToMillis(p95Nanos));
  }

  private long nanosToMillis(long nanos) {
    return nanos / 1_000_000;
  }
}
