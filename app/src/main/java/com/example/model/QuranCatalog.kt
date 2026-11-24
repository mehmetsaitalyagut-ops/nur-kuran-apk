package com.example.model

object QuranCatalog {

    val reciters: List<Reciter> = listOf(
        Reciter(
            id = "ar.alafasy",
            nameTr = "Mishary Rashid Alafasy",
            titleTr = "Kuveyt Büyük Camii",
            countryTr = "Kuveyt",
            styleTr = "Tertil • Huzurlu ve akıcı tilavet",
            initials = "MA",
            everyAyahFolder = "Alafasy_128kbps"
        ),
        Reciter(
            id = "ar.abdurrahmaansudais",
            nameTr = "Abdürrahman es-Sudeys",
            titleTr = "Mescid-i Haram İmamı",
            countryTr = "Mekke",
            styleTr = "Harem-i Şerif • Güçlü ve vakur okuyuş",
            initials = "AS",
            everyAyahFolder = "Abdurrahmaan_As-Sudais_192kbps"
        ),
        Reciter(
            id = "ar.mahermuaiqly",
            nameTr = "Mahir el-Muaykili",
            titleTr = "Mescid-i Haram İmamı",
            countryTr = "Mekke",
            styleTr = "Tertil • Duygulu ve berrak ses",
            initials = "MM",
            everyAyahFolder = "MaherAlMuaiqly128kbps"
        ),
        Reciter(
            id = "ar.abdulbasitmurattal",
            nameTr = "Abdulbasit Abdussamed",
            titleTr = "Klasik Mısır Ekolü",
            countryTr = "Mısır",
            styleTr = "Murattal • Altın çağ kıraat üstadı",
            initials = "AA",
            everyAyahFolder = "Abdul_Basit_Murattal_192kbps"
        ),
        Reciter(
            id = "ar.husary",
            nameTr = "Mahmud Halil el-Husari",
            titleTr = "Ezher Kıraat Reisi",
            countryTr = "Mısır",
            styleTr = "Tecvid • Öğretici ve ölçülü tilavet",
            initials = "MH",
            everyAyahFolder = "Husary_128kbps"
        ),
        Reciter(
            id = "ar.minshawi",
            nameTr = "Muhammed Sıddık el-Minşavi",
            titleTr = "Klasik Mısır Ekolü",
            countryTr = "Mısır",
            styleTr = "Hușû • Derin ve tesirli tilavet",
            initials = "MS",
            everyAyahFolder = "Minshawy_Murattal_128kbps"
        )
    )

    fun reciterById(id: String): Reciter =
        reciters.firstOrNull { it.id == id } ?: reciters.first()

    fun buildEveryAyahUrl(reciter: Reciter, surahNumber: Int, ayahInSurah: Int): String {
        val folder = reciter.everyAyahFolder.ifBlank { "Alafasy_128kbps" }
        return String.format(
            java.util.Locale.US,
            "https://everyayah.com/data/%s/%03d%03d.mp3",
            folder,
            surahNumber,
            ayahInSurah
        )
    }

