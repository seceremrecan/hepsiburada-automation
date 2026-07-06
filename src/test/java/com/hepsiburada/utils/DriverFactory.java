package com.hepsiburada.utils;

import com.hepsiburada.config.ConfigReader;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * WebDriver yasam dongusu: olusturma / erisim / kapatma.
 *
 * ThreadLocal kullaniliyor cunku Gauge senaryolari paralel kosuldugunda
 * her thread kendi driver'ina sahip olmali; statik tek instance yaris
 * kosullarina yol acar.
 *
 * Implicit wait BILEREK tanimlanmiyor: implicit + explicit wait karisimi
 * ongorulemeyen toplam beklemelere neden olur. Tum beklemeler Waits
 * sinifindaki WebDriverWait uzerinden yapilir.
 */
public final class DriverFactory {

    private static final ThreadLocal<WebDriver> DRIVER = new ThreadLocal<>();

    private DriverFactory() {
    }

    public static void initDriver() {
        if (DRIVER.get() != null) {
            return; // ayni senaryoda cift init'i sessizce yoksay
        }
        String browser = ConfigReader.getRequired("browser").toLowerCase(Locale.ROOT);
        WebDriver driver;
        switch (browser) {
            case "chrome":
                WebDriverManager.chromedriver().setup();
                driver = new ChromeDriver(buildChromeOptions());
                break;
            default:
                throw new IllegalArgumentException("Desteklenmeyen tarayici: " + browser);
        }
        driver.manage().timeouts()
                .pageLoadTimeout(Duration.ofSeconds(ConfigReader.getInt("timeout.pageload.seconds")));
        DRIVER.set(driver);
    }

    private static ChromeOptions buildChromeOptions() {
        ChromeOptions options = new ChromeOptions();

        // Acilis argumanlari browser-options.yaml'dan okunur (referans mimari):
        // yeni arguman eklemek icin kod derlemek gerekmez.
        loadBrowserArgs().forEach(options::addArguments);

        options.setExperimentalOption("useAutomationExtension", false);
        // showAutomationInfobar=true (qa-web.yaml) ise Chrome'un yerli
        // "otomatik test yazilimi tarafindan kontrol ediliyor" cubugu GORUNUR;
        // false ise eski davranis (cubuk gizli, bot izi azaltilmis) korunur.
        if (!Boolean.parseBoolean(String.valueOf(ConfigReader.get("show.automation.infobar")))) {
            options.setExperimentalOption("excludeSwitches", List.of("enable-automation"));
        }
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("profile.default_content_setting_values.notifications", 2); // 2 = engelle
        prefs.put("credentials_enable_service", false);            // "sifreyi kaydet?" balonu
        prefs.put("profile.password_manager_enabled", false);
        options.setExperimentalOption("prefs", prefs);

        // (--start-maximized, --lang vb. argumanlar browser-options.yaml'a tasindi)

        // UA sadece config'te ACIKCA verilmisse ezilir. Varsayilan bos:
        // sahte UA, gercek tarayici parmak iziyle celisirse tespiti kolaylastirir.
        String customUserAgent = ConfigReader.get("custom.user.agent");
        if (customUserAgent != null && !customUserAgent.isBlank()) {
            options.addArguments("--user-agent=" + customUserAgent);
        }

        // Kalici profil (SMS/OTP tekrarini onler): dolu ise ayni klasor her
        // kosuda kullanilir, site cihazi tanir. Goreli yol mutlaklastirilir.
        String profileDir = ConfigReader.get("chrome.profile.dir");
        if (profileDir != null && !profileDir.isBlank()) {
            java.nio.file.Path profilePath = java.nio.file.Path.of(profileDir).toAbsolutePath();
            try {
                java.nio.file.Files.createDirectories(profilePath);
                options.addArguments("--user-data-dir=" + profilePath);
            } catch (java.io.IOException e) {
                System.err.println("[DriverFactory] Profil klasoru olusturulamadi ("
                        + profilePath + "); sifir profille devam ediliyor: " + e.getMessage());
            }
        }

        if (ConfigReader.getBoolean("headless")) {
            options.addArguments("--headless=new");
        }
        return options;
    }

    /** browser-options.yaml -> chrome.args listesi (dosya/anahtar yoksa bos liste). */
    @SuppressWarnings("unchecked")
    private static List<String> loadBrowserArgs() {
        java.nio.file.Path file = java.nio.file.Path.of("src", "test", "resources", "browser-options.yaml");
        if (!java.nio.file.Files.exists(file)) {
            return List.of();
        }
        try (java.io.InputStream in = java.nio.file.Files.newInputStream(file)) {
            Map<String, Object> yaml = new org.yaml.snakeyaml.Yaml().load(in);
            if (yaml == null || !(yaml.get("chrome") instanceof Map<?, ?> chrome)) {
                return List.of();
            }
            Object args = ((Map<String, Object>) chrome).get("args");
            return args instanceof List<?> list
                    ? list.stream().map(String::valueOf).toList()
                    : List.of();
        } catch (java.io.IOException e) {
            throw new IllegalStateException("browser-options.yaml okunamadi: " + file.toAbsolutePath(), e);
        }
    }

    public static WebDriver getDriver() {
        WebDriver driver = DRIVER.get();
        if (driver == null) {
            throw new IllegalStateException("Driver baslatilmamis. Once DriverFactory.initDriver() cagrilmali "
                    + "(normalde ExecutionHooks bunu senaryo basinda yapar).");
        }
        return driver;
    }

    public static void quitDriver() {
        WebDriver driver = DRIVER.get();
        if (driver != null) {
            driver.quit();
            DRIVER.remove();
        }
    }
}
