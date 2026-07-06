package com.hepsiburada.pages;

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

    private static final By CART_ITEMS =
            By.cssSelector("#onboarding_item_list li[data-listingid][data-sku]");
    /** Kart ICINDE relative aranir; tum sayfadan aranmaz. */
    private static final By ITEM_PRODUCT_LINK = By.cssSelector("a[href*='-p-']");
    /**
     * Kart icindeki silme aksiyonu. 2026-07-06 canli teshis kosusunda dogrulandi:
     * <a aria-label="Sepetten Çıkar" class="delete_button_...">. Yedek olarak
     * 'Sil' metin/etiket varyantlari ve delete_button class oneki de kapsanir.
     */
    private static final By ITEM_DELETE_BUTTON = By.xpath(
            ".//*[self::button or self::a]"
                    + "[@aria-label='Sepetten Çıkar'"
                    + " or contains(@aria-label,'Sil')"
                    + " or starts-with(@class,'delete_button')"
                    + " or normalize-space()='Sil']");
    /**
     * Silme tiklamasi sonrasi cikan onay diyalogundaki "Sil" butonu
     * (2026-07-06 kosusunun screenshot'indan dogrulandi: "Urunu sepetten
     * cikarmak istedigine emin misin?" + Sil / Vazgec).
     */
    private static final By DELETE_CONFIRM_BUTTON =
            By.xpath("//button[normalize-space()='Sil']");

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
                // Teshis: locator eslesmediyse karttaki TUM tiklanabilir ogeleri
                // ozetle loga bas; dogru secici buradan okunarak guncellenir.
                for (WebElement el : target.findElements(By.xpath(".//a | .//button"))) {
                    System.err.println("[CartPage][teshis] tag=" + el.getTagName()
                            + " aria-label=" + el.getAttribute("aria-label")
                            + " class=" + el.getAttribute("class")
                            + " data-test-id=" + el.getAttribute("data-test-id")
                            + " id=" + el.getAttribute("id")
                            + " text=" + el.getText().replaceAll("\\s+", " ").trim());
                }
                throw new IllegalStateException(
                        "Sepet kartinda 'Sil' butonu bulunamadi; ITEM_DELETE_BUTTON locator'i "
                                + "gercek DOM'a gore guncellenmeli (konsoldaki teshis satirlarina bakin).");
            }
            scrollIntoCenter(deleteButtons.get(0));
            deleteButtons.get(0).click();
            // Onay diyalogu cikarsa "Sil" ile onayla (kisa bekleme; cikmazsa gec).
            if (waits.isVisibleWithin(DELETE_CONFIRM_BUTTON, java.time.Duration.ofSeconds(3))) {
                waits.clickable(DELETE_CONFIRM_BUTTON).click();
            }
            // Kart DOM'dan dusene kadar bekle; sayaci ondan sonra artir.
            waits.until(org.openqa.selenium.support.ui.ExpectedConditions.stalenessOf(target));
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
