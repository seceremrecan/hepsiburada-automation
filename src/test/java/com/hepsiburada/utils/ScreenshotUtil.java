package com.hepsiburada.utils;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Ekran goruntulerini timestamp'li dosya adiyla screenshots/ klasorune yazar.
 * Gauge'un kendi rapor klasoru her kosuda temizlendigi icin bilerek ayri,
 * kalici bir klasor kullaniliyor (klasor .gitignore'da).
 */
public final class ScreenshotUtil {

    private static final Path SCREENSHOT_DIR = Path.of("screenshots");
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");

    private ScreenshotUtil() {
    }

    /** @return kaydedilen dosyanin mutlak yolu; alinamazsa null (test akisini bozmaz). */
    public static Path capture(WebDriver driver, String label) {
        try {
            Files.createDirectories(SCREENSHOT_DIR);
            byte[] png = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
            Path target = SCREENSHOT_DIR.resolve(label + "_" + TS.format(LocalDateTime.now()) + ".png");
            Files.write(target, png);
            return target.toAbsolutePath();
        } catch (IOException | RuntimeException e) {
            // Screenshot alinamamasi testi dusurmemeli; asil hata zaten raporlanacak.
            System.err.println("[ScreenshotUtil] Ekran goruntusu alinamadi: " + e.getMessage());
            return null;
        }
    }
}
