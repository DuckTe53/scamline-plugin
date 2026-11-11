# Scam Line (Paper 1.21.11)

Sosyal aldatmaca mini oyunu. Oyuncular ayrı odalarda, eski telefon ve kafa ile iletişim kurar.

## Gereksinimler
- Paper 1.21.11
- Java 21
- SkinsRestorer (skin değişimi için, isteğe bağlı ama önerilir)

## Derleme
```
mvn package
```
Çıkan `target/ScamLine.jar` dosyasını sunucunun `plugins/` klasörüne koy.

## Kurulum
1. `plugins/ScamLine/config.yml` içinde `rooms` listesini 8 oda için düzenle. Format: `dünya,x,y,z,yaw`
2. `corridor` alanına ortak koridorun koordinatını yaz.
3. `frames` ayarlarında yeşil/kırmızı çerçevelerin oda merkezine göre konumunu ayarla.
4. Skin adlarını ve `skin-command` şablonunu kendi SkinsRestorer sürümüne göre kontrol et.

## Komutlar
| Komut | Açıklama |
|---|---|
| `/scamline baslat` | Oyunu başlatır, oyuncuları odalara gönderir |
| `/scamline etapbitir` | Aktif etabı bitirir, puanları dağıtır |
| `/scamline yenietap` | Sıradaki etabı başlatır |
| `/scamline durdur` | Oyunu zorla durdurur |
| `/scamline palyacotahmin <oyuncu>` | Kostüm etabında palyaço tahmini |
| `/scamline sayitahmin <sayı> <oyuncu>` | Sayı etabında tahmin |
| `/scamline renktahmin <renk> <oyuncu>` | Renk etabında tahmin |

## Etaplar
Paylaş ya da Çal, Kostüm Partisi, Sayı Tahmin, Renk Tahmin, İyi Oy, Kötü Oy, Ok Ruleti. Sıra her oyunda rastgele belirlenir.

## Not
Bu kod sunucuda henüz test edilmedi. Simple API metot adlarında veya sürüm farklarında küçük düzeltmeler gerekebilir.
