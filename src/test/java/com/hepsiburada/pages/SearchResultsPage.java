package com.hepsiburada.pages;

import com.hepsiburada.utils.ElementRepository;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.InvalidElementStateException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Arama ve sonuc grid'i.
 *
 * "Ikinci satirdaki ilk urun" secimi kolon sayisi VARSAYILMADAN, kartlarin
 * ekrandaki Y koordinatindan tespit edilir (bkz. getSecondRowFirstVisibleProductCard):
 * boylece cozunurluk/kolon degisse de dogru kart secilir. Yalnizca gorunur ve
 * icinde urun linki olan kartlar sayilir; dinamik class adlari kullanilmaz.
 */
public class SearchResultsPage extends BasePage {

    // Locator'lar element-infos/Arama.json deposunda.
    private static final By SEARCH_INPUT = ElementRepository.by("txt_search");
    private static final By PRODUCT_CARDS = ElementRepository.by("card_search_product");
    private static final By PRODUCT_GRID_CONTAINER = ElementRepository.by("grid_search_results");
    private static final By PRODUCT_LINK_IN_CARD = ElementRepository.by("link_product_in_card");

    /** Grid dolana kadar beklenecek asgari gorunur kart sayisi. */
    private static final int MIN_VISIBLE_CARDS = 5;
    /** Ayni satirdaki kartlarin Y koordinati arasindaki tolerans (px). */
    private static final int ROW_Y_TOLERANCE_PX = 10;

    public SearchResultsPage(WebDriver driver) {
        super(driver);
    }

    /** Arama kutusuna yazar ve Enter ile aramayi tetikler. */
    public void searchProduct(String keyword) {
        dismissPopups();
        // Arama input'u once "initialComponent" adinda pasif bir kopya olarak
        // gelir; tiklama/odak sonrasi HB onu GERCEK input ile degistirir
        // (2026-07-06 kosularinda once ElementClickIntercepted, sonra
        // StaleElementReference bu yuzden goruldu). Bu nedenle: aktive et,
        // elemani YENIDEN bul, sonra yaz.
        WebElement initialBox = waits.visible(SEARCH_INPUT);
        try {
            initialBox.click();
        } catch (ElementClickInterceptedException e) {
            ((JavascriptExecutor) driver).executeScript("arguments[0].focus();", initialBox);
        } catch (StaleElementReferenceException e) {
            // Aktivasyon tiklama aninda gerceklesti; asagida zaten yeniden bulunacak.
        }
        typeIntoSearchBoxWithStaleRetry(keyword);
        dismissPopups(); // sonuc sayfasinda da popup cikabiliyor
    }

