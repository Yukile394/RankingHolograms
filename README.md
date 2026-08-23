# RankingHolograms

Citizens NPC'lerine bagli, SimpleClans entegrasyonlu siralama hologramlari.
Her hologram bir Citizens NPC'sinin uzerinde, NPC'nin isim (rename) yazisinin
oldugu yerde gorunur ve NPC'nin `/npc attribute scale` degeriyle birlikte
buyur/kucur.

## Bagimliliklar

- [Citizens](https://www.spigotmc.org/resources/citizens.13811/) (hard depend)
- [SimpleClans](https://www.spigotmc.org/resources/simpleclans.19630/) (hard depend)

Ikisi de sunucuda kurulu ve etkin olmadan plugin acilmaz.

## Nasil calisir

1. Citizens'in kendi komutuyla bir NPC olustur:
   ```
   /npc create <isim>
   ```
2. NPC'nin yanina git ve sec (Citizens'in kendi komutu):
   ```
   /npc select
   ```
   ya da yakinindaki bir NPC'yi otomatik secmek icin bu pluginin kisayolu:
   ```
   /npcselect
   ```
3. Secili NPC'ye istedigin siralama + rank'i bagla:
   ```
   /killsiralama1   Kill siralamasinda 1. kisi
   /killsiralama2   Kill siralamasinda 2. kisi
   /killsiralama3   Kill siralamasinda 3. kisi

   /olumsiralama1   Olum siralamasinda 1. kisi
   /olumsiralama2   Olum siralamasinda 2. kisi
   /olumsiralama3   Olum siralamasinda 3. kisi

   /zamansiralama1  Zaman siralamasinda 1. kisi
   /zamansiralama2  Zaman siralamasinda 2. kisi
   /zamansiralama3  Zaman siralamasinda 3. kisi

   /klansiralama1   Klan siralamasinda 1. klan
   /klansiralama2   Klan siralamasinda 2. klan
   /klansiralama3   Klan siralamasinda 3. klan
   ```

Her komut, o an secili olan NPC'ye ilgili siralama+rank hologramini baglar.
Bir NPC'ye ayni anda sadece bir hologram baglanabilir (son komut kazanir).
Her kategori (kill/zaman/olum/klan) kendi icinde top-3 gosterir; ayri NPC'ler
kullanarak istedigin kadar hologram olusturabilirsin.

Baglantiyi kaldirmak icin secili NPC'de:
```
/siralama remove
```

Config'i yeniden yuklemek icin:
```
/siralama reload
```

## Hologram gorunumu

Iki satir, NPC'nin tam ustunde, ortalanmis:

```
<isim> (#<rank>
<kategoriye ozel aciklama>
```

Ornek (kill siralamasi, 1. sira):
```
Yukile (#1
En cok oldurmeye sahip kisi
```

Renkler ve aciklama metinleri `config.yml` -> `colors` / `messages` altindan
degistirilebilir.

## Veri

- Kill/olum/oyun-suresi istatistikleri oyuncu PvP olumlerinden ve
  giris/cikis sureleri uzerinden otomatik toplanir (SQLite).
- Klan kill istatistikleri SimpleClans uzerinden otomatik toplanir.
- NPC <-> hologram baglantilari da ayni veritabaninda saklanir, sunucu
  yeniden baslatildiginda otomatik geri yuklenir.

## Build

```
./gradlew build
```

Citizens ve SimpleClans `compileOnly` bagimlilik olarak eklenmistir; jar'a
dahil edilmezler, sunucuda ayrica kurulu olmalari gerekir.
