package com.hepsiburada.utils;

import com.thoughtworks.gauge.AfterScenario;
import com.thoughtworks.gauge.AfterStep;
import com.thoughtworks.gauge.BeforeScenario;
import com.thoughtworks.gauge.BeforeStep;
import com.thoughtworks.gauge.BeforeSuite;
import com.thoughtworks.gauge.ExecutionContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * Gauge yasam dongusu kancalari — klasik JUnit projelerindeki BaseTest'in
 * karsiligi. Her senaryo temiz bir tarayici oturumuyla baslar ve biter;
 * senaryolar arasi durum sizintisi (cerez, sepet icerigi) olmaz.
 */
public class ExecutionHooks {

    /**
     * Her kosu basinda screenshots/ klasoru temizlenir; klasorde yalnizca
     * SON kosunun goruntuleri kalir (kullanici istegi: klasor temiz kalsin).
     */
    @BeforeSuite
    public void cleanScreenshotsFolder() {
        Path dir = Path.of("screenshots");
        if (!Files.isDirectory(dir)) {
            return;
        }
        try (Stream<Path> files = Files.list(dir)) {
            files.filter(Files::isRegularFile).forEach(f -> {
                try {
                    Files.delete(f);
                } catch (IOException e) {
                    System.err.println("[ExecutionHooks] Eski screenshot silinemedi: " + f);
                }
            });
        } catch (IOException e) {
            System.err.println("[ExecutionHooks] screenshots/ temizlenemedi: " + e.getMessage());
        }
    }

    @BeforeScenario
    public void setUp() {
        DriverFactory.initDriver();
    }

    // Adim yasam dongusu loglari (referans mimarideki
    // "... step started / step has been successfully completed" kalibi).
    // Konsola yazilir; HTML raporu sisirilmez.
    @BeforeStep
    public void beforeStep(ExecutionContext context) {
        StepLogger.console("step started: " + stepTextOf(context));
    }

    @AfterStep
    public void afterStep(ExecutionContext context) {
        boolean failed = context.getCurrentStep().getIsFailing();
        StepLogger.console("step " + (failed ? "FAILED" : "has been successfully completed")
                + ": " + stepTextOf(context));
    }

    private static String stepTextOf(ExecutionContext context) {
        try {
            return context.getCurrentStep().getText();
        } catch (RuntimeException e) {
            return "(adim metni okunamadi)";
        }
    }

    @AfterScenario
    public void tearDown(ExecutionContext context) {
        try {
            if (context.getCurrentScenario().getIsFailing()) {
                ScreenshotUtil.capture(DriverFactory.getDriver(),
                        "fail_" + sanitize(context.getCurrentScenario().getName()));
            }
        } catch (RuntimeException e) {
            // Screenshot/rapor sorunu driver'in kapanmasini asla engellememeli.
            System.err.println("[ExecutionHooks] Hata sonrasi screenshot alinamadi: " + e.getMessage());
        } finally {
            holdBrowserBeforeQuit();
            DriverFactory.quitDriver();
        }
    }

    /**
     * Senaryo bitince tarayiciyi hemen kapatmayip qa-web.yaml'daki
     * holdBrowserSeconds kadar acik tutar (demo/video icin son ekran gorunur
     * kalsin). Bu, kosul-tabanli bir bekleme DEGIL bilincli bir "ekrani tut"
     * duraklamasidir; o yuzden Waits yerine dogrudan burada uygulanir.
     */
    private void holdBrowserBeforeQuit() {
        String raw = com.hepsiburada.config.ConfigReader.get("hold.browser.seconds");
        int seconds;
        try {
            seconds = raw == null ? 0 : Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            seconds = 0;
        }
        if (seconds <= 0) {
            return;
        }
        StepLogger.console("Tarayici " + seconds + " sn acik tutuluyor (demo icin), sonra kapatilacak...");
        try {
            Thread.sleep(seconds * 1000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static String sanitize(String name) {
        return name == null ? "senaryo" : name.replaceAll("[^a-zA-Z0-9-_]", "_");
    }
}