    /**
     * Arama bileseni aktivasyon/yeniden cizim sirasinda birkac kez degistirilebiliyor;
     * stale yakalanirsa TAZE elemanla bastan yazilir (HB swap'i alani bos baslatiyor).
     */
    private void typeIntoSearchBoxWithStaleRetry(String keyword) {
        final int maxAttempts = 4;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                // Ayni locator birden fazla input'a uyabilir (pasif kopya + overlay);
                // her denemede GORUNUR ve ETKIN olan aday acikca secilir.
                WebElement searchBox = activeSearchBox();
                searchBox.sendKeys(keyword);
                // Yazi gercekten kutuya ulasti mi? (Swap aninda eski elemana
                // gidebiliyor; 2026-07-06 kosusunda bos kutuya Enter basilmisti.)
                WebElement freshBox = activeSearchBox();
                String value = String.valueOf(freshBox.getAttribute("value"));
                if (value.contains(keyword)) {
                    freshBox.sendKeys(Keys.ENTER);
                    return;
                }
                // Deger yerlesmemis: temizleyip bastan dene.
                freshBox.clear();
            } catch (StaleElementReferenceException
                     | InvalidElementStateException e) {
                // InvalidElementState, ElementNotInteractable'i da kapsar (ust sinifi).
                // Gecici swap belirtileri: yeniden dene. Baska her hata aninda firlar.
                if (attempt == maxAttempts) {
                    throw e;
                }
            }
        }
        throw new IllegalStateException(
                "Arama kutusuna '" + keyword + "' " + maxAttempts + " denemede yazilamadi; "
                        + "arama bileseni beklenenden farkli davraniyor (screenshots/ kontrol edin).");
    }

    /** Locator'a uyan, su an GORUNUR ve ETKIN olan arama input'unu dondurur. */
    private WebElement activeSearchBox() {
        return waits.until(d -> d.findElements(SEARCH_INPUT).stream()
                .filter(el -> el.isDisplayed() && el.isEnabled())
                .findFirst().orElse(null));
    }

    /** Sonuc grid'i gorunur mu? (assertion step katmaninda yapilir) */
    public boolean isProductListDisplayed() {
        return waits.isVisible(PRODUCT_GRID_CONTAINER);
    }

    /** 2. satir 1. urunun adini, urune TIKLAMADAN, linkin title'indan dondurur. */
    public String getSecondRowFirstProductName() {
        WebElement card = getSecondRowFirstVisibleProductCard();
        return card.findElement(PRODUCT_LINK_IN_CARD).getAttribute("title").trim();
    }

    /**
     * 2. satir 1. urunun linkine tiklar. Urun yeni sekmede acilirsa
     * driver o sekmeye gecirilir; aksi halde ayni sekmede devam edilir.
     */
    public void clickSecondRowFirstProduct() {
        WebElement link = getSecondRowFirstVisibleProductCard().findElement(PRODUCT_LINK_IN_CARD);
        scrollIntoCenter(link);
        Set<String> handlesBeforeClick = driver.getWindowHandles();
        waits.until(d -> link.isDisplayed() && link.isEnabled() ? link : null).click();
        switchToNewTabIfOpened(handlesBeforeClick);
    }

    private void switchToNewTabIfOpened(Set<String> handlesBeforeClick) {
        try {
            waits.until(d -> d.getWindowHandles().size() > handlesBeforeClick.size() ? Boolean.TRUE : null);
        } catch (TimeoutException e) {
            return; // yeni sekme acilmadi — ayni sekmede yonlenmisiz, sorun yok
        }
        Set<String> newHandles = new HashSet<>(driver.getWindowHandles());
        newHandles.removeAll(handlesBeforeClick);
        driver.switchTo().window(newHandles.iterator().next());
    }

    /**
     * Hedef karti bulan TEK yer (ad okuma ve tiklama ayni karti kullansin diye).
     *
     * Satirlar sabit kolon sayisi VARSAYILMADAN, kartlarin ekrandaki Y
     * koordinatindan tespit edilir: DOM sirasi satir-oncelikli oldugundan,
     * ilk kartin satirindan asagida baslayan ILK kart = 2. satirin 1. urunu.
     * Boylece cozunurluk/kolon sayisi degisse bile dogru kart secilir
     * (gorev dokumanindaki "listeyi satir satir degerlendir" adimina birebir).
     */
    private WebElement getSecondRowFirstVisibleProductCard() {
        // Grid dolana kadar bekle (lazy-load'a karsi asgari kart sayisi).
        try {
            waits.until(d -> visibleCards().size() >= MIN_VISIBLE_CARDS);
        } catch (TimeoutException e) {
            // Asagida ayrintili mesajla raporlanacak.
        }
        List<WebElement> cards = visibleCards();
        if (cards.size() < MIN_VISIBLE_CARDS) {
            throw new IllegalStateException(
                    "2. satirdaki ilk urun secilemedi: gorunur urun karti sayisi "
                            + cards.size() + " (< " + MIN_VISIBLE_CARDS + "). "
                            + "Arama sonucu bos/az olabilir ya da grid yapisi degismis olabilir.");
        }
        int firstRowY = cards.get(0).getLocation().getY();
        for (WebElement card : cards) {
            if (card.getLocation().getY() > firstRowY + ROW_Y_TOLERANCE_PX) {
                return card; // ilk satirdan asagida baslayan ilk kart
            }
        }
        throw new IllegalStateException(
                "Ikinci satir bulunamadi: " + cards.size()
                        + " gorunur kartin tamami ayni satirda gorunuyor. "
                        + "Grid yerlesimi degismis olabilir; screenshots/ klasorunu kontrol edin.");
    }

    /**
     * Yalnizca GORUNUR ve icinde URUN LINKI olan kartlar sayilir; grid'e
     * araya giren reklam/banner kartlari (linki olmayan li'ler) satir
     * hesabini bozmasin diye elenir (2026-07-06 kosusunda gozlendi).
     */
    private List<WebElement> visibleCards() {
        return driver.findElements(PRODUCT_CARDS).stream()
                .filter(WebElement::isDisplayed)
                .filter(card -> !card.findElements(PRODUCT_LINK_IN_CARD).isEmpty())
                .toList();
    }
}
