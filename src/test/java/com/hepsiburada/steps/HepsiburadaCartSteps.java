package com.hepsiburada.steps;

import com.hepsiburada.config.ConfigReader;
import com.hepsiburada.pages.CartPage;
import com.hepsiburada.pages.HeaderPage;
import com.hepsiburada.pages.LoginPage;
import com.hepsiburada.pages.ProductDetailPage;
import com.hepsiburada.pages.SearchResultsPage;
import com.hepsiburada.utils.CaptchaOrBlockDetector;
import com.hepsiburada.utils.DriverFactory;
import com.hepsiburada.utils.StepLogger;
import com.hepsiburada.utils.TextNormalizer;
import com.hepsiburada.utils.ValueResolver;
import com.thoughtworks.gauge.Step;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;

import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HB-TC01 spec adimlarinin implementasyonu.
 *
 * Gauge, spec'teki her satiri buradaki ayni metinli @Step metoduyla eslestirir.
 * Kural: aksiyonlar sayfa nesnelerinde, ASSERTION'lar (AssertJ) burada.
 * Gauge her senaryo icin bu sinifin yeni bir ornegini olusturur; adimlar arasi
 * tasinan durum (secilen urun adi) instance alaninda guvenle tutulur.
 *
 * Her adim StepLogger ile hem konsola (zaman damgali) hem Gauge raporuna
 * ilerleme yazar.
 */
public class HepsiburadaCartSteps {

    /** Adim 6'da belirlenen urunun adi; sonraki dogrulamalarda ve temizlikte kullanilir. */
    private String selectedProductName;

    private WebDriver driver() {
        return DriverFactory.getDriver();
    }

    @Step("Ana sayfayi ac")
    public void openHomePage() {
        StepLogger.log("ADIM 1: Ana sayfa aciliyor: " + ConfigReader.getRequired("app.base.url"));
        new LoginPage(driver()).openHomePage();
        StepLogger.log("ADIM 1: Ana sayfa acildi, popuplar temizlendi.");
    }

    @Step("Giris yap sayfasina git")
    public void goToLoginForm() {
        StepLogger.log("ADIM 2: Giris formuna gidiliyor (header hesap alani)...");
        new LoginPage(driver()).goToLoginForm();
        StepLogger.log("ADIM 2: Giris formu goruntulendi.");
    }

    @Step("Email <emailKey> ve sifre <passwordKey> ile giris yap")
    public void loginWithCredentialKeys(String emailKey, String passwordKey) {
        StepLogger.log("ADIM 3: Kimlik bilgileri '" + emailKey + "' / '" + passwordKey
                + "' anahtarlarindan cozulup giris yapiliyor (insan-gibi yazim)...");
        // Data_* anahtarlari value-infos uzerinden cozulur; sifrelerin kendisi
        // values.json'a DEGIL, gitignore'lu env/secrets katmanina isaret eder.
        String username = ValueResolver.resolve(emailKey);
        String password = ValueResolver.resolve(passwordKey);
        new LoginPage(driver()).loginWith(username, password);
        StepLogger.log("ADIM 3: Giris denemesi tamamlandi (bot-engel kontrolu yapildi).");
    }

    @Step("Girisin basarili oldugunu dogrula")
    public void verifyLoginSucceeded() {
        String displayedUsername;
        try {
            displayedUsername = new LoginPage(driver()).getDisplayedUsername();
        } catch (TimeoutException e) {
            // Kullanici adi gorunmediyse once neden'i okunur raporla:
            // OTP ekrani mi (elle bir kez kod girilmeli), bot-engel mi?
            CaptchaOrBlockDetector.checkForOtpChallenge(driver(), "login-dogrulama");
            CaptchaOrBlockDetector.checkAndFailIfBlocked(driver(), "login-dogrulama");
            throw e; // ikisi de degilse orijinal timeout mesajiyla fail et
        }
        // Assertion 1: login basarili — header'da kullanici adi gorunuyor.
        assertThat(displayedUsername)
                .as("Login sonrasi header'da kullanici adi gorunmeli")
                .isNotEmpty();
        StepLogger.log("ADIM 3d: [ASSERTION 1 OK] Giris dogrulandi, kullanici: " + displayedUsername);
    }

    @Step("Arama kutusuna <kelime> yazip aramayi baslat")
    public void searchFor(String kelime) {
        StepLogger.log("ADIM 4: Arama kutusuna '" + kelime + "' yazilip Enter'a basiliyor...");
        new SearchResultsPage(driver()).searchProduct(kelime);
        StepLogger.log("ADIM 4: Arama tetiklendi.");
    }

