package com.hepsiburada.utils;

import com.hepsiburada.config.ConfigReader;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Beklenmedik anlarda cikan popup/ara katmanlarin (cerez bandi, bulten,
 * uygulama indirme vb.) merkezi yonetimi.
 *
 * Tasarim kararlari:
 *  - Her popup bir kayit (PopupRule) olarak tanimlanir; yeni popup cikarsa
 *    listeye TEK SATIR eklenir, akis kodu degismez (genisletilebilirlik).
 *  - dismissPopupsIfPresent() KISA ve exception firlatmayan kontroller yapar:
 *    ilk gecis findElements ile beklemesizdir; yalnizca "gec acilan" olarak
 *    isaretli popuplar icin config'teki kisa timeout kadar beklenir.
 *  - Popup kapatilamazsa test DUSURULMEZ (popup coğu zaman akisi engellemez);
 *    sadece stderr'e not dusulur. Akisi gercekten engelleyen bir eleman varsa
 *    zaten asil adimin explicit wait'i okunur sekilde patlar.
 *  - Tarayici bildirim izni popup'i burada YOK: o Chrome'un kendi UI'si,
 *    DOM disidir; DriverFactory'deki ChromeOptions ile kaynaginda kapatildi.
 *
 * NOT: Bot-engel/CAPTCHA bir "popup" DEGILDIR ve burada kapatilmaz;
 * o durum CaptchaOrBlockDetector'un isidir (tespit + acik fail).
 */
public final class PopupHandler {

    /**
     * @param name        rapor/log icin okunur ad
     * @param closeButton popup'i kapatan elemanin locator'i
     * @param waitShortly true ise eleman hemen DOM'da olmasa bile kisa sure beklenir
     *                    (sayfa yuklendikten 1-2 sn sonra acilan popuplar icin)
     */
    private record PopupRule(String name, By closeButton, boolean waitShortly) {
    }

    /**
     * Kapatilacak popuplarin kayit listesi (genisletme noktasi: yeni bir
     * popup turu cikarsa buraya tek satir eklenir, akis kodu degismez).
     *
     * Su an bos cunku sahada karsilasilan tek katman cerez bandi ve o,
     * shadow DOM icinde oldugundan asagidaki dismissEfilliConsentIfPresent()
     * ozel isleyicisiyle kapatiliyor; XPath/CSS kurallari ona erisemiyor.
     * (2026-07-06 kosularinda bulten/uygulama-banner popuplarina hic
     * rastlanmadi; rastlanirsa dogrulanmis locator ile kayit eklenecek.)
     */
    private static final List<PopupRule> POPUP_RULES = List.of();

    private PopupHandler() {
    }

    /**
     * Kayitli tum popuplari kontrol eder, gorunur olanlari kapatir.
     * Her buyuk adimin oncesinde/sonrasinda guvenle cagrilabilir;
     * popup yoksa maliyeti findElements taramasi kadardir (~0).
     *
     * @return kapatilan popup adlari (log/rapor icin)
     */
    public static List<String> dismissPopupsIfPresent(WebDriver driver) {
        Duration shortWait = Duration.ofSeconds(ConfigReader.getInt("popup.check.timeout.seconds"));
        Waits waits = new Waits(driver);
        List<String> dismissed = new ArrayList<>();

        // Sayfa gecislerinde kaybolan bilgilendirme bandini yeniden enjekte et
        // (bu metot her kritik adimda cagrildigi icin dogal yeniden-cizim noktasi).
        TestRunBanner.show(driver);

        if (dismissEfilliConsentIfPresent(driver)) {
            dismissed.add("Cerez onayi (efilli, shadow DOM)");
        }

        for (PopupRule rule : POPUP_RULES) {
            try {
                if (isPresent(driver, waits, rule, shortWait)) {
                    WebElement button = waits.clickable(rule.closeButton());
                    button.click();
                    waits.invisible(rule.closeButton()); // kapandigini teyit et
                    dismissed.add(rule.name());
                }
            } catch (RuntimeException e) {
                // Popup kapatma "best effort"tur; asil test adimini bloklamaz.
                System.err.println("[PopupHandler] '" + rule.name() + "' kapatilamadi: "
                        + e.getClass().getSimpleName());
            }
        }
        return dismissed;
    }

    /**
     * HB'nin cerez onay bandi <efilli-layout-dynamic> ozel elementi icinde ve
     * SHADOW DOM kullaniyor; XPath/CSS oraya erisemedigi icin kayit listesindeki
     * kural onu hic bulamiyordu (2026-07-06 kosusunda "Sepete Ekle" tiklamasini
     * bu bant kesti). Shadow root'lara JS ile inip "Kabul Et" butonuna tiklanir.
     */
    private static boolean dismissEfilliConsentIfPresent(WebDriver driver) {
        if (driver.findElements(By.tagName("efilli-layout-dynamic")).isEmpty()) {
            return false;
        }
        String deepClickScript =
                "const walk = (root) => {"
                        + "  for (const el of root.querySelectorAll('*')) {"
                        + "    if (el.tagName === 'BUTTON' && el.textContent.trim().toLocaleLowerCase('tr') === 'kabul et') {"
                        + "      el.click(); return true;"
                        + "    }"
                        + "    if (el.shadowRoot && walk(el.shadowRoot)) return true;"
                        + "  }"
                        + "  return false;"
                        + "};"
                        + "return walk(document);";
        Object clicked = ((JavascriptExecutor) driver).executeScript(deepClickScript);
        if (Boolean.TRUE.equals(clicked)) {
            return true;
        }
        // Son care: buton shadow yapida bulunamadiysa bant DOM'dan kaldirilir.
        // Bu bir cerez ARAYUZU temizligidir (tiklamalari kesmesin diye);
        // bot korumasi/CAPTCHA ile ilgisi yoktur, onlara dokunulmaz.
        ((JavascriptExecutor) driver).executeScript(
                "document.querySelectorAll('efilli-layout-dynamic').forEach(el => el.remove());");
        System.err.println("[PopupHandler] efilli 'Kabul Et' tiklanamadi; bant DOM'dan kaldirildi.");
        return true;
    }

    private static boolean isPresent(WebDriver driver, Waits waits, PopupRule rule, Duration shortWait) {
        if (!driver.findElements(rule.closeButton()).isEmpty()) {
            return true; // zaten DOM'da — beklemeye gerek yok
        }
        // Yalnizca gec acildigi bilinen popuplar icin kisa sure bekle;
        // digerlerinde beklemek her cagriya gereksiz gecikme ekler.
        return rule.waitShortly() && waits.isVisibleWithin(rule.closeButton(), shortWait);
    }
}