    val surahs: List<SurahInfo> = listOf(
        SurahInfo(1, "الفاتحة", "Fâtiha", "Açılış", "Mekki", 7, isPopular = true),
        SurahInfo(2, "البقرة", "Bakara", "İnek", "Medeni", 286, isPopular = true),
        SurahInfo(3, "آل عمران", "Âl-i İmrân", "İmrân Ailesi", "Medeni", 200),
        SurahInfo(4, "النساء", "Nisâ", "Kadınlar", "Medeni", 176),
        SurahInfo(5, "المائدة", "Mâide", "Sofra", "Medeni", 120),
        SurahInfo(6, "الأنعام", "En'âm", "Hayvanlar", "Mekki", 165),
        SurahInfo(7, "الأعراف", "A'râf", "Surlar / Yüksek Yerler", "Mekki", 206),
        SurahInfo(8, "الأنفال", "Enfâl", "Ganimetler", "Medeni", 75),
        SurahInfo(9, "التوبة", "Tevbe", "Tövbe", "Medeni", 129),
        SurahInfo(10, "يونس", "Yûnus", "Hz. Yûnus", "Mekki", 109),
        SurahInfo(11, "هود", "Hûd", "Hz. Hûd", "Mekki", 123),
        SurahInfo(12, "يوسف", "Yûsuf", "Hz. Yûsuf", "Mekki", 111),
        SurahInfo(13, "الرعد", "Ra'd", "Gök Gürültüsü", "Medeni", 43),
        SurahInfo(14, "إبراهيم", "İbrâhîm", "Hz. İbrâhîm", "Mekki", 52),
        SurahInfo(15, "الحجر", "Hicr", "Hicr Bölgesi", "Mekki", 99),
        SurahInfo(16, "النحل", "Nahl", "Bal Arısı", "Mekki", 128),
        SurahInfo(17, "الإسراء", "İsrâ", "Gece Yürüyüşü", "Mekki", 111),
        SurahInfo(18, "الكهف", "Kehf", "Mağara", "Mekki", 110, isPopular = true),
        SurahInfo(19, "مريم", "Meryem", "Hz. Meryem", "Mekki", 98),
        SurahInfo(20, "طه", "Tâhâ", "Tâhâ", "Mekki", 135),
        SurahInfo(21, "الأنبياء", "Enbiyâ", "Peygamberler", "Mekki", 112),
        SurahInfo(22, "الحج", "Hac", "Hac", "Medeni", 78),
        SurahInfo(23, "المؤمنون", "Mü'minûn", "Müminler", "Mekki", 118),
        SurahInfo(24, "النور", "Nûr", "Işık / Nûr", "Medeni", 64),
        SurahInfo(25, "الفرقان", "Furkân", "Hak ile Bâtılı Ayıran", "Mekki", 77),
        SurahInfo(26, "الشعراء", "Şuarâ", "Şairler", "Mekki", 227),
        SurahInfo(27, "النمل", "Neml", "Karınca", "Mekki", 93),
        SurahInfo(28, "القصص", "Kasas", "Kıssalar", "Mekki", 88),
        SurahInfo(29, "العنكبوت", "Ankebût", "Örümcek", "Mekki", 69),
        SurahInfo(30, "الروم", "Rûm", "Rumlular", "Mekki", 60),
        SurahInfo(31, "لقمان", "Lokmân", "Hz. Lokmân", "Mekki", 34),
        SurahInfo(32, "السجدة", "Secde", "Secde", "Mekki", 30),
        SurahInfo(33, "الأحزاب", "Ahzâb", "Gruplar", "Medeni", 73),
        SurahInfo(34, "سبإ", "Sebe'", "Sebe Kavmi", "Mekki", 54),
        SurahInfo(35, "فاطر", "Fâtır", "Yoktan Var Eden", "Mekki", 45),
        SurahInfo(36, "يس", "Yâsîn", "Yâsîn", "Mekki", 83, isPopular = true),
        SurahInfo(37, "الصافات", "Sâffât", "Saf Tutanlar", "Mekki", 182),
        SurahInfo(38, "ص", "Sâd", "Sâd", "Mekki", 88),
        SurahInfo(39, "الزمر", "Zümer", "Gruplar", "Mekki", 75),
        SurahInfo(40, "غافر", "Mü'min (Gâfir)", "Bağışlayan", "Mekki", 85),
        SurahInfo(41, "فصلت", "Fussilet", "Açıklanmış", "Mekki", 54),
        SurahInfo(42, "الشورى", "Şûrâ", "Danışma", "Mekki", 53),
        SurahInfo(43, "الزخرف", "Zuhruf", "Altın Süs", "Mekki", 89),
        SurahInfo(44, "الدخان", "Duhân", "Duman", "Mekki", 59),
        SurahInfo(45, "الجاثية", "Câsiye", "Diz Çökenler", "Mekki", 37),
        SurahInfo(46, "الأحقاف", "Ahkâf", "Kum Tepeleri", "Mekki", 35),
        SurahInfo(47, "محمد", "Muhammed", "Hz. Muhammed", "Medeni", 38),
        SurahInfo(48, "الفتح", "Fetih", "Fetih / Zafer", "Medeni", 29, isPopular = true),
        SurahInfo(49, "الحجرات", "Hucurât", "Odalar", "Medeni", 18),
        SurahInfo(50, "ق", "Kâf", "Kâf", "Mekki", 45),
        SurahInfo(51, "الذاريات", "Zâriyât", "Savurup Dağıtanlar", "Mekki", 60),
        SurahInfo(52, "الطور", "Tûr", "Tûr Dağı", "Mekki", 49),
        SurahInfo(53, "النجم", "Necm", "Yıldız", "Mekki", 62),
        SurahInfo(54, "القمر", "Kamer", "Ay", "Mekki", 55),
        SurahInfo(55, "الرحمن", "Rahmân", "Çok Merhametli", "Medeni", 78, isPopular = true),
        SurahInfo(56, "الواقعة", "Vâkıa", "Kıyamet Olayı", "Mekki", 96, isPopular = true),
        SurahInfo(57, "الحديد", "Hadîd", "Demir", "Medeni", 29),
        SurahInfo(58, "المجادلة", "Mücâdele", "Mücadele Eden", "Medeni", 22),
        SurahInfo(59, "الحشر", "Haşr", "Toplanma", "Medeni", 24),
        SurahInfo(60, "الممتحنة", "Mümtehine", "Sınanan Kadın", "Medeni", 13),
        SurahInfo(61, "الصف", "Saf", "Savaş Düzeni", "Medeni", 14),
        SurahInfo(62, "الجمعة", "Cuma", "Cuma Günü", "Medeni", 11),
        SurahInfo(63, "المنافقون", "Münâfikûn", "Münafıklar", "Medeni", 11),
        SurahInfo(64, "التغابن", "Tegâbün", "Aldanma / Kâr-Zarar", "Medeni", 18),
        SurahInfo(65, "الطلاق", "Talâk", "Boşanma", "Medeni", 12),
        SurahInfo(66, "التحريم", "Tahrîm", "Haram Kılma", "Medeni", 12),
        SurahInfo(67, "الملك", "Mülk", "Hükümranlık", "Mekki", 30, isPopular = true),
        SurahInfo(68, "القلم", "Kalem", "Kalem", "Mekki", 52),
        SurahInfo(69, "الحاقة", "Hâkka", "Gerçekleşecek Olan", "Mekki", 52),
        SurahInfo(70, "المعارج", "Meâric", "Yükselme Dereceleri", "Mekki", 44),
        SurahInfo(71, "نوح", "Nûh", "Hz. Nûh", "Mekki", 28),
        SurahInfo(72, "الجن", "Cin", "Cinler", "Mekki", 28),
        SurahInfo(73, "المزمل", "Müzzemmil", "Örtüsüne Bürünen", "Mekki", 20),
        SurahInfo(74, "المدثر", "Müddessir", "Bürünüp Sarınan", "Mekki", 56),
        SurahInfo(75, "القيامة", "Kıyâmet", "Kıyamet", "Mekki", 40),
        SurahInfo(76, "الإنسان", "İnsân", "İnsan", "Medeni", 31),
        SurahInfo(77, "المرسلات", "Mürselât", "Gönderilenler", "Mekki", 50),
        SurahInfo(78, "النبإ", "Nebe'", "Büyük Haber", "Mekki", 40, isPopular = true),
        SurahInfo(79, "النازعات", "Nâziât", "Söküp Çıkaranlar", "Mekki", 46),
        SurahInfo(80, "عبس", "Abese", "Yüzünü Ekşitti", "Mekki", 42),
        SurahInfo(81, "التكوير", "Tekvîr", "Dürülüp Bükülme", "Mekki", 29),
        SurahInfo(82, "الانفطار", "İnfitâr", "Yarılma", "Mekki", 19),
        SurahInfo(83, "المطففين", "Mutaffifîn", "Ölçüde Hile Yapanlar", "Mekki", 36),
        SurahInfo(84, "الانشقاق", "İnşikâk", "Parçalanma", "Mekki", 25),
        SurahInfo(85, "البروج", "Bürûc", "Burçlar", "Mekki", 22),
        SurahInfo(86, "الطارق", "Târık", "Gece Yıldızı", "Mekki", 17),
        SurahInfo(87, "الأعلى", "A'lâ", "En Yüce", "Mekki", 19),
        SurahInfo(88, "الغاشية", "Gâşiye", "Kaplayıp Bürüyecek Olan", "Mekki", 26),
        SurahInfo(89, "الفجر", "Fecr", "Tan Yeri / Şafak", "Mekki", 30),
        SurahInfo(90, "البلد", "Beled", "Şehir", "Mekki", 20),
        SurahInfo(91, "الشمس", "Şems", "Güneş", "Mekki", 15),
        SurahInfo(92, "الليل", "Leyl", "Gece", "Mekki", 21),
        SurahInfo(93, "الضحى", "Duhâ", "Kuşluk Vakti", "Mekki", 11, isPopular = true),
        SurahInfo(94, "الشرح", "İnşirâh", "Göğsü Açıp Ferahlatma", "Mekki", 8, isPopular = true),
        SurahInfo(95, "التين", "Tîn", "İncir", "Mekki", 8),
        SurahInfo(96, "العلق", "Alak", "Asılıp Tutunan", "Mekki", 19),
        SurahInfo(97, "القدر", "Kadir", "Kadir Gecesi", "Mekki", 5),
        SurahInfo(98, "البينة", "Beyyine", "Apaçık Delil", "Medeni", 8),
        SurahInfo(99, "الزلزلة", "Zilzâl", "Deprem", "Medeni", 8),
        SurahInfo(100, "العاديات", "Âdiyât", "Koşan Atlar", "Mekki", 11),
        SurahInfo(101, "القارعة", "Kâria", "Yürekleri Hoplatan", "Mekki", 11),
        SurahInfo(102, "التكاثر", "Tekâsür", "Çoğalma Yarışı", "Mekki", 8),
        SurahInfo(103, "العصر", "Asr", "Zaman / İkindi", "Mekki", 3),
        SurahInfo(104, "الهمزة", "Hümeze", "Kötüleyen / Alaycı", "Mekki", 9),
        SurahInfo(105, "الفيل", "Fîl", "Fil", "Mekki", 5),
        SurahInfo(106, "قريش", "Kureyş", "Kureyş Kabilesi", "Mekki", 4),
        SurahInfo(107, "الماعون", "Mâûn", "Yardımlaşma", "Mekki", 7),
        SurahInfo(108, "الكوثر", "Kevser", "Bolluk ve Bereket", "Mekki", 3),
        SurahInfo(109, "الكافرون", "Kâfirûn", "İnkârcılar", "Mekki", 6),
        SurahInfo(110, "النصر", "Nasr", "Yardım / Zafer", "Medeni", 3),
        SurahInfo(111, "المسد", "Tebbet (Mesed)", "Hurma Lifi", "Mekki", 5),
        SurahInfo(112, "الإخلاص", "İhlâs", "Samimi İnanç / Tevhid", "Mekki", 4, isPopular = true),
        SurahInfo(113, "الفلق", "Felak", "Sabah Aydınlığı", "Mekki", 5),
        SurahInfo(114, "الناس", "Nâs", "İnsanlar", "Mekki", 6)
    )

