# RankingHolograms

FancyHolograms uzerinden calisan, otomatik guncellenen Top 10 siralama sistemi (SimpleClans entegrasyonlu). NPC kullanilmaz.

## Gereksinimler
Paper 1.21+, SimpleClans, FancyHolograms (v2 veya v3).

## Kullanim
Hologram satirina placeholder yaz:

```
/hologram edit ornek setline 1 &aEn cok oldurme #1 %killed1%
```

| Placeholder | Anlami |
|---|---|
| `%killed1%` - `%killed10%` | Oldurme siralamasi (oyuncu adi) |
| `%zaman1%` - `%zaman10%` | Sure siralamasi |
| `%olum1%` - `%olum10%` | Olum siralamasi |
| `%klan1%` - `%klan10%` | Klan siralamasi (klan adi) |
| `%killed1_deger%` vb. | Ayni siradaki degerin kendisi (kill, sure "Xs Ydk", olum, klan kill) |

- Renk kodlari (`&a`, `&c`, `&6`, `&l`, `&#RRGGBB`) ve MiniMessage calisir.
- Satirda sadece `%killed1%` yazmak yeterli, isim tek basina gelir.
- Her placeholder yalnizca kendi kategorisinin verisini okur.
- Bos siralarda `placeholders.empty-name` / `empty-value` gosterilir.
- Satiri placeholder'siz bir metinle degistirirsen sistem o satirdan elini cekmis olur.

## Komutlar
- `/siralama liste <killed|zaman|olum|klan>` - ilk 10'u sohbette gosterir
- `/siralama reload` - config'i yeniler

Yetkiler: `rankhologram.admin`, `rankhologram.reload`.

## Notlar
- Veritabani tablolari (`player_stats`, `clan_stats`) degismedi, mevcut veriler korunur. Eski `rank_bindings` tablosu artik kullanilmaz, silinmez.
- Satir sablonlari `hologram_templates` tablosunda saklanir; kapanista hologramlara sablon metni geri yazilir.
- Cevrimici oyuncularin suresi `hologram.time-flush-seconds` araligiyla siralamaya yansir.
