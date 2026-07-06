package com.hepsiburada.pages;

import com.hepsiburada.utils.CaptchaOrBlockDetector;
import com.hepsiburada.utils.StepLogger;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;

/**
 * Urun detay sayfasi + sepete ekleme onay paneli.
 *
 * HB-TC01 ana akisinda panel "Alisverise devam et" ile kapatilir ve sepete
 * header'daki Sepetim baglantisiyla gidilir (HomePage.goToCart());
 * goToCartFromConfirmationPanel() yardimci olarak durur, ana akista kullanilmaz.
 */
public class ProductDetailPage extends BasePage {

    private static final By PRODUCT_TITLE =
            By.cssSelector("[data-test-id='title-area'] h1[data-test-id='title']");
    private static final By ADD_TO_CART_BUTTON =
            By.cssSelector("[data-test-id='add-to-cart'] button[data-test-id='addToCart']");

    // Onay paneli: id "AddToCart_<uuid>" — UUID sabitlenmedi, starts-with kullanildi.
    private static final By CONFIRMATION_SUCCESS_MESSAGE = By.xpath(
            "//div[starts-with(@id,'AddToCart_')]"
                    + "//span[normalize-space()='Ürün sepetinizde']");
    private static final By CONFIRMATION_PRODUCT_NAME = By.xpath(
            "//div[starts-with(@id,'AddToCart_')]"
                    + "//span[normalize-space()='Ürün sepetinizde']"
                    + "/following-sibling::h6");
    private static final By CONFIRMATION_GO_TO_CART_BUTTON = By.xpath(
            "//div[starts-with(@id,'AddToCart_')]"
                    + "//button[normalize-space()='Sepete git']");
    private static final By CONFIRMATION_CONTINUE_SHOPPING_BUTTON = By.xpath(
            "//div[starts-with(@id,'AddToCart_')]"
                    + "//button[normalize-space()='Alışverişe devam et']");

    // --- "Onerilen satici" modali (2026-07-06 screenshot'indan dogrulandi) ---
    // "Urunleriniz sectiginiz saticilardan tedarik edilemiyor..." uyarisini
    // tasiyan EN KUCUK (yaprak) eleman; dinamik checkoutui-* class'lari
    // bilerek kullanilmadi, metin capasi kullanildi.
    private static final String RECOMMENDED_MODAL_MARKER_XPATH =
            "//*[contains(normalize-space(.),'tedarik edilemiyor')"
                    + " and not(.//*[contains(normalize-space(.),'tedarik edilemiyor')])]";
    private static final By RECOMMENDED_MODAL_MARKER = By.xpath(RECOMMENDED_MODAL_MARKER_XPATH);
    /** Modal icindeki "Sepete Ekle": uyari metnine EN YAKIN ortak atadan GORELI aranir. */
    private static final By RECOMMENDED_MODAL_ADD_TO_CART = By.xpath(
            RECOMMENDED_MODAL_MARKER_XPATH
                    + "/ancestor::*[.//button[normalize-space()='Sepete Ekle']][1]"
                    + "//button[normalize-space()='Sepete Ekle']");

    public ProductDetailPage(WebDriver driver) {
        super(driver);
    }

    /** Detay sayfasindaki urun basligi (dogru urune yonlendik mi kontrolu icin). */
    public String getProductTitle() {
        dismissPopups(); // yeni sekmede cerez bandi/duyurular yeniden gelebiliyor
        return waits.visible(PRODUCT_TITLE).getText().trim();
    }

    public boolean isAddToCartButtonDisplayed() {
        return waits.isVisible(ADD_TO_CART_BUTTON);
    }

    /** Ana "Sepete Ekle" butonuna tiklar (onay paneli ayri metotlarla dogrulanir). */
    public void addToCart() {
        dismissPopups();
        var button = waits.clickable(ADD_TO_CART_BUTTON);
        scrollIntoCenter(button);
        button.click();
    }

    /**
     * Sepete ekler ve onayi bekler; panel gelmezse tiklama bir overlay'e
     * denk gelmis olabilir diye BIR kez daha dener (2026-07-06 kosusunda
     * ilk tiklamanin ardindan panelin gelmedigi gozlendi). Ikinci deneme
     * urunu muhtemelen tekrar ekler; sepet dogrulamasi ada gore yapildigi
     * icin senaryo sonucunu etkilemez.
     */
    public boolean addToCartAndAwaitConfirmation() {
        String badgeBefore = new HeaderPage(driver).getCartItemCountText();
        addToCart();

        String outcome = awaitConfirmationOrRecommendedModal();
        if ("onay".equals(outcome)) {
            return true;
        }
        if ("modal".equals(outcome)) {
            // KORLEME retry YOK: modal aciksa ana butona tekrar tiklanmaz,
            // modalin icinden ilerlenir.
            return acceptRecommendedSellerOffer(badgeBefore);
        }

        // Panel/modal gorunmedi ama header'daki sepet rozeti arttiysa ekleme
        // aslinda basarilidir; ikinci tiklama yapilmaz (cifte ekleme onlemi).
        String badgeAfter = new HeaderPage(driver).getCartItemCountText();
        if (!badgeAfter.isEmpty() && !badgeAfter.equals(badgeBefore)) {
            StepLogger.log("Onay paneli gorunmedi ama sepet rozeti "
                    + badgeBefore + " -> " + badgeAfter + " oldu; ekleme basarili sayildi.");
            return true;
        }

        StepLogger.log("Onay paneli ilk tiklamada gelmedi; bir kez daha deneniyor.");
        addToCart();
        outcome = awaitConfirmationOrRecommendedModal();
        if ("onay".equals(outcome)) {
            return true;
        }
        if ("modal".equals(outcome)) {
            return acceptRecommendedSellerOffer(badgeBefore);
        }
        // Okunur teshis: onay gelmiyorsa sebep bot-engel/CAPTCHA olabilir;
        // oyleyse BotDetectionException ile net rapor verilir (bypass yok).
        CaptchaOrBlockDetector.checkAndFailIfBlocked(driver, "sepete-ekleme");
        return false;
    }

