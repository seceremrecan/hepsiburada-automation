package com.hepsiburada.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

/**
 * Tum konfigurasyonun tek giris noktasi.
 *
 * Okuma onceligi: JVM system property > ortam degiskeni > properties dosyasi.
 * Boylece CI uzerinde -Dbrowser=chrome ya da HB_USERNAME ortam degiskeni ile
 * dosyaya dokunmadan deger ezilebilir.
 *
 * Dosyalar Gauge'un env mekanizmasina birakilmadan dogrudan diskten okunur;
 * bu sayede ayni sinif duz JUnit/IDE calistirmalarinda da calisir.
 */
public final class ConfigReader {

    private static final Path DEFAULT_CONFIG = Path.of("env", "default", "default.properties");
    private static final Path CREDENTIALS_CONFIG = Path.of("env", "secrets", "credentials.properties");
    private static final Path QA_WEB_YAML = Path.of("src", "test", "resources", "qa-web.yaml");

    private static final Properties PROPS = loadAll();

    private ConfigReader() {
    }

    private static Properties loadAll() {
        Properties props = new Properties();
        loadInto(props, DEFAULT_CONFIG, true);
        // Kimlik bilgisi dosyasi lokalde kullanici tarafindan doldurulur;
        // CI'da yoksa degerler ortam degiskeninden gelebilir, o yuzden zorunlu degil.
        loadInto(props, CREDENTIALS_CONFIG, false);
        // qa-web.yaml (referans mimari) properties'i EZER; sifreler yaml'a girmez.
        overlayQaWebYaml(props);
        return props;
    }

    /**
     * qa-web.yaml anahtarlarini projenin kanonik anahtarlarina cevirip ekler:
     * browserType->browser, explicitTimeOut->timeout.explicit.seconds,
     * pageTimeOut->timeout.pageload.seconds, <env>Url->app.base.url ...
     */
    private static void overlayQaWebYaml(Properties props) {
        if (!Files.exists(QA_WEB_YAML)) {
            return; // yaml opsiyonel; eski properties duzeni tek basina da calisir
        }
        try (InputStream in = Files.newInputStream(QA_WEB_YAML)) {
            Map<String, Object> yaml = new org.yaml.snakeyaml.Yaml().load(in);
            if (yaml == null) {
                return;
            }
            putIfPresent(props, "browser", yaml.get("browserType"), true);
            putIfPresent(props, "headless", yaml.get("headless"), false);
            putIfPresent(props, "timeout.explicit.seconds", yaml.get("explicitTimeOut"), false);
            putIfPresent(props, "timeout.pageload.seconds", yaml.get("pageTimeOut"), false);
            putIfPresent(props, "env", yaml.get("env"), false);
            putIfPresent(props, "show.automation.infobar", yaml.get("showAutomationInfobar"), false);
            putIfPresent(props, "hold.browser.seconds", yaml.get("holdBrowserSeconds"), false);
            // Ortam URL secimi: env=test -> testUrl, prep -> prepUrl, live -> liveUrl
            String env = props.getProperty("env", "test");
            putIfPresent(props, "app.base.url", yaml.get(env + "Url"), false);
        } catch (IOException e) {
            throw new IllegalStateException("qa-web.yaml okunamadi: " + QA_WEB_YAML.toAbsolutePath(), e);
        }
    }

    private static void putIfPresent(Properties props, String key, Object value, boolean lowercase) {
        if (value != null && !String.valueOf(value).isBlank()) {
            String s = String.valueOf(value).trim();
            props.setProperty(key, lowercase ? s.toLowerCase(Locale.ROOT) : s);
        }
    }

    private static void loadInto(Properties props, Path file, boolean required) {
        if (!Files.exists(file)) {
            if (required) {
                throw new IllegalStateException("Zorunlu konfigurasyon dosyasi bulunamadi: " + file.toAbsolutePath());
            }
            return;
        }
        try (InputStream in = Files.newInputStream(file)) {
            props.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("Konfigurasyon dosyasi okunamadi: " + file.toAbsolutePath(), e);
        }
    }

    /** "app.base.url" -> once -Dapp.base.url, sonra APP_BASE_URL, sonra dosya. */
    public static String get(String key) {
        String fromSystem = System.getProperty(key);
        if (isNotBlank(fromSystem)) {
            return fromSystem.trim();
        }
        String envKey = key.replace('.', '_').toUpperCase(Locale.ROOT);
        String fromEnv = System.getenv(envKey);
        if (isNotBlank(fromEnv)) {
            return fromEnv.trim();
        }
        String fromFile = PROPS.getProperty(key);
        return fromFile == null ? null : fromFile.trim();
    }

    /** Degeri zorunlu kilar; eksikse testin NEDEN kosamadigini acikca soyler. */
    public static String getRequired(String key) {
        String value = get(key);
        if (!isNotBlank(value)) {
            throw new IllegalStateException(
                    "Zorunlu konfigurasyon degeri eksik: '" + key + "'. "
                            + "env/secrets/credentials.properties dosyasini doldurun ya da "
                            + key.replace('.', '_').toUpperCase(Locale.ROOT) + " ortam degiskenini tanimlayin.");
        }
        return value;
    }

    public static int getInt(String key) {
        return Integer.parseInt(getRequired(key));
    }

    public static boolean getBoolean(String key) {
        return Boolean.parseBoolean(getRequired(key));
    }

    private static boolean isNotBlank(String s) {
        return s != null && !s.isBlank();
    }
}
