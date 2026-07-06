package com.hepsiburada.utils;

import com.thoughtworks.gauge.AfterScenario;
import com.thoughtworks.gauge.BeforeScenario;
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
            DriverFactory.quitDriver();
        }
    }

    private static String sanitize(String name) {
        return name == null ? "senaryo" : name.replaceAll("[^a-zA-Z0-9-_]", "_");
    }
}
