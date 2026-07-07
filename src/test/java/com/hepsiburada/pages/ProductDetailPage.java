package com.hepsiburada.pages;

import com.hepsiburada.utils.CaptchaOrBlockDetector;
import com.hepsiburada.utils.ElementRepository;
import com.hepsiburada.utils.PopupHandler;
import com.hepsiburada.utils.ScreenshotUtil;
import com.hepsiburada.utils.StepLogger;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import java.time.Duration;

/**
 * Urun detay sayfasi + sepete ekleme onay paneli.
 *
 * Ana akis: addToCartAndAwaitConfirmation() ile eklenir, onay paneli
 * "Alisverise devam et" ile kapatilir; sepete header'daki Sepetim baglantisiyla
 * (HeaderPage.goToCart) gidilir.
 */
public class ProductDetailPage extends BasePage {

    // Locator'lar element-infos/UrunDetay.json deposunda.
    private static final By PRODUCT_TITLE = ElementRepository.by("lbl_product_title");
    private static final By ADD_TO_CART_BUTTON = ElementRepository.by("btn_add_to_cart");
    private static final By CONFIRMATION_SUCCESS_MESSAGE = ElementRepository.by("lbl_added_confirmation");
    private static final By CONFIRMATION_CONTINUE_SHOPPING_BUTTON = ElementRepository.by("btn_confirmation_continue");
    private static final By RECOMMENDED_MODAL_MARKER = ElementRepository.by("lbl_recommended_modal_warning");
    private static final By RECOMMENDED_MODAL_ADD_TO_CART = ElementRepository.by("btn_recommended_modal_add");

    public ProductDetailPage(WebDriver driver) {
        super(driver);
    }

    /** Detay sayfasindaki urun basligi (dogru urune yonlendik mi kontrolu icin). */
    public String getProductTitle() {
        dismissPopups(); // yeni sekmede cerez bandi/duyurular yeniden gelebiliyor
        return waits.visible(PRODUCT_TITLE).getText().trim();
    }

    /** Ana "Sepete Ekle" butonuna tiklar (onay paneli ayri metotlarla dogrulanir). */
    public void addToCart() {
        dismissPopups();
        WebElement button = waits.clickable(ADD_TO_CART_BUTTON);
        scrollIntoCenter(button);
        button.click();
    }

