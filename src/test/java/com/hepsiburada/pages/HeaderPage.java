package com.hepsiburada.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/**
 * Header uzerindeki, sayfadan bagimsiz navigasyon islemleri.
 * (Eski adi HomePage idi; sinif ana sayfayi degil header'i temsil ettigi
 * icin yeniden adlandirildi.) Login'e ozgu header islemleri LoginPage'te;
 * burada sepet navigasyonu ve sepet rozeti var.
 */
public class HeaderPage extends BasePage {

    /** URL'nin tamami bilerek hard-code edilmedi; sadece '/sepetim' parcasi kullanildi. */
    private static final By HEADER_CART_LINK = By.cssSelector("a[href*='/sepetim']");
    /** Header'daki sepet urun adedi rozeti (cifte-ekleme onleminde kullanilir). */
    private static final By CART_ITEM_COUNT = By.id("cartItemCount");

    public HeaderPage(WebDriver driver) {
        super(driver);
    }

    /** Header'daki "Sepetim" baglantisiyla sepete gider. */
    public void goToCart() {
        dismissPopups();
        waits.clickable(HEADER_CART_LINK).click();
    }

    /** Header rozetindeki urun adedi metni (gorunmuyorsa bos string). */
    public String getCartItemCountText() {
        var badges = driver.findElements(CART_ITEM_COUNT);
        return badges.isEmpty() ? "" : badges.get(0).getText().trim();
    }
}
