package com.hepsiburada.pages;

import com.hepsiburada.utils.PopupHandler;
import com.hepsiburada.utils.Waits;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Tum sayfa nesnelerinin ortak atasi.
 *
 * Sayfa nesnesi kurallari (proje sozlesmesi):
 *  - Sadece locator + aksiyon/sorgu metodu icerir; ASSERTION ICERMEZ.
 *    Dogrulamalar step katmanindadir (Faz 6). Boylece ayni sayfa nesnesi
 *    farkli senaryolarda farkli beklentilerle kullanilabilir.
 *  - Thread.sleep yasak; tum beklemeler Waits uzerinden kosulludur.
 *  - Driver, DriverFactory'den degil constructor'dan gelir: sayfa nesneleri
 *    statik duruma bagimli olmaz, test edilebilir kalir.
 */
public abstract class BasePage {

    protected final WebDriver driver;
    protected final Waits waits;

    protected BasePage(WebDriver driver) {
        this.driver = driver;
        this.waits = new Waits(driver);
        // Her sayfa nesnesi olusumunda bilgilendirme bandini tazele (idempotent):
        // kullanici ekrani izlerken testin kontrolde oldugunu her an gorsun.
        com.hepsiburada.utils.TestRunBanner.show(driver);
    }

    /** Kayitli popuplari kapatir; her buyuk aksiyondan once/sonra cagrilabilir. */
    protected void dismissPopups() {
        PopupHandler.dismissPopupsIfPresent(driver);
    }

    /**
     * Elemani ekranin ortasina kaydirir; alt/ust bantlara sabitlenen
     * katmanlarin tiklamayi kesmesini onler (click interception onlemi).
     */
    protected void scrollIntoCenter(WebElement element) {
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({block:'center'});", element);
    }
}
