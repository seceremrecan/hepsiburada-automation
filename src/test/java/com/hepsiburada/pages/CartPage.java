package com.hepsiburada.pages;

import com.hepsiburada.utils.ElementRepository;
import com.hepsiburada.utils.TextNormalizer;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.List;

/**
 * Sepet sayfasi.
 *
 * Sepette onceden baska urunler bulunabilir; bu yuzden hicbir index/ilk-urun
 * varsayimi yapilmaz, TUM kartlar dolasilip ada gore aranir.
 * UUID'li id'ler ve data-sku degerleri sabitlenmez.
 */
public class CartPage extends BasePage {

    // Locator'lar element-infos/Sepet.json deposunda.
    private static final By CART_ITEMS = ElementRepository.by("card_cart_item");
    /** Kart ICINDE relative aranir; tum sayfadan aranmaz. */
    private static final By ITEM_PRODUCT_LINK = ElementRepository.by("link_cart_item_product");

    public CartPage(WebDriver driver) {
        super(driver);
    }

    /**
     * Sepetteki tum urun adlari (rapor/hata mesaji icin de kullanisli).
     * Kartlarin yuklenmesi icin varsayilan timeout kadar beklenir ama BOS
     * sepet hata sayilmaz (temizlik akisi sonrasi sepet bos kalabilir).
     */
    public List<String> getCartProductNames() {
        dismissPopups();
        waits.isVisible(CART_ITEMS); // kartlar varsa yuklenmesini bekle; yoksa bos liste
        List<String> names = new ArrayList<>();
        for (WebElement item : driver.findElements(CART_ITEMS)) {
            String name = productNameOf(item);
            if (!name.isEmpty()) { // headerdaki kullanıcı adının validasyonu burada yapılyıor
                names.add(name);
            }
        }
        return names;
    }

    /**
     * Beklenen urun sepette var mi? Buyuk/kucuk harf (TR locale) ve fazla
     * bosluk farklari yok sayilarak kontrollu contains karsilastirmasi yapilir.
     * Assertion icermez; karar step katmanindadir.
     */
    public boolean isProductInCart(String expectedProductName) {
        return getCartProductNames().stream()
                .anyMatch(actual -> TextNormalizer.matchesLoosely(actual, expectedProductName));
    }

    /** Ayni kartta birden fazla urun linki olabilir; metni bos olmayan ilki secilir. */
    private String productNameOf(WebElement cartItem) {
        for (WebElement link : cartItem.findElements(ITEM_PRODUCT_LINK)) {
            String text = link.getText().trim();
            if (!text.isEmpty()) {
                return text;
            }
        }
        return "";
    }
}
