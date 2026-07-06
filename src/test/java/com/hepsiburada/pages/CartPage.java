package com.hepsiburada.pages;

import com.hepsiburada.utils.ElementRepository;
import com.hepsiburada.utils.TextNormalizer;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.time.Duration;
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
    private static final By ITEM_DELETE_BUTTON = ElementRepository.by("btn_cart_item_delete");
    private static final By DELETE_CONFIRM_BUTTON = ElementRepository.by("btn_delete_confirm");

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
            if (!name.isEmpty()) {
                names.add(name);
            }
        }
        return names;
    }

    /**
     * Ada gore eslesen TUM kartlari sepetten siler (onceki kosulardan kalan
     * kopyalar dahil) ve silinen adedi dondurur. Boylece her kosu, kendi
     * ekledigi urunu temizler; bir sonraki kosu temiz sepetle baslar.
     * Sepetteki BASKA urunlere dokunulmaz.
     */
    public int removeAllMatchingProducts(String expectedProductName) {
        dismissPopups();
        int removed = 0;
        final int maxRemovals = 10; // sonsuz donguye karsi guvenlik siniri
        while (removed < maxRemovals) {
            WebElement target = findFirstMatchingItem(expectedProductName);
            if (target == null) {
                break;
            }
            List<WebElement> deleteButtons = target.findElements(ITEM_DELETE_BUTTON);
            if (deleteButtons.isEmpty()) {
                throw new IllegalStateException(
                        "Sepet kartinda silme butonu bulunamadi; element-infos/Sepet.json'daki "
                                + "'btn_cart_item_delete' locator'i gercek DOM'a gore guncellenmeli.");
            }
            scrollIntoCenter(deleteButtons.get(0));
            deleteButtons.get(0).click();
            // Onay diyalogu cikarsa "Sil" ile onayla (kisa bekleme; cikmazsa gec).
            if (waits.isVisibleWithin(DELETE_CONFIRM_BUTTON, Duration.ofSeconds(3))) {
                waits.clickable(DELETE_CONFIRM_BUTTON).click();
            }
            // Kart DOM'dan dusene kadar bekle; sayaci ondan sonra artir.
            waits.until(ExpectedConditions.stalenessOf(target));
            removed++;
            dismissPopups(); // silme sonrasi cikabilecek onay/toast katmanlari
        }
        return removed;
    }

    private WebElement findFirstMatchingItem(String expectedProductName) {
        for (WebElement item : driver.findElements(CART_ITEMS)) {
            if (TextNormalizer.matchesLoosely(productNameOf(item), expectedProductName)) {
                return item;
            }
        }
        return null;
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
