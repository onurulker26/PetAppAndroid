package com.tamer.petapp.model

data class PetCareGuide(
    val sections: List<Section>
) {
    data class Section(
        val title: String,
        val content: List<String>
    )

    companion object {
        fun getDogGuide() = PetCareGuide(
            sections = listOf(
                Section(
                    title = "Beslenme",
                    content = listOf(
                        "• Yaşına ve boyutuna uygun kaliteli köpek maması tercih edin",
                        "• Günlük öğün sayısını yaşına göre ayarlayın (yetişkinler için 2 öğün)",
                        "• Her zaman temiz su bulundurun",
                        "• İnsan yiyeceklerinden uzak tutun, özellikle çikolata, üzüm ve soğan gibi zararlı besinlerden",
                        "• Mama değişikliklerini kademeli yapın"
                    )
                ),
                Section(
                    title = "Egzersiz ve Aktivite",
                    content = listOf(
                        "• Günde en az 30-60 dakika yürüyüş yaptırın",
                        "• Irkına ve enerjisine uygun aktiviteler planlayın",
                        "• Zihinsel aktiviteler ve oyuncaklar sağlayın",
                        "• Düzenli oyun seansları yapın",
                        "• Aşırı sıcak veya soğuk havalarda dikkatli olun"
                    )
                ),
                Section(
                    title = "Sağlık Bakımı",
                    content = listOf(
                        "• Düzenli veteriner kontrolü (yılda en az 1 kez)",
                        "• Aşı takvimini takip edin",
                        "• İç ve dış parazit kontrolü yapın",
                        "• Diş sağlığını kontrol edin ve düzenli diş fırçalama",
                        "• Tüy bakımı ve tarama",
                        "• Tırnak kesimi (ayda 1 kez veya gerektiğinde)"
                    )
                ),
                Section(
                    title = "Hijyen",
                    content = listOf(
                        "• Ayda 1-2 kez veya gerektiğinde banyo",
                        "• Kulak temizliği ve kontrolü",
                        "• Göz çevresi temizliği",
                        "• Yatak ve oyuncakların düzenli temizliği",
                        "• Mama ve su kaplarının günlük temizliği"
                    )
                ),
                Section(
                    title = "Eğitim ve Sosyalleşme",
                    content = listOf(
                        "• Temel itaat eğitimi",
                        "• Erken yaşta sosyalleşme",
                        "• Pozitif pekiştirme yöntemleri kullanın",
                        "• Tutarlı kurallar ve sınırlar belirleyin",
                        "• Diğer köpeklerle güvenli etkileşim sağlayın"
                    )
                ),
                Section(
                    title = "Güvenlik",
                    content = listOf(
                        "• Mikroçip taktırın",
                        "• Tasma ve künye kullanın",
                        "• Ev içi tehlikelere karşı önlem alın",
                        "• Seyahat güvenliği için gerekli ekipmanları bulundurun",
                        "• Acil durum planı hazırlayın"
                    )
                )
            )
        )

        fun getCatGuide() = PetCareGuide(
            sections = listOf(
                Section(
                    title = "Beslenme",
                    content = listOf(
                        "• Yaşa ve aktiviteye uygun kaliteli kedi maması",
                        "• Günde 2-3 öğün düzenli besleme",
                        "• Taze ve temiz su (günlük değişim)",
                        "• Yaş mama takviyesi (protein desteği)",
                        "• Özel diyet gereksinimleri (varsa)",
                        "• Zararlı besinlerden koruma (çiğ et, süt ürünleri)"
                    )
                ),
                Section(
                    title = "Kum Kabı Bakımı",
                    content = listOf(
                        "• Günlük temizlik (en az 2 kez)",
                        "• Haftalık komple kum değişimi",
                        "• Her kedi için ayrı kum kabı (1 kedi + 1 yedek)",
                        "• Sessiz ve erişilebilir lokasyon",
                        "• Kaliteli ve topaklanan kum kullanımı",
                        "• Düzenli kum kabı dezenfeksiyonu"
                    )
                ),
                Section(
                    title = "Sağlık Bakımı",
                    content = listOf(
                        "• Yıllık veteriner kontrolleri",
                        "• Düzenli aşı takibi ve yenileme",
                        "• Aylık parazit kontrolü ve koruma",
                        "• Diş sağlığı kontrolü ve bakımı",
                        "• Düzenli tüy bakımı ve tarama",
                        "• Tırnak kontrolü ve kesimi (2-4 haftada bir)"
                    )
                ),
                Section(
                    title = "Aktivite ve Oyun",
                    content = listOf(
                        "• Günlük interaktif oyun seansları",
                        "• Tırmanma ağacı ve platformlar",
                        "• Çeşitli oyuncaklar (fare, kuş, ipli oyuncaklar)",
                        "• Catnip ve interaktif oyuncaklar",
                        "• Pencere önü dinlenme alanı",
                        "• Karton kutular ve saklanma yerleri"
                    )
                ),
                Section(
                    title = "Ev Düzeni ve Güvenlik",
                    content = listOf(
                        "• Toksik bitkilerden uzak tutma",
                        "• Elektrik kablolarını gizleme/koruma",
                        "• Pencere ve balkon güvenliği",
                        "• Kimyasal temizlik ürünlerinden koruma",
                        "• Küçük nesneleri ulaşılamaz yerde tutma",
                        "• Rahat ve güvenli uyku alanları"
                    )
                ),
                Section(
                    title = "Davranış ve Sosyalleşme",
                    content = listOf(
                        "• Erken yaşta sosyalleştirme",
                        "• Düzenli ve tutarlı rutinler",
                        "• Pozitif pekiştirme teknikleri",
                        "• Stres faktörlerini minimize etme",
                        "• Diğer evcil hayvanlarla tanıştırma",
                        "• Yalnız kalma süresini ayarlama"
                    )
                ),
                Section(
                    title = "Özel Bakım",
                    content = listOf(
                        "• Uzun tüylü kediler için ekstra tüy bakımı",
                        "• Yaşlı kediler için özel düzenlemeler",
                        "• Mevsimsel bakım değişiklikleri",
                        "• Seyahat hazırlıkları",
                        "• Acil durum planı",
                        "• Düzenli ağırlık takibi"
                    )
                )
            )
        )

        fun getBirdGuide() = PetCareGuide(
            sections = listOf(
                Section(
                    title = "Beslenme",
                    content = listOf(
                        "• Türe özgü kaliteli kuş yemi",
                        "• Taze meyve ve sebzeler",
                        "• Temiz su (günlük değişim)",
                        "• Mineral ve vitamin takviyeleri",
                        "• Zararlı yiyeceklerden koruma"
                    )
                ),
                Section(
                    title = "Kafes Bakımı",
                    content = listOf(
                        "• Günlük temizlik",
                        "• Haftalık detaylı temizlik",
                        "• Uygun kafes boyutu",
                        "• Çeşitli tünekler",
                        "• Oyuncak ve aktivite alanları"
                    )
                ),
                Section(
                    title = "Sağlık",
                    content = listOf(
                        "• Düzenli veteriner kontrolleri",
                        "• Tüy ve gaga kontrolü",
                        "• Ayak sağlığı kontrolü",
                        "• Hastalık belirtilerini tanıma",
                        "• Uygun sıcaklık ve nem"
                    )
                ),
                Section(
                    title = "Aktivite",
                    content = listOf(
                        "• Günlük uçuş egzersizi",
                        "• Zihinsel aktivite oyuncakları",
                        "• Sosyal etkileşim",
                        "• Müzik ve ses uyaranları",
                        "• Güvenli kafes dışı zaman"
                    )
                )
            )
        )

        fun getFishGuide() = PetCareGuide(
            sections = listOf(
                Section(
                    title = "Su Kalitesi",
                    content = listOf(
                        "• Düzenli su değişimi: Akvaryum hacminin %20-25'i haftalık olarak değiştirilmelidir. Büyük değişimler balıkları strese sokar.",
                        "• Su parametreleri: pH (6.5-7.5), amonyak (0 ppm), nitrit (0 ppm), nitrat (<20 ppm) değerleri düzenli kontrol edilmelidir.",
                        "• Klor arındırma: Musluk suyu kullanılıyorsa, klor arındırıcı ile işlem görmeli veya 24 saat bekletilmelidir.",
                        "• Su sıcaklığı: Tür özelliklerine göre uygun sıcaklık (genellikle 24-26°C) korunmalı ve ani değişimlerden kaçınılmalıdır.",
                        "• Su sertliği: Balık türlerinize uygun su sertliği (GH ve KH) seviyeleri sağlanmalıdır.",
                        "• Su testleri: Haftalık olarak su parametrelerini test etmek için uygun test kitleri kullanın."
                    )
                ),
                Section(
                    title = "Beslenme",
                    content = listOf(
                        "• Balık yemi çeşitleri: Pul, granül, tabletler ve dondurulmuş yemler gibi çeşitli formlar kullanın. Balık türüne uygun yem seçin.",
                        "• Beslenme sıklığı: Günde 1-2 kez, 2-3 dakika içinde tüketebilecekleri kadar yem verin. Aşırı beslemeden kaçının.",
                        "• Özel beslenme gereksinimleri: Dip balıkları, otçullar ve etçiller için farklı yemler gereklidir.",
                        "• Canlı yem seçenekleri: Artemia, kan kurdu, tubifex veya su piresi gibi canlı yemler beslenmeyi zenginleştirir.",
                        "• Aç bırakma günü: Haftada bir gün aç bırakmak sindirim sisteminin temizlenmesine yardımcı olur.",
                        "• İlaçlı yemlendirme: Gerektiğinde veteriner kontrolünde ilaçlı yem kullanılabilir."
                    )
                ),
                Section(
                    title = "Akvaryum Bakımı",
                    content = listOf(
                        "• Cam temizliği: Yosunlar oluştuğunda yumuşak bir sünger veya manyetik temizleyici ile camları temizleyin.",
                        "• Dip çekimi: Haftada bir kez dipteki atıkları sifon ile temizleyin. Bu işlemle birlikte kısmi su değişimi yapılabilir.",
                        "• Filtre bakımı: Filtre malzemelerini ayda bir temiz akvaryum suyu ile durulayın (musluk suyu kullanmayın).",
                        "• Bitki bakımı: Canlı bitkiler için uygun ışık ve besleyiciler sağlayın. Kurumuş yaprakları düzenli olarak alın.",
                        "• Dekorasyon düzeni: Balıklara yeterli yüzme alanı ve saklanma yerleri sağladığınızdan emin olun.",
                        "• Ekipman kontrolü: Isıtıcı, filtre, hava pompası gibi ekipmanların çalışmasını düzenli kontrol edin."
                    )
                ),
                Section(
                    title = "Sağlık",
                    content = listOf(
                        "• Hastalık belirtileri: Solungaçların hızlı hareketi, renk değişimi, yüzme problemleri, iştahsızlık, vücut yaraları hastalık belirtileridir.",
                        "• Karantina: Yeni balıkları en az 2 hafta karantina akvaryumunda tutun ve hastalık belirtilerini gözlemleyin.",
                        "• Yaygın hastalıklar: İch (beyaz benek), mantar enfeksiyonları, yüzgeç çürümesi ve parazitler en sık görülen hastalıklardır.",
                        "• Balık sayısı: Akvaryumunuzu aşırı kalabalıklaştırmayın. Her balık için en az 2-3 litre su olmalıdır.",
                        "• Uyumsuz türler: Saldırgan ve pasif türleri bir arada bulundurmayın. Tür uyumluluğunu araştırın.",
                        "• Stres faktörleri: Ani ışık değişimleri, aşırı gürültü ve titreşimlerden kaçının. Bunlar balıklarda strese neden olur."
                    )
                ),
                Section(
                    title = "Ekipman Bilgisi",
                    content = listOf(
                        "• Filtre sistemleri: Akvaryum hacmine uygun bir filtre seçin. Saatte en az 3-4 kez tüm suyu devridaim etmelidir.",
                        "• Isıtıcılar: 3-5 watt/litre gücünde ısıtıcı kullanın ve termostat ile sıcaklığı kontrol edin.",
                        "• Aydınlatma: Günde 8-10 saat ışık yeterlidir. Bitkilerin ihtiyaçlarına göre uygun spektrumda ışık kullanın.",
                        "• Havalandırma: Özellikle sıcak havalarda veya yoğun stoklanan akvaryumlarda hava taşı kullanın.",
                        "• Su değişim ekipmanları: Sifon, su kovası, su arıtıcı ve test kitleri gibi temel bakım malzemelerini bulundurun.",
                        "• Otomatik sistemler: Tatil dönemleri için otomatik yemleme cihazları ve zamanlayıcılar kullanılabilir."
                    )
                )
            )
        )

        fun getRabbitGuide() = PetCareGuide(
            sections = listOf(
                Section(
                    title = "Beslenme",
                    content = listOf(
                        "• Saman/kuru ot: Tavşanların diyetinin %80'i kaliteli kuru ot (özellikle timothy otu) olmalıdır. 7/24 sınırsız erişim sağlanmalıdır.",
                        "• Taze yeşillikler: Diyetin %10-15'i taze yeşilliklerden oluşmalıdır. Maydanoz, semizotu, roka, hindiba, brokoli yaprakları verilebilir.",
                        "• Pelet yem: Tavşan peleti diyetin sadece %5'ini oluşturmalıdır. Günde tavşanın boyutuna göre 1-2 yemek kaşığı yeterlidir.",
                        "• Meyveler ve atıştırmalıklar: Elma, armut gibi meyveler çok az miktarda (haftada 1-2 kez küçük parçalar) verilebilir.",
                        "• Su: Temiz ve taze su her zaman bulunmalıdır. Su şişesi yerine geniş ve ağır bir kase tercih edilebilir.",
                        "• Zararlı besinler: Patates, soğan, sarımsak, çikolata, avokado, ceviz ve badem gibi besinler zehirlidir ve kesinlikle verilmemelidir."
                    )
                ),
                Section(
                    title = "Barınak Bakımı",
                    content = listOf(
                        "• Kafes boyutu: Orta boy (3-4 kg) bir tavşan için en az 120x60 cm taban alanı olmalıdır. Ne kadar büyük olursa o kadar iyidir.",
                        "• Zemin kaplama: Sert plastik, tel zemin veya beton zeminler tavşan patilerine zarar verebilir. Yumuşak ancak kaymayan bir zemin tercih edin.",
                        "• Tuvalet eğitimi: Tavşanlar köşeleri tuvalet alanı olarak kullanma eğilimindedir. Bu alanlara tuvalet kabı yerleştirin.",
                        "• Talaş ve altlık: Çam ve sedir talaşı kullanmayın. Kağıt bazlı ürünler veya kenevir altlıklar daha uygundur.",
                        "• Günlük temizlik: Tuvalet alanları ve kirli altlıklar her gün değiştirilmelidir. Haftalık olarak tüm altlık değişimi yapın.",
                        "• Oyun alanı: Tavşanlar günde en az 3-4 saat güvenli bir alanda serbest dolaşabilmelidir."
                    )
                ),
                Section(
                    title = "Sağlık",
                    content = listOf(
                        "• Veteriner kontrolü: Yılda en az bir kez düzenli sağlık kontrolü yaptırın. Tavşanlara özel uzman bir veteriner tercih edin.",
                        "• Aşılar: Myxomatosis ve Viral Hemorrhagic Disease (VHD) için aşılama yapılmalıdır. Veterinerinize danışın.",
                        "• Kısırlaştırma: 4-6 aylık yaşta kısırlaştırma sağlık sorunlarını azaltır ve davranış problemlerini önler.",
                        "• Diş sağlığı: Tavşanların dişleri sürekli uzar. Bu nedenle yeterli saman ve kemirme malzemesi sağlayın. Düzenli diş kontrolü yaptırın.",
                        "• Tüy bakımı: Özellikle uzun tüylü türlerde düzenli tarama gereklidir. Tüy dökme dönemlerinde günlük tarama yapmak gerekebilir.",
                        "• Acil durumlar: İştahsızlık, dışkılama/idrar yapamama, ishal, baş eğikliği acil müdahale gerektiren durumlardır."
                    )
                ),
                Section(
                    title = "Aktivite",
                    content = listOf(
                        "• Günlük egzersiz: Tavşanlar günde en az 3-4 saat güvenli bir alanda serbest hareket etmelidir.",
                        "• Oyuncaklar: Tavşanlar için güvenli olan, boyalı olmayan doğal malzemelerden yapılmış oyuncaklar tercih edin.",
                        "• Kemirme malzemeleri: Söğüt dalları, işlenmemiş deniz otu hasırlar ve karton kutular güvenli kemirme malzemeleridir.",
                        "• Tüneller ve saklanma alanları: Tavşanlar saklanmayı severler. Karton kutular ve özel tüneller sağlayın.",
                        "• Etkileşim: Tavşanınızla her gün zaman geçirin. Onlarla oynamak ve sevmek sosyalleşmeleri için önemlidir.",
                        "• Zenginleştirme: Oyuncakları ve ortamı düzenli olarak değiştirerek zihinsel uyarım sağlayın."
                    )
                ),
                Section(
                    title = "Davranış ve Sosyalleşme",
                    content = listOf(
                        "• Vücut dili: Tavşanlar kulaklarını, vücut duruşlarını ve ayaklarını kullanarak iletişim kurarlar. Bu işaretleri öğrenin.",
                        "• Sosyal hayvanlar: Tavşanlar genellikle yalnız yaşamamalıdır. İdeal olan kısırlaştırılmış bir çift beslemektir.",
                        "• Yeni tavşan tanıştırma: Yeni tavşanları yavaşça ve kontrollü bir şekilde tanıştırın. Bu süreç haftalar alabilir.",
                        "• Toprak kazma: Kazma doğal bir davranıştır. Özel kazma alanları veya derin altlıklı kutular sağlayın.",
                        "• Markaj: Nötrlenmemiş tavşanlar çevreyi işaretleme eğilimindedir. Kısırlaştırma bu davranışı azaltır.",
                        "• Korktuklarında: Tavşanlar tehdit altında hissettiklerinde donup kalabilir veya kaçabilirler. Sakin ve yavaş yaklaşın."
                    )
                )
            )
        )

        fun getHamsterGuide() = PetCareGuide(
            sections = listOf(
                Section(
                    title = "Beslenme",
                    content = listOf(
                        "• Kaliteli hamster yemi: Temel besin kaynağı olarak özel hamster pelet yemleri kullanın. Günlük olarak türe göre 1-2 yemek kaşığı yem yeterlidir.",
                        "• Taze sebze ve meyve: Az miktarda havuç, elma, salatalık gibi taze besinler verilebilir. Günlük olarak küçük bir parça yeterlidir.",
                        "• Protein takviyeleri: Haftada 1-2 kez haşlanmış yumurta, kurtçuk veya böcek gibi protein kaynakları verin.",
                        "• Temiz su: Su şişesi her gün temizlenmeli ve taze su ile doldurulmalıdır.",
                        "• Tahıl karışımları: Ay çekirdeği, keten tohumu gibi tohumları az miktarda takviye olarak verebilirsiniz.",
                        "• Sakıncalı besinler: Çikolata, soğan, sarımsak, avokado, narenciye ve baharatlı yiyeceklerden kaçının."
                    )
                ),
                Section(
                    title = "Kafes Bakımı",
                    content = listOf(
                        "• Kafes boyutu: En az 80x50 cm taban alanı ve 30 cm yüksekliği olan kafesler idealdir. Daha büyük kafesler her zaman daha iyidir.",
                        "• Talaş seçimi: Çam ve sedir talaşı kullanmayın, kokusuz yumuşak kavak talaşı veya kağıt bazlı malzemeler tercih edin.",
                        "• Temizlik rutini: Haftada bir kez kafes tamamen temizlenmeli, talaş değiştirilmelidir. Tuvalet alanlarını daha sık temizleyebilirsiniz.",
                        "• Oyun ekipmanları: Koşu tekerleği (en az 20 cm çapında), tüneller, tırmanma oyuncakları ve saklanma alanları sağlayın.",
                        "• Yataklama malzemesi: Pamuk bazlı malzemeler yerine kağıt havlu parçaları veya hamster için özel üretilmiş malzemeler kullanın.",
                        "• Isı kontrolü: Hamsterlar 18-24°C arası sıcaklıkta tutulmalı, doğrudan güneş ışığı ve hava cereyanından korunmalıdır."
                    )
                ),
                Section(
                    title = "Sağlık",
                    content = listOf(
                        "• Gözlem: Her gün hamsterinizi gözlemleyin. Aktivite seviyesi, iştah, tüy durumu ve davranış değişikliklerine dikkat edin.",
                        "• Diş bakımı: Hamsterların dişleri sürekli uzar. Kemirmeleri için ağaç dalları veya özel kemirme oyuncakları sağlayın.",
                        "• Tüy kontrolü: Düzenli olarak tüylerinde parazit, dökülme veya yaralanma olup olmadığını kontrol edin.",
                        "• Yaygın hastalıklar: İshal, soğuk algınlığı, ıslak kuyruk sendromu ve tümörler sık görülen sağlık sorunlarıdır. Anormal belirtilerde hemen veterinere başvurun.",
                        "• Kulak ve göz bakımı: Kulak ve gözlerde kızarıklık, akıntı veya şişlik olup olmadığını düzenli olarak kontrol edin.",
                        "• Yaşam süresi: Hamsterlar genellikle 2-3 yıl yaşarlar. Yaşla birlikte özel bakım ihtiyaçları olabilir."
                    )
                ),
                Section(
                    title = "Aktivite",
                    content = listOf(
                        "• Koşu tekerleği: Sağlam ve geniş bir koşu tekerleği hamsterin fiziksel aktivitesi için gereklidir. Kapalı yüzeyli olmalıdır.",
                        "• Tünel sistemleri: Tüneller doğal kazma içgüdüsünü tatmin eder. Plastik veya karton tüneller ekleyebilirsiniz.",
                        "• Serbest dolaşım: Haftada birkaç kez hamster topu veya güvenli bir alanda gözetim altında serbest dolaşım imkanı verin.",
                        "• Kemirme oyuncakları: Doğal ağaç dalları, hamster bisküvileri ve güvenli kemirme oyuncakları dişlerin sağlığı için önemlidir.",
                        "• Yuvalar ve saklama alanları: Hamsterlar saklanmayı ve yuva yapmayı sever. Çeşitli saklanma alanları ve yuva malzemeleri sağlayın.",
                        "• İnteraktif oyun: Hamsterinizle akşam saatlerinde (aktif oldukları zamanda) kısa oyun seansları düzenleyin."
                    )
                ),
                Section(
                    title = "Davranış ve Sosyalleşme",
                    content = listOf(
                        "• Gece aktivitesi: Hamsterlar gece aktif hayvanlardır, gündüz uyurlar. Bu doğal ritimlerine saygı gösterin.",
                        "• Yalnız yaşam: Çoğu hamster türü (özellikle Suriye hamsterları) yalnız yaşamalıdır. Birlikte yaşatılan hamsterlar kavga edebilir.",
                        "• Ele alıştırma: Zamanla ve sabırla hamsterinizi ele alıştırın. Ani hareketlerden kaçının ve her zaman nazik olun.",
                        "• Korktuğunda davranışları: Korktuğunda ısırabilir veya donabilir. Bu durumlarda zorlamayın ve sakinleşmesini bekleyin.",
                        "• İletişim sesleri: Bazı sesler (ciyaklama, hırlama) rahatsızlığı gösterir. Bu seslere dikkat edin.",
                        "• Rutin: Hamsterlar rutinlere alışkındır, besleme ve aktivite zamanlarını düzenli tutun."
                    )
                )
            )
        )

        fun getGenericGuide() = PetCareGuide(
            sections = listOf(
                Section(
                    title = "Temel Bakım",
                    content = listOf(
                        "• Türe uygun beslenme",
                        "• Düzenli veteriner kontrolleri",
                        "• Uygun yaşam alanı",
                        "• Temizlik ve hijyen",
                        "• Sevgi ve ilgi"
                    )
                )
            )
        )
    }
} 