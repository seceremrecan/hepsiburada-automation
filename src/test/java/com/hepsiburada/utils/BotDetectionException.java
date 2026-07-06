package com.hepsiburada.utils;

import java.nio.file.Path;

/**
 * Site CAPTCHA / bot-engel sayfasi gosterdiginde firlatilir.
 *
 * Bu durum bir "test hatasi" degil, bilinen bir ortam kisitidir:
 * engel ASILMAZ, asilmaya calisilmaz; test okunur bir mesaj ve ekran
 * goruntusu yoluyla acikca fail eder (proje kurali).
 */
public class BotDetectionException extends RuntimeException {

    private final Path screenshotPath;

    public BotDetectionException(String context, String marker, Path screenshotPath) {
        super(buildMessage(context, marker, screenshotPath));
        this.screenshotPath = screenshotPath;
    }

    private static String buildMessage(String context, String marker, Path screenshotPath) {
        return "BOT KORUMASI TESPIT EDILDI [" + context + "] — site CAPTCHA/dogrulama sayfasi gosterdi. "
                + "Tetikleyen isaret: '" + marker + "'. "
                + "Engeli asmak bilerek denenmedi (kapsam disi). "
                + "Ekran goruntusu: " + (screenshotPath != null ? screenshotPath : "alinamadi") + ". "
                + "Oneri: testi farkli bir IP/zamanda tekrar kosun ya da manuel dogrulama sonrasi deneyin.";
    }

    public Path getScreenshotPath() {
        return screenshotPath;
    }
}
