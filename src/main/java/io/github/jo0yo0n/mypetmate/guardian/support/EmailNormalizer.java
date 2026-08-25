package io.github.jo0yo0n.mypetmate.guardian.support;

import java.util.Locale;

public final class EmailNormalizer {

  private EmailNormalizer() {}

  public static String normalize(String email) {
    if (email == null) {
      return null;
    }
    return email.trim().toLowerCase(Locale.ROOT);
  }
}
