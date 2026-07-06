package com.hepsiburada.utils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.hepsiburada.config.ConfigReader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Test verisi cozumleyici — referans mimarideki "Data_Email_User" kalibi.
 *
 * Spec/concept'lerde gecen "Data_..." anahtarlari
 * src/test/resources/value-infos/<ortam>/values.json dosyasindan cozulur
 * (ortam: config 'env' degeri; test/prep/live klasorleri desteklenir).
 *
 * Deger "config:x.y" bicimindeyse ConfigReader'a delege edilir — boylece
 * SIFRELER values.json'a yazilmaz, gitignore'lu env/secrets katmaninda kalir.
 * "Data_" ile baslamayan degerler oldugu gibi (literal) doner.
 */
public final class ValueResolver {

    private static final String DATA_PREFIX = "Data_";
    private static final String CONFIG_PREFIX = "config:";

    private ValueResolver() {
    }

    public static String resolve(String raw) {
        if (raw == null || !raw.startsWith(DATA_PREFIX)) {
            return raw; // literal deger
        }
        String env = ConfigReader.get("env") == null ? "test" : ConfigReader.get("env");
        Path valuesFile = Path.of("src", "test", "resources", "value-infos", env, "values.json");
        Map<String, String> values = load(valuesFile);
        String value = values.get(raw);
        if (value == null) {
            throw new IllegalStateException(
                    "'" + raw + "' anahtari " + valuesFile + " icinde tanimli degil.");
        }
        if (value.startsWith(CONFIG_PREFIX)) {
            return ConfigReader.getRequired(value.substring(CONFIG_PREFIX.length()));
        }
        return value;
    }

    private static Map<String, String> load(Path valuesFile) {
        try {
            return new Gson().fromJson(
                    Files.readString(valuesFile, StandardCharsets.UTF_8),
                    new TypeToken<Map<String, String>>() { }.getType());
        } catch (IOException e) {
            throw new IllegalStateException("values.json okunamadi: " + valuesFile.toAbsolutePath(), e);
        }
    }
}
