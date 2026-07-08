package com.hepsiburada.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TextNormalizer birim testleri.
 *
 * Bu sinif tarayici gerektirmeyen SAF mantik tasir ve HB-TC01'in 3. ve 5.
 * assertion'lari (detay sayfasi basligi eslesmesi, urunun sepette bulunmasi)
 * dogrudan matchesLoosely'e dayandigi icin ayrica birim testiyle guvenceye
 * alinir. UI adimlari Gauge senaryosuyla uctan uca dogrulanir.
 */
class TextNormalizerTest {

    @Nested
    @DisplayName("normalize")
    class Normalize {

        @Test
        @DisplayName("null girdi bos string doner (NPE atmaz)")
        void nullReturnsEmpty() {
            assertThat(TextNormalizer.normalize(null)).isEmpty();
        }

        @Test
        @DisplayName("bastaki/sondaki bosluklari kirpar")
        void trimsSurroundingWhitespace() {
            assertThat(TextNormalizer.normalize("  MacBook  ")).isEqualTo("macbook");
        }

        @Test
        @DisplayName("coklu ic bosluklari tek bosluga indirir")
        void collapsesInnerWhitespace() {
            assertThat(TextNormalizer.normalize("Apple   MacBook\tAir\nM4"))
                    .isEqualTo("apple macbook air m4");
        }

        @Test
        @DisplayName("TR locale ile kucuk harfe cevirir")
        void lowercasesUsingTurkishLocale() {
            // TR locale'de 'I' -> 'ı', 'İ' -> 'i' olur; ekseni bu davranis.
            assertThat(TextNormalizer.normalize("BILGISAYAR")).isEqualTo("bılgısayar");
            assertThat(TextNormalizer.normalize("İyi")).isEqualTo("iyi");
        }
    }

    @Nested
    @DisplayName("matchesLoosely")
    class MatchesLoosely {

        @Test
        @DisplayName("bire bir ayni metinler eslesir")
        void identicalMatches() {
            assertThat(TextNormalizer.matchesLoosely("MacBook Air M4", "MacBook Air M4")).isTrue();
        }

        @Test
        @DisplayName("buyuk/kucuk harf ve bosluk farklari yok sayilir")
        void ignoresCaseAndWhitespace() {
            assertThat(TextNormalizer.matchesLoosely("  APPLE   MacBook  ", "apple macbook")).isTrue();
        }

        @Test
        @DisplayName("biri digerini iceriyorsa eslesir (her iki yon)")
        void substringMatchesBothDirections() {
            String full = "Apple MacBook Air M4 16GB 256GB SSD macOS 15\" Bilgisayar";
            String partial = "MacBook Air M4";
            assertThat(TextNormalizer.matchesLoosely(full, partial)).isTrue();
            assertThat(TextNormalizer.matchesLoosely(partial, full)).isTrue();
        }

        @Test
        @DisplayName("alakasiz metinler eslesmez")
        void unrelatedDoesNotMatch() {
            assertThat(TextNormalizer.matchesLoosely("MacBook Air", "Lenovo IdeaPad")).isFalse();
        }

        @Test
        @DisplayName("null veya bos girdi eslesmez")
        void nullOrEmptyDoesNotMatch() {
            assertThat(TextNormalizer.matchesLoosely(null, "MacBook")).isFalse();
            assertThat(TextNormalizer.matchesLoosely("MacBook", "")).isFalse();
            assertThat(TextNormalizer.matchesLoosely("   ", "MacBook")).isFalse();
        }
    }
}