    fun surahByNumber(number: Int): SurahInfo =
        surahs.firstOrNull { it.number == number } ?: surahs.first()

    val popularSurahs: List<SurahInfo> = surahs.filter { it.isPopular }

    val juzList: List<JuzInfo> = listOf(
        JuzInfo(1, "1. Cüz", "Fâtiha 1 – Bakara 141", 1, 1),
        JuzInfo(2, "2. Cüz", "Bakara 142 – Bakara 252", 2, 142),
        JuzInfo(3, "3. Cüz", "Bakara 253 – Âl-i İmrân 92", 2, 253),
        JuzInfo(4, "4. Cüz", "Âl-i İmrân 93 – Nisâ 23", 3, 93),
        JuzInfo(5, "5. Cüz", "Nisâ 24 – Nisâ 147", 4, 24),
        JuzInfo(6, "6. Cüz", "Nisâ 148 – Mâide 81", 4, 148),
        JuzInfo(7, "7. Cüz", "Mâide 82 – En'âm 110", 5, 82),
        JuzInfo(8, "8. Cüz", "En'âm 111 – A'râf 87", 6, 111),
        JuzInfo(9, "9. Cüz", "A'râf 88 – Enfâl 40", 7, 88),
        JuzInfo(10, "10. Cüz", "Enfâl 41 – Tevbe 92", 8, 41),
        JuzInfo(11, "11. Cüz", "Tevbe 93 – Hûd 5", 9, 93),
        JuzInfo(12, "12. Cüz", "Hûd 6 – Yûsuf 52", 11, 6),
        JuzInfo(13, "13. Cüz", "Yûsuf 53 – İbrâhîm 52", 12, 53),
        JuzInfo(14, "14. Cüz", "Hicr 1 – Nahl 128", 15, 1),
        JuzInfo(15, "15. Cüz", "İsrâ 1 – Kehf 74", 17, 1),
        JuzInfo(16, "16. Cüz", "Kehf 75 – Tâhâ 135", 18, 75),
        JuzInfo(17, "17. Cüz", "Enbiyâ 1 – Hac 78", 21, 1),
        JuzInfo(18, "18. Cüz", "Mü'minûn 1 – Furkân 20", 23, 1),
        JuzInfo(19, "19. Cüz", "Furkân 21 – Neml 55", 25, 21),
        JuzInfo(20, "20. Cüz", "Neml 56 – Ankebût 45", 27, 56),
        JuzInfo(21, "21. Cüz", "Ankebût 46 – Ahzâb 30", 29, 46),
        JuzInfo(22, "22. Cüz", "Ahzâb 31 – Yâsîn 27", 33, 31),
        JuzInfo(23, "23. Cüz", "Yâsîn 28 – Zümer 31", 36, 28),
        JuzInfo(24, "24. Cüz", "Zümer 32 – Fussilet 46", 39, 32),
        JuzInfo(25, "25. Cüz", "Fussilet 47 – Câsiye 37", 41, 47),
        JuzInfo(26, "26. Cüz", "Ahkâf 1 – Zâriyât 30", 46, 1),
        JuzInfo(27, "27. Cüz", "Zâriyât 31 – Hadîd 29", 51, 31),
        JuzInfo(28, "28. Cüz", "Mücâdele 1 – Tahrîm 12", 58, 1),
        JuzInfo(29, "29. Cüz", "Mülk 1 – Mürselât 50", 67, 1),
        JuzInfo(30, "30. Cüz", "Nebe' 1 – Nâs 6", 78, 1)
    )

