# HB-TC01 - Login, bilgisayar ara, ikinci satirdaki ilk urunu sepete ekle ve sepette oldugunu dogrula

Tags: hb-tc01, sepet, smoke

On kosullar:

- Kullanici aktif bir Hepsiburada hesabina sahip olmali; kimlik bilgileri
  env/secrets/credentials.properties dosyasinda (git'e girmez) tanimli olmali.
- Tarayici senaryo basinda otomatik acilir, sonunda otomatik kapanir (ExecutionHooks).

Adim eslemesi (gorev dokumani): 1→ana sayfa, 2→giris sayfasi, 3→login+dogrulama,
4→arama, 5→sonuc dogrulama, 6→urun belirleme, 7→urune tiklama, 8→sepete ekleme,
9→sepete gitme, 10→sepette dogrulama.

## Kullanici girisi yapip aramadan gelen urunu sepete ekler ve dogrular

* Ana sayfayi ac
* Giris yap sayfasina git
* Gecerli kullanici bilgileriyle giris yap
* Girisin basarili oldugunu dogrula
* Arama kutusuna "bilgisayar" yazip aramayi baslat
* Arama sonuclarinin "bilgisayar" icin yuklendigini dogrula
* Ikinci satirdaki ilk urunu belirle
* Belirlenen urune tikla ve urun detay sayfasina gidildigini dogrula
* Urunu sepete ekle ve eklendigine dair onay mesajini dogrula
* Header uzerinden sepete git
* Eklenen urunun sepette oldugunu dogrula
* Eklenen urunu sepetten sil ve sepetin temizlendigini dogrula
