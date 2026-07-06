package com.hepsiburada.utils;

import com.thoughtworks.gauge.Gauge;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Adim adim ilerleme logu: ayni mesaj hem KONSOLA (zaman damgali) hem de
 * Gauge HTML raporuna yazilir. Tek cagriyla iki hedef — cift satir yazma
 * derdi olmadan konsoldan canli izleme yapilabilir.
 */
public final class StepLogger {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private StepLogger() {
    }

    public static void log(String message) {
        System.out.println("[" + LocalTime.now().format(TS) + "] [HB-TC01] " + message);
        try {
            Gauge.writeMessage(message);
        } catch (RuntimeException | LinkageError e) {
            // Gauge baglami yoksa (or. duz JUnit kosusu) konsol logu yeterli.
        }
    }
}