    @Step("Arama sonuclarinin <kelime> icin yuklendigini dogrula")
    public void verifySearchResultsLoaded(String kelime) {
        StepLogger.log("ADIM 5: Sonuc grid'inin yuklenmesi bekleniyor...");
        SearchResultsPage results = new SearchResultsPage(driver());
        boolean listDisplayed = results.isProductListDisplayed();
        if (!listDisplayed) {
            // Liste gelmediyse once bot-engel ihtimalini okunur sekilde ele:
            // engel varsa BotDetectionException, yoksa asagida normal assertion mesaji.
            CaptchaOrBlockDetector.checkAndFailIfBlocked(driver(), "arama-sonuclari");
        }
        // Assertion 2: sonuclar kullanici girdisine uygun geldi.
        assertThat(listDisplayed)
                .as("Urun listesi/grid'i gorunur olmali")
                .isTrue();
        assertThat(driver().getCurrentUrl().toLowerCase(Locale.forLanguageTag("tr-TR")))
                .as("Sonuc sayfasi URL'i arama terimini yansitmali")
                .contains(kelime.toLowerCase(Locale.forLanguageTag("tr-TR")));
        StepLogger.log("ADIM 5: [ASSERTION 2 OK] Sonuclar '" + kelime + "' icin yuklendi.");
    }

    @Step("Ikinci satirdaki ilk urunu belirle")
    public void identifySecondRowFirstProduct() {
        StepLogger.log("ADIM 6: Grid satir satir degerlendiriliyor (Y-koordinat tabanli)...");
        selectedProductName = new SearchResultsPage(driver()).getSecondRowFirstProductName();
        assertThat(selectedProductName)
                .as("2. satirdaki ilk urunun adi okunabilmeli")
                .isNotEmpty();
        StepLogger.log("ADIM 6: Hedef urun (2. satir 1. urun): " + selectedProductName);
    }

    @Step("Belirlenen urune tikla ve urun detay sayfasina gidildigini dogrula")
    public void clickSelectedProductAndVerifyDetailPage() {
        StepLogger.log("ADIM 7: Hedef urune tiklaniyor (yeni sekme acilirsa gecilecek)...");
        new SearchResultsPage(driver()).clickSecondRowFirstProduct();
        String detailTitle = new ProductDetailPage(driver()).getProductTitle();
        // Assertion 3: dogru urunun (2. satir 1.) sayfasina yonlendik.
        assertThat(TextNormalizer.matchesLoosely(detailTitle, selectedProductName))
                .as("Detay sayfasi basligi secilen urunle eslesmeli. Beklenen: '%s', Gorunen: '%s'",
                        selectedProductName, detailTitle)
                .isTrue();
        StepLogger.log("ADIM 7: [ASSERTION 3 OK] Detay sayfasi dogrulandi: " + detailTitle);
    }

    @Step("Urunu sepete ekle ve eklendigine dair onay mesajini dogrula")
    public void addToCartAndVerifyConfirmation() {
        StepLogger.log("ADIM 8: 'Sepete Ekle' tiklaniyor, onay paneli bekleniyor...");
        ProductDetailPage detail = new ProductDetailPage(driver());
        // Assertion 4: urun sepete eklendi (onay paneli goruldu / rozet artti).
        assertThat(detail.addToCartAndAwaitConfirmation())
                .as("'Urun sepetinizde' onay mesaji gorunmeli")
                .isTrue();
        StepLogger.log("ADIM 8: [ASSERTION 4 OK] Urun sepete eklendi.");
        // Ana akis: panel kapatilir, sepete header'dan gidilecek (PDF adim 9).
        detail.continueShoppingFromConfirmationPanel();
        StepLogger.log("ADIM 8: Onay paneli 'Alisverise devam et' ile kapatildi.");
    }

    @Step("Header uzerinden sepete git")
    public void goToCartViaHeader() {
        StepLogger.log("ADIM 9: Header'daki Sepetim baglantisina tiklaniyor...");
        new HeaderPage(driver()).goToCart();
        StepLogger.log("ADIM 9: Sepet sayfasi acildi.");
    }

    @Step("Eklenen urunun sepette oldugunu dogrula")
    public void verifyProductIsInCart() {
        StepLogger.log("ADIM 10: Sepet icerigi kontrol ediliyor...");
        CartPage cart = new CartPage(driver());
        List<String> cartNames = cart.getCartProductNames();
        // Assertion 5: eklenen urun sepette gorunuyor (ad/marka eslesmesi).
        assertThat(cart.isProductInCart(selectedProductName))
                .as("Urun sepette bulunamadi. Beklenen: '%s'. Sepettekiler: %s",
                        selectedProductName, cartNames)
                .isTrue();
        StepLogger.log("ADIM 10: [ASSERTION 5 OK] Urun sepette dogrulandi: " + selectedProductName);
    }
}
