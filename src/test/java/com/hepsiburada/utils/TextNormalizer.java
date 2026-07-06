package com.hepsiburada.utils;

import java.util.Locale;

/**
 * Urun adi karsilastirmalarinda ortak normalizasyon:
 * TR locale lowercase + coklu boslugu tek bosluga indirme + trim.
 * (CartPage ve step katmani ayni kurali kullansin diye tek yerde.)
 */
public final class TextNormalizer {

    private static final Locale TR = Locale.forLanguageTag("tr-TR");

    private TextNormalizer() {
    }

    public static String normalize(String s) {
        if (s == null) {
            return "";
        }
        return s.toLowerCase(TR).replaceAll("\\s+", " ").trim();
    }

    /** Iki metinden biri digerini iceriyorsa true (normalize edilmis halleriyle). */
    public static boolean matchesLoosely(String a, String b) {
        String na = normalize(a);
        String nb = normalize(b);
        if (na.isEmpty() || nb.isEmpty()) {
            return false;
        }
        return na.contains(nb) || nb.contains(na);
    }
}
