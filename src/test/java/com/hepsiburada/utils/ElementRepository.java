package com.hepsiburada.utils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.openqa.selenium.By;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Locator deposu: src/test/resources/element-infos altindaki TUM *.json
 * dosyalarini yukler ve key -> By donusumu sunar.
 *
 * Referans mimari (Virgosol/terabank): locator'lar Java'da degil, modul bazli
 * JSON dosyalarinda tutulur; kod yalnizca anahtar (txt_username, btn_login...)
 * bilir. Boylece locator degisikligi kod derlemesi gerektirmez ve tum
 * seciciler tek bakista denetlenebilir.
 */
public final class ElementRepository {

    private static final Path ELEMENT_INFOS_DIR = Path.of("src", "test", "resources", "element-infos");
    private static final Map<String, ElementInfo> ELEMENTS = loadAll();

    /** JSON semasi: key + type (css|xpath|id) + value (+ aciklama). */
    private static final class ElementInfo {
        String key;
        String type;
        String value;
        String description;
    }

    private ElementRepository() {
    }

    private static Map<String, ElementInfo> loadAll() {
        Map<String, ElementInfo> map = new HashMap<>();
        Gson gson = new Gson();
        try (Stream<Path> files = Files.list(ELEMENT_INFOS_DIR)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".json")).toList()) {
                List<ElementInfo> infos = gson.fromJson(
                        Files.readString(file, StandardCharsets.UTF_8),
                        new TypeToken<List<ElementInfo>>() { }.getType());
                for (ElementInfo info : infos) {
                    ElementInfo previous = map.put(info.key, info);
                    if (previous != null) {
                        throw new IllegalStateException(
                                "element-infos icinde cift tanimli key: '" + info.key + "' (" + file + ")");
                    }
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException(
                    "element-infos okunamadi: " + ELEMENT_INFOS_DIR.toAbsolutePath(), e);
        }
        if (map.isEmpty()) {
            throw new IllegalStateException("element-infos bos: " + ELEMENT_INFOS_DIR.toAbsolutePath());
        }
        return map;
    }

    /** key -> Selenium By. Tanimsiz key acik mesajla patlar (sessiz yanlislik olmaz). */
    public static By by(String key) {
        ElementInfo info = ELEMENTS.get(key);
        if (info == null) {
            throw new IllegalArgumentException(
                    "element-infos icinde tanimsiz element key'i: '" + key + "'. "
                            + "src/test/resources/element-infos altindaki JSON'lara ekleyin.");
        }
        return switch (info.type.toLowerCase(Locale.ROOT)) {
            case "css" -> By.cssSelector(info.value);
            case "xpath" -> By.xpath(info.value);
            case "id" -> By.id(info.value);
            default -> throw new IllegalArgumentException(
                    "Desteklenmeyen locator tipi '" + info.type + "' (key: " + key + ")");
        };
    }
}
