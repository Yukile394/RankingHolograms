# RankingHolograms

Modern Display Entity (TextDisplay) tabanli, SimpleClans entegrasyonlu siralama hologram eklentisi. Paper 1.21.x ve sonrasi icin gelistirilmistir. ArmorStand kullanilmaz.

## Ozellikler

- Kill / Olum / Zaman / Klan Kill / Klan Olum / Haftalik Kill / Haftalik Olum siralamalari
- Tum leaderboardlar ayni simetrik hologram tasarim sistemini kullanir
- SQLite tabanli kalicilik (async okuma/yazma), ileride MySQL'e genisletilebilir soyutlama katmani
- Config uzerinden tamamen ozellestirilebilir renkler (MiniMessage) ve metinler
- Periyodik, entity yeniden olusturmadan yapilan hologram guncellemeleri (config: `hologram.update-interval`)
- Otomatik, tek seferlik haftalik reset sistemi (config: `weekly.*`)
- SimpleClans zorunlu bagimliliktir; sunucuda yoksa plugin yuklenmez

## Kurulum

1. `SimpleClans` eklentisini sunucunuza kurun.
2. Bu eklentinin derlenmis jar dosyasini `plugins/` klasorune atin.
3. Sunucuyu baslatin, `plugins/RankingHolograms/config.yml` dosyasini ihtiyaciniza gore duzenleyin.
4. `/siralama reload` ile config'i yeniden yukleyebilirsiniz.

## Komutlar

| Komut | ASCII | Aciklama | Izin |
|---|---|---|---|
| `/killsıralama` | `/killsiralama` | Bulundugunuz konuma kill siralamasi hologrami olusturur | `rankhologram.create` |
| `/ölümsıralama` | `/olumsiralama` | Olum siralamasi hologrami olusturur | `rankhologram.create` |
| `/zamansıralama` | `/zamansiralama` | Zaman siralamasi hologrami olusturur | `rankhologram.create` |
| `/klansıralama` | `/klansiralama` | Klan kill siralamasi hologrami olusturur | `rankhologram.create` |
| `/sıralamahologramayarla` | `/siralama` | Ana ayar menusu (5 kategori) ve hologram yonetimi | `rankhologram.admin` |

`/siralama` alt komutlari: `oyuncu`, `haftalik`, `zaman`, `klan`, `yonetim`, `create <tur>`, `remove <id>`, `reload`.

Kill veya Olum hologramlarina sag tiklamak (Interaction entity uzerinden) o hologrami toplam ve haftalik gorunum arasinda gecis yaptirir.

## Permissionlar

- `rankhologram.admin` - tum admin islemleri (default: op)
- `rankhologram.create` - hologram olusturma (default: op)
- `rankhologram.remove` - hologram silme (default: op)
- `rankhologram.reload` - config reload (default: op)

## Config

`config.yml` icinde `settings`, `hologram`, `weekly`, `time`, `database`, `colors` ve `messages` bolumleri bulunur. Tum hologram baslik metinleri, renkler ve mesajlar buradan degistirilebilir.

## Hologram olusturma sistemi

`/siralama` komutu bulundugunuz her konuma sadece Java kodundan degil, chat uzerindeki tiklanabilir menuden de yeni hologram olusturmaniza izin verir. Her hologram benzersiz bir UUID ile SQLite'a kaydedilir ve sunucu yeniden baslatildiginda otomatik olarak geri yuklenir. Bozuk/gecersiz bir kayit (silinmis dunya, parse edilemeyen tur) atlanir ve loglanir; diger hologramlarin yuklenmesini engellemez.

## Haftalik reset

Varsayilan olarak her Pazartesi 00:00'da sadece haftalik kill/olum sayaclari sifirlanir; toplam degerler ve online sure etkilenmez. Reset zamanini gecen server restartlarinda tekrar tetiklenmemesi icin son reset ISO hafta anahtari veritabaninda saklanir.

## Gradle build

```
./gradlew build
```

Cikti: `build/libs/RankingHolograms-<version>.jar`

> Not: Bu repository standart Gradle wrapper script'lerini (`gradlew`, `gradlew.bat`) icerir, fakat `gradle-wrapper.jar` binary dosyasi internet erisimi olmayan bir ortamda uretilemedi. Ilk klonlamadan sonra bir kere `gradle wrapper` komutunu calistirarak (sisteminizde Gradle kuruluysa) bu dosyayi olusturabilir, ya da dogrudan sistem Gradle'inizla `gradle build` calistirabilirsiniz. GitHub Actions workflow'u wrapper jar'ina ihtiyac duymadan `gradle/actions/setup-gradle` uzerinden calisir.

## GitHub Actions

`.github/workflows/build.yml` her push/PR'da Java 21 kurar, Gradle'i (wrapper'a gerek kalmadan) kurar, `gradle build shadowJar` calistirir ve olusan jar dosyasini artifact olarak yukler.

## Paper API notlari

- Hologramlar `TextDisplay` entity'si ile olusturulur, gorunurluk `view-distance` ile sinirlanir.
- Sag tiklama etkilesimi icin ayrica bir `Interaction` entity spawn edilir (KILL/DEATH/WEEKLY_* turleri icin).
- Klan siralamalari SimpleClans'in gercek `SimpleClans.getInstance().getClanManager()` API'si uzerinden hesaplanir.
