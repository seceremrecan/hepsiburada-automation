# Hepsiburada - Sepet Senaryolari

Is adimlari (Turkce) specs/Concepts/hepsiburada_tanimlar.cpt icinde generic
keyword adimlarina acilir; generic adimlar StepImplementation'da uygulanir.
Gizli veriler (e-posta/sifre) "Data_*" anahtarlariyla value-infos'tan cozulur.

## HB-TC01 - Login, Bilgisayar Ara, Ikinci Satirdaki Ilk Urunu Sepete Ekle ve Dogrula

tags: login, sepet, smoke

* Ana Sayfaya Gidilir
* Basariyla Email "Data_Email_User" ve Sifre "Data_Password" Giris Yapilir
* "bilgisayar" Aramasi Yapilir
* Arama Sonuclarinin "bilgisayar" Icin Yuklendigi Dogrulanir
* Ikinci Satirdaki Ilk Urun Secilir ve Urun Sayfasina Gidilir
* Urun Sepete Eklenir ve Onay Dogrulanir
* Sepete Gidilir ve Urunun Sepette Oldugu Dogrulanir