    /**
     * Sepete ekler ve onayi bekler; panel gelmezse tiklama bir overlay'e
     * denk gelmis olabilir diye BIR kez daha dener. Ikinci deneme urunu
     * tekrar ekleyebilir; sepet dogrulamasi ada gore yapildigi icin senaryo
     * sonucunu etkilemez.
     */
    public boolean addToCartAndAwaitConfirmation() {
        String badgeBefore = new HeaderPage(driver).getCartItemCountText();
        addToCart();

        String outcome = awaitConfirmationOrRecommendedModal();
        if ("onay".equals(outcome)) {
            return true;
        }
        if ("modal".equals(outcome)) {
            // Modal aciksa ana butona KORLEME tekrar tiklanmaz; modalin icinden ilerlenir.
            return acceptRecommendedSellerOffer(badgeBefore);
        }

        // Panel/modal gorunmedi ama sepet rozeti arttiysa ekleme basarilidir;
        // ikinci tiklama yapilmaz (cifte ekleme onlemi).
        if (cartBadgeChanged(badgeBefore)) {
            StepLogger.log("Onay paneli gorunmedi ama sepet rozeti degisti; ekleme basarili sayildi.");
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
        // Onay gelmiyorsa sebep bot-engel/CAPTCHA olabilir; okunur sekilde raporla (bypass yok).
        CaptchaOrBlockDetector.checkAndFailIfBlocked(driver, "sepete-ekleme");
        return false;
    }

    /** "Ürün sepetinizde" onay mesaji goruldu mu? */
    public boolean isProductAddedConfirmationDisplayed() {
        return waits.isVisible(CONFIRMATION_SUCCESS_MESSAGE);
    }

    /**
     * Ana akis: paneli "Alisverise devam et" ile kapat, sepete header'dan gidilecek.
     * Ekleme onay paneli DISINDA bir yolla dogrulanmissa kapatilacak panel
     * olmayabilir; o durumda sessizce gecilir.
     */
    public void continueShoppingFromConfirmationPanel() {
        if (!waits.isVisibleWithin(CONFIRMATION_CONTINUE_SHOPPING_BUTTON, Duration.ofSeconds(3))) {
            return;
        }
        waits.clickable(CONFIRMATION_CONTINUE_SHOPPING_BUTTON).click();
        waits.invisible(CONFIRMATION_SUCCESS_MESSAGE); // panel kapandi mi teyit et
    }

    /** Tek beklemede iki olasi sonuc: "onay" paneli veya "modal"; ikisi de yoksa null. */
    private String awaitConfirmationOrRecommendedModal() {
        try {
            return waits.until(d -> {
                try {
                    if (isAnyDisplayed(CONFIRMATION_SUCCESS_MESSAGE)) {
                        return "onay";
                    }
                    if (isAnyDisplayed(RECOMMENDED_MODAL_MARKER)) {
                        return "modal";
                    }
                } catch (WebDriverException e) {
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
     * HICBIR durumda exception firlatmaz: buton yoksa screenshot + log birakip
     * false doner, nihai karari step katmanindaki assertion verir.
     */
    private boolean acceptRecommendedSellerOffer(String badgeBefore) {
        StepLogger.log("Onerilen satici modali acildi; modal icinden 'Sepete Ekle' denenecek.");
        if (!isAnyDisplayed(RECOMMENDED_MODAL_ADD_TO_CART)) {
            var screenshot = ScreenshotUtil.capture(driver, "onerilen_satici_modal");
            String warningText = driver.findElements(RECOMMENDED_MODAL_MARKER).stream()
                    .findFirst().map(WebElement::getText).orElse("(metin okunamadi)");
            StepLogger.log("UYARI: Onerilen satici modalinda 'Sepete Ekle' bulunamadi; "
                    + "test patlatilmiyor, log ile gecildi. Modal uyarisi: \"" + warningText
                    + "\". Screenshot: " + screenshot);
            new Actions(driver).sendKeys(Keys.ESCAPE).perform();
            return false;
        }
        waits.clickable(RECOMMENDED_MODAL_ADD_TO_CART).click();
        // Modal akisinda "urun sepetinizde" onay mesaji GELMEZ; onu 15sn beklemek
        // bosuna zaman kaybi. Ekleme kaniti olarak sepet rozetinin degismesini
        // (veya onay mesajini) kisa bir pencerede yoklariz; ikisinden biri
        // gorununce hemen doneriz.
        boolean added = awaitAddProof(badgeBefore, Duration.ofSeconds(5));
        // Bu modal ESC ile kapanmiyor; overlay'i hedefli kaldiririz — ekleme
        // kaniti yukarida alindigi icin guvenli. Aksi halde sonraki header
        // tiklamalarini keser.
        PopupHandler.removeBlockingCheckoutOverlay(driver);
        return added;
    }

    /**
     * Ekleme kanitini kisa bir pencerede bekler: sepet rozeti degisirse VEYA
     * onay mesaji gorunurse true; sure dolarsa false. Full 15sn timeout yerine
     * hizli, kanit gorununce aninda donen bekleme.
     */
    private boolean awaitAddProof(String badgeBefore, Duration timeout) {
        try {
            return waits.until(d ->
                    (cartBadgeChanged(badgeBefore) || isAnyDisplayed(CONFIRMATION_SUCCESS_MESSAGE))
                            ? Boolean.TRUE : null,
                    timeout);
        } catch (TimeoutException e) {
            return false;
        }
    }

    /** Sepet rozeti eklemeden bu yana degisti mi? (bos rozet degisim sayilmaz) */
    private boolean cartBadgeChanged(String badgeBefore) {
        String badgeAfter = new HeaderPage(driver).getCartItemCountText();
        return !badgeAfter.isEmpty() && !badgeAfter.equals(badgeBefore);
    }

    private boolean isAnyDisplayed(By locator) {
        return driver.findElements(locator).stream().anyMatch(WebElement::isDisplayed);
    }
}
