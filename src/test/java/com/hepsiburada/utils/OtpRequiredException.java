package com.hepsiburada.utils;

import java.nio.file.Path;

/**
 * Site SMS/e-posta dogrulama kodu (OTP) istediginde firlatilir.
 *
 * Kod OKUNMAZ/otomatiklestirilmez (CAPTCHA ile ayni ilke: dogrulama
 * mekanizmalari asilmaz). Cozum yolu mesajin icinde aciklanir.
 */
public class OtpRequiredException extends RuntimeException {

    public OtpRequiredException(String context, Path screenshotPath) {
        super("SMS/OTP DOGRULAMASI ISTENDI [" + context + "] — otomasyon dogrulama kodunu "
                + "bilerek OKUMAZ (kapsam disi). Cozum: chrome.profile.dir ayari acikken "
                + "testi bir kez calistirip OTP ekrani geldiginde kodu ELLE girin; profil "
                + "cihazi hatirlayacagi icin sonraki kosularda kod istenmez. "
                + "Ekran goruntusu: " + (screenshotPath != null ? screenshotPath : "alinamadi"));
    }
}
