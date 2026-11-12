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
1. Odalar artık `/scamline baslat` ile otomatik kurulur. Oyunu başlatmadan önce odaların başlamasını istediğin yerde dur. Ayrıca odaları ayrıca kurmak istersen `/scamline kurulum` da kullanabilirsin. Plugin 8 oda ve koridoru 250 blok arayla kurar ve koordinatları `config.yml` dosyasına yazar. Bu komut kurulum alanındaki bloklarını temizler, o yüzden boş bir alanda çalıştır.
2. `frames` ayarlarını gerekirse düzenle. Yeşil/kırmızı çerçeveler oda içindeki kuzey duvarına yerleşir.
3. Skin adlarını ve `skin-command` şablonunu kendi SkinsRestorer sürümüne göre kontrol et.

## Komutlar
| Komut | Açıklama |
|---|---|
| `/scamline kurulum` | Odaları ve koridoru kurar |
| `/scamline baslat` | Odaları komutu yazdığın yerde kurar, oyuncuları odalara gönderir |
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