    /**
     * Tek beklemede iki olasi sonucu yoklar: "onay" (Urun sepetinizde paneli)
     * veya "modal" (onerilen satici modali). Ikisi de gelmezse null.
     */
    private String awaitConfirmationOrRecommendedModal() {
        try {
            return waits.until(d -> {
                try {
                    if (d.findElements(CONFIRMATION_SUCCESS_MESSAGE).stream()
                            .anyMatch(org.openqa.selenium.WebElement::isDisplayed)) {
                        return "onay";
                    }
                    if (d.findElements(RECOMMENDED_MODAL_MARKER).stream()
                            .anyMatch(org.openqa.selenium.WebElement::isDisplayed)) {
                        return "modal";
                    }
                } catch (org.openqa.selenium.WebDriverException e) {
                    // Yeniden cizim/gecis ani: bir sonraki yoklamada tekrar bakilir.
                }
                return null;
            });
        } catch (TimeoutException e) {
            return null;
        }
    }

    /**
     * Onerilen satici modalindaki "Sepete Ekle" ile eklemeyi kabul eder.
     * Bu yol HICBIR durumda exception FIRLATMAZ (kullanici istegi):
     * buton yoksa screenshot + konsol logu birakilir, modal ESC ile kapatilir
     * ve false donulur; nihai karari step katmanindaki assertion verir.
     */
    private boolean acceptRecommendedSellerOffer(String badgeBefore) {
        StepLogger.log("Onerilen satici modali acildi; modal icinden 'Sepete Ekle' denenecek.");
        var modalButtons = driver.findElements(RECOMMENDED_MODAL_ADD_TO_CART);
        if (modalButtons.stream().noneMatch(org.openqa.selenium.WebElement::isDisplayed)) {
            var screenshot = com.hepsiburada.utils.ScreenshotUtil.capture(driver, "onerilen_satici_modal");
            String warningText = driver.findElements(RECOMMENDED_MODAL_MARKER).stream()
                    .findFirst().map(org.openqa.selenium.WebElement::getText).orElse("(metin okunamadi)");
            StepLogger.log("UYARI: Onerilen satici modalinda 'Sepete Ekle' butonu bulunamadi; "
                    + "test patlatilmiyor, log ile gecildi. Modal uyarisi: \"" + warningText
                    + "\". Screenshot: " + screenshot);
            // Modal acik kalmasin: ESC ile kapatmayi dene (bulunamazsa da zarar vermez).
            new org.openqa.selenium.interactions.Actions(driver)
                    .sendKeys(org.openqa.selenium.Keys.ESCAPE).perform();
            return false;
        }
        waits.clickable(RECOMMENDED_MODAL_ADD_TO_CART).click();
        // Basari kaniti: onay paneli VEYA sepet rozeti artisi — hangisi gelirse.
        if (isProductAddedConfirmationDisplayed()) {
            return true;
        }
        String badgeAfter = new HeaderPage(driver).getCartItemCountText();
        boolean badgeIncreased = !badgeAfter.isEmpty() && !badgeAfter.equals(badgeBefore);
        if (badgeIncreased) {
            StepLogger.log("Onerilen saticidan ekleme rozetle dogrulandi: "
                    + badgeBefore + " -> " + badgeAfter);
        }
        return badgeIncreased;
    }

    /** "Ürün sepetinizde" onay mesaji goruldu mu? */
    public boolean isProductAddedConfirmationDisplayed() {
        return waits.isVisible(CONFIRMATION_SUCCESS_MESSAGE);
    }

    /** Onay panelinde gosterilen urun adi. */
    public String getAddedProductName() {
        return waits.visible(CONFIRMATION_PRODUCT_NAME).getText().trim();
    }

    /** Yardimci: panel uzerinden dogrudan sepete git (ana akista KULLANILMAZ). */
    public void goToCartFromConfirmationPanel() {
        waits.clickable(CONFIRMATION_GO_TO_CART_BUTTON).click();
    }

    /**
     * Ana akis: paneli "Alisverise devam et" ile kapat, sepete header'dan gidilecek.
     * Ekleme onay paneli DISINDA bir yolla dogrulanmissa (sepet rozeti / onerilen
     * satici modali) kapatilacak panel olmayabilir — o durumda sessizce gecilir.
     */
    public void continueShoppingFromConfirmationPanel() {
        if (!waits.isVisibleWithin(CONFIRMATION_CONTINUE_SHOPPING_BUTTON, java.time.Duration.ofSeconds(3))) {
            return;
        }
        waits.clickable(CONFIRMATION_CONTINUE_SHOPPING_BUTTON).click();
        waits.invisible(CONFIRMATION_SUCCESS_MESSAGE); // panel kapandi mi teyit et
    }
}
