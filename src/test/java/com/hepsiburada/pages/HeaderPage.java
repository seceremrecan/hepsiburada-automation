package com.hepsiburada.pages;

import com.hepsiburada.utils.ElementRepository;
import com.hepsiburada.utils.PopupHandler;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.WebDriver;

/**
 * Header uzerindeki, sayfadan bagimsiz navigasyon islemleri.
 * (Eski adi HomePage idi; sinif ana sayfayi degil header'i temsil ettigi
 * icin yeniden adlandirildi.) Login'e ozgu header islemleri LoginPage'te;
 * burada sepet navigasyonu ve sepet rozeti var.
 */
public class HeaderPage extends BasePage {

    // Locator'lar element-infos/Header.json deposunda.
    private static final By HEADER_CART_LINK = ElementRepository.by("link_header_cart");
    private static final By CART_ITEM_COUNT = ElementRepository.by("badge_cart_item_count");

    public HeaderPage(WebDriver driver) {
        super(driver);
    }

    /** Header'daki "Sepetim" baglantisiyla sepete gider. */
    public void goToCart() {
        dismissPopups();
        try {
            waits.clickable(HEADER_CART_LINK).click();
        } catch (ElementClickInterceptedException e) {
            // Acik kalmis bir modal/overlay tiklamayi kesti: overlay'i kaldirip
            // BIR kez daha dene (korleme tekrar degil, tek seferlik kurtarma).
            PopupHandler.removeBlockingCheckoutOverlay(driver);
            dismissPopups();
            waits.clickable(HEADER_CART_LINK).click();
        }
    }

    /** Header rozetindeki urun adedi metni (gorunmuyorsa bos string). */
    public String getCartItemCountText() {
        var badges = driver.findElements(CART_ITEM_COUNT);
        return badges.isEmpty() ? "" : badges.get(0).getText().trim();
    }
}
