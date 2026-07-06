# Hepsiburada - Sepet Senaryolari

Hepsiburada web sitesinde sepet akislarini kapsayan senaryolar.
Is adimlari specs/Concepts/hepsiburada_tanimlar.cpt dosyasinda tanimlidir;
test verileri "Data_*" anahtarlariyla value-infos/<ortam>/values.json'dan cozulur.

## HB-TC01 - Login - Bilgisayar Ara - Ikinci Satirdaki Ilk Urunu Sepete Ekle ve Sepette Dogrula

tags: login, sepet, smoke

* Basariyla Email "Data_Email_User" ve Sifre "Data_Password" Ile Giris Yapilir
* Arama Kutusuna "bilgisayar" Yazilir ve Arama Baslatilir
* Arama Sonuclarinin "bilgisayar" Icin Yuklendigi Dogrulanir
* Ikinci Satirdaki Ilk Urun Secilir ve Urun Sayfasina Gidilir
* Urun Sepete Eklenir ve Onay Mesaji Dogrulanir
* Sepete Gidilir ve Urunun Sepette Oldugu Dogrulanir
* Sepet Temizlenir
