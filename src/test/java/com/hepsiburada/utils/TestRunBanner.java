package com.hepsiburada.utils;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

/**
 * Sayfanin altina "bu sayfa otomatik test tarafindan kontrol ediliyor"
 * bandini enjekte eder (kullanicinin ekranda testi izlerken gormesi icin).
 *
 * Tasarim notlari:
 *  - pointer-events:none — bant TIKLAMALARI ASLA KESMEZ; efilli bandinda
 *    yasadigimiz interception sorununu kendi elimizle yaratmamak icin sart.
 *  - Idempotent: ayni sayfaya ikinci enjeksiyon hicbir sey yapmaz (id kontrolu).
 *  - Sayfa gecisinde DOM sifirlandigi icin dismissPopupsIfPresent uzerinden
 *    her kritik noktada yeniden enjekte edilir.
 */
public final class TestRunBanner {

    private static final String INJECT_SCRIPT =
            "if (!window.__qaBannerKeeper) {"
                    + "  const ensure = () => {"
                    + "    if (document.body && !document.getElementById('qa-test-banner')) {"
                    + "      const b = document.createElement('div');"
                    + "      b.id = 'qa-test-banner';"
                    + "      b.textContent = '\\u26A0 Bu sayfa otomatik test (HB-TC01) taraf\\u0131ndan kontrol edilmektedir';"
                    + "      b.style.cssText = 'position:fixed;top:0;left:0;width:100%;"
                    + "z-index:2147483647;background:#1a1a2e;color:#ffd166;text-align:center;"
                    + "font:600 14px/2 sans-serif;padding:2px;opacity:.92;pointer-events:none;';"
                    + "      document.body.appendChild(b);"
                    + "    }"
                    + "  };"
                    // Bekci: sayfa ici yeniden cizimlerde bant silinirse 500ms icinde
                    // geri eklenir; boylece bant SAYFA BOYUNCA surekli gorunur kalir.
                    + "  window.__qaBannerKeeper = setInterval(ensure, 500);"
                    + "  ensure();"
                    + "}";

    private TestRunBanner() {

    }

    public static void show(WebDriver driver) {
        try {
            ((JavascriptExecutor) driver).executeScript(INJECT_SCRIPT);
        } catch (RuntimeException e) {
            // Bant salt gorsel bilgilendirmedir; enjekte edilemezse test akisini etkilemez.
        }
    }
}