    val dailyVerses: List<DailyVerse> = listOf(
        DailyVerse(
            surahNumber = 94,
            surahNameTr = "İnşirâh",
            ayahNumber = 6,
            arabicText = "إِنَّ مَعَ ٱلْعُسْرِ يُسْرًا",
            turkishText = "Şüphesiz güçlükle beraber bir kolaylık vardır."
        ),
        DailyVerse(
            surahNumber = 13,
            surahNameTr = "Ra'd",
            ayahNumber = 28,
            arabicText = "أَلَا بِذِكْرِ ٱللَّهِ تَطْمَئِنُّ ٱلْقُلُوبُ",
            turkishText = "Bilesiniz ki kalpler ancak Allah’ı anmakla huzur bulur."
        ),
        DailyVerse(
            surahNumber = 2,
            surahNameTr = "Bakara",
            ayahNumber = 152,
            arabicText = "فَٱذْكُرُونِىٓ أَذْكُرْكُمْ وَٱشْكُرُوا۟ لِى وَلَا تَكْفُرُونِ",
            turkishText = "Öyleyse siz beni anın ki ben de sizi anayım. Bana şükredin, sakın nankörlük etmeyin."
        ),
        DailyVerse(
            surahNumber = 24,
            surahNameTr = "Nûr",
            ayahNumber = 35,
            arabicText = "ٱللَّهُ نُورُ ٱلسَّمَـٰوَٰتِ وَٱلْأَرْضِ",
            turkishText = "Allah, göklerin ve yerin nûrudur."
        ),
        DailyVerse(
            surahNumber = 39,
            surahNameTr = "Zümer",
            ayahNumber = 53,
            arabicText = "لَا تَقْنَطُوا۟ مِن رَّحْمَةِ ٱللَّهِ ۚ إِنَّ ٱللَّهَ يَغْفِرُ ٱلذُّنُوبَ جَمِيعًا",
            turkishText = "Allah’ın rahmetinden ümit kesmeyin. Şüphesiz Allah bütün günahları bağışlar."
        ),
        DailyVerse(
            surahNumber = 65,
            surahNameTr = "Talâk",
            ayahNumber = 3,
            arabicText = "وَمَن يَتَوَكَّلْ عَلَى ٱللَّهِ فَهُوَ حَسْبُهُۥ",
            turkishText = "Kim Allah’a tevekkül ederse O, kendisine yeter."
        )
    )

    fun verseOfToday(dayOfYear: Int): DailyVerse {
        val idx = (dayOfYear.coerceAtLeast(0)) % dailyVerses.size
        return dailyVerses[idx]
    }
}
