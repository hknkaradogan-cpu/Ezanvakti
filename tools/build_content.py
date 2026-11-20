#!/usr/bin/env python3
"""web/data/content/*.json dosyalarını üretir. Kur'an'dan alınan dualar ve günün ayetleri, metni
   doğrudan web/data/quran/*.json (Uthmani metin + Diyanet meali + Saheeh International) içinden alır;
   elle yazılmış Kur'an metni yoktur."""
import json, os
from content_esma import ESMA
B = os.path.join(os.path.dirname(__file__), '..', 'web', 'data')
Q = {k: json.load(open(f'{B}/quran/{k}.json', encoding='utf-8')) for k in ('ar', 'tr', 'en', 'translit')}
def ayah(s, a, b=None):
    rng = range(a, (b or a) + 1)
    return {'ar': ' '.join(Q['ar'][s-1][i-1] for i in rng), 'tr': ' '.join(Q['tr'][s-1][i-1] for i in rng),
            'en': ' '.join(Q['en'][s-1][i-1] for i in rng), 'ref': f'{s}:{a}' + (f'-{b}' if b else '')}
def dump(n, o):
    os.makedirs(f'{B}/content', exist_ok=True)
    json.dump(o, open(f'{B}/content/{n}.json', 'w', encoding='utf-8'), ensure_ascii=False, separators=(',', ':'))
    print(n, len(o))

SURAH_TR = {s['n']: s['tr'] for s in json.load(open(f'{B}/quran/surahs.json', encoding='utf-8'))}
def qdua(cat, s, a, b, oku, title):
    x = ayah(s, a, b); x.update({'cat': cat, 'title': title, 'okunus': oku, 'src': f"Kur'an-ı Kerim, {SURAH_TR[s]} {a}" + (f'-{b}' if b else ''), 'quran': True}); return x
def hdua(cat, title, ar, oku, tr, en, src):
    return {'cat': cat, 'title': title, 'ar': ar, 'okunus': oku, 'tr': tr, 'en': en, 'src': src, 'quran': False}

CATS = [('sabah','Sabah duaları','Morning'),('aksam','Akşam duaları','Evening'),('yemek','Yemek duaları','Meals'),('yolculuk','Yolculuk duaları','Travel'),
        ('sifa','Şifa duaları','Healing'),('rizik','Rızık duaları','Provision'),('aile','Aile duaları','Family'),('anne_baba','Anne ve baba duaları','Parents'),
        ('sikinti','Sıkıntı duaları','Distress'),('uyku','Uyku duaları','Sleep'),('namaz','Namaz sonrası dualar','After prayer'),
        ('peygamber','Peygamber duaları','Prophetic duas'),('ramazan','Ramazan duaları','Ramadan')]
D = [
 hdua('sabah','Sabaha erişince',"أَصْبَحْنَا وَأَصْبَحَ الْمُلْكُ لِلَّهِ، وَالْحَمْدُ لِلَّهِ، لَا إِلَٰهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلَىٰ كُلِّ شَيْءٍ قَدِيرٌ",
      "Asbahnâ ve asbaha'l-mülkü lillâh, ve'l-hamdü lillâh, lâ ilâhe illallâhü vahdehû lâ şerîke leh, lehü'l-mülkü ve lehü'l-hamdü ve hüve alâ külli şey'in kadîr.",
      "Sabaha erdik, mülk de Allah'ın olarak sabaha erdi. Hamd Allah'a mahsustur. Allah'tan başka ilah yoktur; O tektir, ortağı yoktur. Mülk O'nundur, hamd O'na aittir ve O her şeye kadirdir.",
      "We have entered the morning and the whole dominion belongs to Allah. Praise be to Allah. There is no god but Allah alone, without partner; His is the dominion and His is the praise, and He has power over all things.","Müslim, Zikir 75; Ebû Dâvûd, Edeb 101"),
 hdua('sabah','Uyanınca',"الْحَمْدُ لِلَّهِ الَّذِي أَحْيَانَا بَعْدَ مَا أَمَاتَنَا وَإِلَيْهِ النُّشُورُ",
      "Elhamdü lillâhillezî ahyânâ ba'de mâ emâtenâ ve ileyhi'n-nüşûr.","Bizi öldürdükten (uyuttuktan) sonra dirilten Allah'a hamd olsun. Dönüş O'nadır.",
      "Praise be to Allah who gave us life after He caused us to die, and to Him is the resurrection.","Buhârî, Deavât 7"),
 hdua('sabah','Seyyidü\'l-istiğfâr',"اللَّهُمَّ أَنْتَ رَبِّي لَا إِلَٰهَ إِلَّا أَنْتَ، خَلَقْتَنِي وَأَنَا عَبْدُكَ، وَأَنَا عَلَىٰ عَهْدِكَ وَوَعْدِكَ مَا اسْتَطَعْتُ، أَعُوذُ بِكَ مِنْ شَرِّ مَا صَنَعْتُ، أَبُوءُ لَكَ بِنِعْمَتِكَ عَلَيَّ، وَأَبُوءُ بِذَنْبِي، فَاغْفِرْ لِي فَإِنَّهُ لَا يَغْفِرُ الذُّنُوبَ إِلَّا أَنْتَ",
      "Allâhümme ente rabbî lâ ilâhe illâ ente, halaktenî ve ene abdük, ve ene alâ ahdike ve va'dike mesteta'tü, eûzü bike min şerri mâ sana'tü, ebûü leke bi-ni'metike aleyye, ve ebûü bi-zenbî, fağfir lî fe-innehû lâ yağfirü'z-zünûbe illâ ent.",
      "Allah'ım! Sen benim Rabbimsin. Senden başka ilah yoktur. Beni Sen yarattın, ben Senin kulunum. Gücüm yettiğince Sana verdiğim söz ve ahdin üzerindeyim. Yaptıklarımın şerrinden Sana sığınırım. Bana verdiğin nimetleri ikrar eder, günahımı itiraf ederim. Beni bağışla; çünkü günahları Senden başka bağışlayan yoktur.",
      "O Allah, You are my Lord; there is no god but You. You created me and I am Your servant, and I abide by Your covenant and promise as best I can. I seek refuge in You from the evil of what I have done. I acknowledge Your favour upon me and I acknowledge my sin, so forgive me, for none forgives sins but You.","Buhârî, Deavât 2, 16"),
 hdua('aksam','Akşama erişince',"أَمْسَيْنَا وَأَمْسَى الْمُلْكُ لِلَّهِ، وَالْحَمْدُ لِلَّهِ، لَا إِلَٰهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلَىٰ كُلِّ شَيْءٍ قَدِيرٌ",
      "Emseynâ ve emse'l-mülkü lillâh, ve'l-hamdü lillâh, lâ ilâhe illallâhü vahdehû lâ şerîke leh, lehü'l-mülkü ve lehü'l-hamdü ve hüve alâ külli şey'in kadîr.",
      "Akşama erdik, mülk de Allah'ın olarak akşama erdi. Hamd Allah'a mahsustur. Allah'tan başka ilah yoktur; O tektir, ortağı yoktur. Mülk O'nundur, hamd O'na aittir ve O her şeye kadirdir.",
      "We have entered the evening and the whole dominion belongs to Allah. Praise be to Allah. There is no god but Allah alone, without partner; His is the dominion and His is the praise, and He has power over all things.","Müslim, Zikir 75; Ebû Dâvûd, Edeb 101"),
 hdua('aksam','Akşam sığınma duası',"أَعُوذُ بِكَلِمَاتِ اللَّهِ التَّامَّاتِ مِنْ شَرِّ مَا خَلَقَ",
      "Eûzü bi-kelimâtillâhi't-tâmmâti min şerri mâ halak.","Yarattığı şeylerin şerrinden Allah'ın eksiksiz kelimelerine sığınırım.",
      "I seek refuge in the perfect words of Allah from the evil of what He has created.","Müslim, Zikir 54-55"),
 hdua('aksam','Zarar görmemek için (üç defa)',"بِسْمِ اللَّهِ الَّذِي لَا يَضُرُّ مَعَ اسْمِهِ شَيْءٌ فِي الْأَرْضِ وَلَا فِي السَّمَاءِ وَهُوَ السَّمِيعُ الْعَلِيمُ",
      "Bismillâhillezî lâ yedurru mea'smihî şey'ün fi'l-ardı ve lâ fi's-semâi ve hüve's-semîu'l-alîm.","İsmiyle birlikte yerde ve gökte hiçbir şeyin zarar veremediği Allah'ın adıyla. O her şeyi işiten ve bilendir.",
      "In the name of Allah, with whose name nothing on earth or in heaven can cause harm, and He is the All-Hearing, the All-Knowing.","Ebû Dâvûd, Edeb 101; Tirmizî, Deavât 13"),
 hdua('yemek','Yemeğe başlarken',"بِسْمِ اللَّهِ","Bismillâh.","Allah'ın adıyla.","In the name of Allah.","Buhârî, Et'ime 2; Müslim, Eşribe 108"),
 hdua('yemek','Başta besmele unutulursa',"بِسْمِ اللَّهِ أَوَّلَهُ وَآخِرَهُ","Bismillâhi evvelehû ve âhirah.","Başında da sonunda da Allah'ın adıyla.","In the name of Allah, at its beginning and at its end.","Ebû Dâvûd, Et'ime 15; Tirmizî, Et'ime 47"),
 hdua('yemek','Yemekten sonra',"الْحَمْدُ لِلَّهِ الَّذِي أَطْعَمَنَا وَسَقَانَا وَجَعَلَنَا مُسْلِمِينَ",
      "Elhamdü lillâhillezî et'amenâ ve sekânâ ve cealenâ müslimîn.","Bizi yediren, içiren ve bizi Müslüman kılan Allah'a hamd olsun.",
      "Praise be to Allah who fed us, gave us drink and made us Muslims.","Ebû Dâvûd, Et'ime 52; Tirmizî, Deavât 56"),
 hdua('yolculuk','Yolculuğa çıkarken',"اللَّهُ أَكْبَرُ، اللَّهُ أَكْبَرُ، اللَّهُ أَكْبَرُ، سُبْحَانَ الَّذِي سَخَّرَ لَنَا هَٰذَا وَمَا كُنَّا لَهُ مُقْرِنِينَ وَإِنَّا إِلَىٰ رَبِّنَا لَمُنْقَلِبُونَ",
      "Allâhü ekber, Allâhü ekber, Allâhü ekber. Sübhânellezî sahhara lenâ hâzâ ve mâ künnâ lehû mukrinîn, ve innâ ilâ rabbinâ le-munkalibûn.",
      "Allah en büyüktür (üç defa). Bunu bizim hizmetimize veren Allah'ı tesbih ederiz; yoksa biz buna güç yetiremezdik. Şüphesiz biz Rabbimize döneceğiz.",
      "Allah is the Greatest (three times). Glory be to Him who has subjected this to us, and we could not have done it by ourselves; and to our Lord we shall surely return.","Müslim, Hac 425; Zuhruf 43:13-14"),
 hdua('yolculuk','Bir yere konaklayınca',"أَعُوذُ بِكَلِمَاتِ اللَّهِ التَّامَّاتِ مِنْ شَرِّ مَا خَلَقَ",
      "Eûzü bi-kelimâtillâhi't-tâmmâti min şerri mâ halak.","Yarattığı şeylerin şerrinden Allah'ın eksiksiz kelimelerine sığınırım. (Bir yere konaklayan bunu söylerse oradan ayrılıncaya kadar ona bir şey zarar vermez.)",
      "I seek refuge in the perfect words of Allah from the evil of what He has created.","Müslim, Zikir 54"),
 hdua('sifa','Hastaya şifa duası',"اللَّهُمَّ رَبَّ النَّاسِ، أَذْهِبِ الْبَأْسَ، اشْفِ أَنْتَ الشَّافِي، لَا شِفَاءَ إِلَّا شِفَاؤُكَ، شِفَاءً لَا يُغَادِرُ سَقَمًا",
      "Allâhümme rabbe'n-nâs, ezhibi'l-be's, işfi ente'ş-şâfî, lâ şifâe illâ şifâük, şifâen lâ yuğâdiru sekamâ.",
      "Allah'ım! Ey insanların Rabbi! Sıkıntıyı gider, şifa ver. Şifa veren Sensin. Senin şifandan başka şifa yoktur. Hiçbir hastalık bırakmayan bir şifa ver.",
      "O Allah, Lord of mankind, remove the harm and heal, for You are the Healer. There is no healing but Your healing, a healing that leaves no illness behind.","Buhârî, Merdâ 20; Müslim, Selâm 46"),
 qdua('sifa',21,83,None,"Ennî messeniye'd-durru ve ente erhamü'r-râhimîn.","Hz. Eyyûb'un duası"),
 qdua('rizik',2,201,None,"Rabbenâ âtinâ fi'd-dünyâ haseneten ve fi'l-âhirati haseneten ve kınâ azâbe'n-nâr.","Dünya ve ahiret iyiliği"),
 qdua('rizik',28,24,None,"Rabbi innî limâ enzelte ileyye min hayrin fakîr.","Hz. Mûsâ'nın duası"),
 hdua('rizik','Faydalı ilim ve helal rızık',"اللَّهُمَّ إِنِّي أَسْأَلُكَ عِلْمًا نَافِعًا، وَرِزْقًا طَيِّبًا، وَعَمَلًا مُتَقَبَّلًا",
      "Allâhümme innî es'elüke ilmen nâfian ve rizkan tayyiben ve amelen mütekabbelâ.","Allah'ım! Senden faydalı ilim, helal ve temiz rızık ve kabul edilmiş amel isterim.",
      "O Allah, I ask You for beneficial knowledge, good provision and accepted deeds.","İbn Mâce, İkâmet 32"),
 qdua('aile',25,74,None,"Rabbenâ heb lenâ min ezvâcinâ ve zürriyyâtinâ kurrete a'yünin vec'alnâ li'l-müttekîne imâmâ.","Eş ve çocuklar için"),
 qdua('aile',14,40,None,"Rabbi'c'alnî mukîme's-salâti ve min zürriyyetî, rabbenâ ve tekabbel duâ.","Namaz kılan bir nesil için"),
 qdua('anne_baba',17,24,None,"Rabbi'rhamhümâ kemâ rabbeyânî sağîrâ.","Anne-babaya merhamet duası"),
 qdua('anne_baba',14,41,None,"Rabbenağfir lî ve li-vâlideyye ve li'l-mü'minîne yevme yekûmü'l-hisâb.","Hz. İbrahim'in duası"),
 qdua('anne_baba',71,28,None,"Rabbiğfir lî ve li-vâlideyye ve li-men dehale beytiye mü'minen ve li'l-mü'minîne ve'l-mü'minât...","Hz. Nûh'un duası"),
 hdua('sikinti','Sıkıntı anında',"لَا إِلَٰهَ إِلَّا اللَّهُ الْعَظِيمُ الْحَلِيمُ، لَا إِلَٰهَ إِلَّا اللَّهُ رَبُّ الْعَرْشِ الْعَظِيمِ، لَا إِلَٰهَ إِلَّا اللَّهُ رَبُّ السَّمَاوَاتِ وَرَبُّ الْأَرْضِ وَرَبُّ الْعَرْشِ الْكَرِيمِ",
      "Lâ ilâhe illallâhü'l-azîmü'l-halîm, lâ ilâhe illallâhü rabbü'l-arşi'l-azîm, lâ ilâhe illallâhü rabbü's-semâvâti ve rabbü'l-ardı ve rabbü'l-arşi'l-kerîm.",
      "Azîm ve Halîm olan Allah'tan başka ilah yoktur. Büyük Arş'ın Rabbi Allah'tan başka ilah yoktur. Göklerin Rabbi, yerin Rabbi ve kerîm Arş'ın Rabbi Allah'tan başka ilah yoktur.",
      "There is no god but Allah, the Magnificent, the Forbearing. There is no god but Allah, Lord of the Mighty Throne. There is no god but Allah, Lord of the heavens, Lord of the earth and Lord of the Noble Throne.","Buhârî, Deavât 27; Müslim, Zikir 83"),
 hdua('sikinti','Kaygı ve üzüntüden sığınma',"اللَّهُمَّ إِنِّي أَعُوذُ بِكَ مِنَ الْهَمِّ وَالْحَزَنِ، وَالْعَجْزِ وَالْكَسَلِ، وَالْبُخْلِ وَالْجُبْنِ، وَضَلَعِ الدَّيْنِ وَغَلَبَةِ الرِّجَالِ",
      "Allâhümme innî eûzü bike mine'l-hemmi ve'l-hazen, ve'l-aczi ve'l-kesel, ve'l-buhli ve'l-cübn, ve dala'i'd-deyni ve ğalebeti'r-ricâl.",
      "Allah'ım! Kaygıdan ve üzüntüden, acizlikten ve tembellikten, cimrilikten ve korkaklıktan, borcun ağırlığından ve insanların baskısından Sana sığınırım.",
      "O Allah, I seek refuge in You from anxiety and grief, from incapacity and laziness, from miserliness and cowardice, from the burden of debt and from being overpowered by men.","Buhârî, Deavât 36, 40"),
 qdua('sikinti',21,87,None,"Lâ ilâhe illâ ente sübhâneke innî küntü mine'z-zâlimîn.","Hz. Yûnus'un duası"),
 qdua('sikinti',3,173,None,"...Hasbünallâhü ve ni'me'l-vekîl.","Allah bize yeter"),
 hdua('uyku','Uyumadan önce',"بِاسْمِكَ اللَّهُمَّ أَمُوتُ وَأَحْيَا","Bismike'llâhümme emûtü ve ahyâ.","Allah'ım! Senin adınla ölür (uyur) ve dirilirim (uyanırım).",
      "In Your name, O Allah, I die and I live.","Buhârî, Deavât 7, 8"),
 hdua('uyku','Yatağa girince',"اللَّهُمَّ قِنِي عَذَابَكَ يَوْمَ تَبْعَثُ عِبَادَكَ","Allâhümme kınî azâbeke yevme teb'asü ibâdek.","Allah'ım! Kullarını dirilteceğin gün beni azabından koru.",
      "O Allah, protect me from Your punishment on the Day You resurrect Your servants.","Ebû Dâvûd, Edeb 98; Tirmizî, Deavât 18"),
 hdua('namaz','Selam verdikten sonra',"اللَّهُمَّ أَنْتَ السَّلَامُ وَمِنْكَ السَّلَامُ، تَبَارَكْتَ يَا ذَا الْجَلَالِ وَالْإِكْرَامِ",
      "Allâhümme ente's-selâmü ve minke's-selâm, tebârakte yâ ze'l-celâli ve'l-ikrâm.","Allah'ım! Sen selamsın, esenlik Sendendir. Ey celal ve ikram sahibi, Sen yücesin.",
      "O Allah, You are Peace and from You is peace. Blessed are You, O Possessor of Majesty and Honour.","Müslim, Mesâcid 136"),
 hdua('namaz','Namazdan sonra tesbihat',"سُبْحَانَ اللَّهِ (٣٣) الْحَمْدُ لِلَّهِ (٣٣) اللَّهُ أَكْبَرُ (٣٣) لَا إِلَٰهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلَىٰ كُلِّ شَيْءٍ قَدِيرٌ",
      "Sübhânallâh (33), Elhamdülillâh (33), Allâhü ekber (33). Lâ ilâhe illallâhü vahdehû lâ şerîke leh, lehü'l-mülkü ve lehü'l-hamdü ve hüve alâ külli şey'in kadîr.",
      "33 defa Allah'ı tesbih, 33 defa hamd, 33 defa tekbir; yüzü 'Allah'tan başka ilah yoktur, O tektir, ortağı yoktur; mülk O'nundur, hamd O'na aittir ve O her şeye kadirdir' sözüyle tamamlamak.",
      "Glorify Allah 33 times, praise Him 33 times, magnify Him 33 times, and complete the hundred with: There is no god but Allah alone, without partner; His is the dominion and His is the praise, and He has power over all things.","Müslim, Mesâcid 146"),
 hdua('namaz','Âyetü\'l-Kürsî',ayah(2,255)['ar'],"Allâhü lâ ilâhe illâ hüve'l-hayyü'l-kayyûm...",ayah(2,255)['tr'],ayah(2,255)['en'],"Kur'an-ı Kerim, Bakara 255; namaz sonrası okunması: Nesâî, es-Sünenü'l-kübrâ, Amelü'l-yevm 9928"),
 qdua('peygamber',2,201,None,"Rabbenâ âtinâ fi'd-dünyâ haseneten ve fi'l-âhirati haseneten ve kınâ azâbe'n-nâr.","Hz. Peygamber'in en çok yaptığı dua (Buhârî, Deavât 55)"),
 hdua('peygamber','Kalbin dinde sebatı',"يَا مُقَلِّبَ الْقُلُوبِ ثَبِّتْ قَلْبِي عَلَىٰ دِينِكَ","Yâ mukallibe'l-kulûb, sebbit kalbî alâ dînik.","Ey kalpleri halden hale çeviren! Kalbimi dinin üzere sabit kıl.",
      "O Turner of hearts, keep my heart firm upon Your religion.","Tirmizî, Kader 7; Deavât 89"),
 hdua('peygamber','Hidayet ve takva',"اللَّهُمَّ إِنِّي أَسْأَلُكَ الْهُدَىٰ وَالتُّقَىٰ وَالْعَفَافَ وَالْغِنَىٰ","Allâhümme innî es'elüke'l-hüdâ ve't-tükâ ve'l-afâfe ve'l-ğınâ.",
      "Allah'ım! Senden hidayet, takva, iffet ve gönül zenginliği isterim.","O Allah, I ask You for guidance, piety, chastity and self-sufficiency.","Müslim, Zikir 72"),
 qdua('peygamber',20,114,None,"...Rabbi zidnî ilmâ.","İlim artışı için"),
 qdua('peygamber',3,8,None,"Rabbenâ lâ tüziğ kulûbenâ ba'de iz hedeytenâ ve heb lenâ min ledünke rahmeh, inneke ente'l-vehhâb.","Hidayet üzere sebat"),
 hdua('ramazan','İftar ederken',"ذَهَبَ الظَّمَأُ وَابْتَلَّتِ الْعُرُوقُ وَثَبَتَ الْأَجْرُ إِنْ شَاءَ اللَّهُ","Zehebe'z-zameü ve'btelleti'l-urûku ve sebete'l-ecru inşâallâh.",
      "Susuzluk gitti, damarlar ıslandı ve inşallah sevap sabit oldu.","The thirst is gone, the veins are moistened and the reward is confirmed, if Allah wills.","Ebû Dâvûd, Savm 22"),
 hdua('ramazan','Kadir Gecesi duası',"اللَّهُمَّ إِنَّكَ عَفُوٌّ تُحِبُّ الْعَفْوَ فَاعْفُ عَنِّي","Allâhümme inneke afüvvün tühibbü'l-afve fa'fü annî.",
      "Allah'ım! Sen affedicisin, affetmeyi seversin; beni affet.","O Allah, You are Pardoning and love to pardon, so pardon me.","Tirmizî, Deavât 84; İbn Mâce, Dua 5"),
 qdua('ramazan',2,186,None,"Ve izâ seeleke ıbâdî annî fe-innî karîb...","Allah kullarına yakındır (oruç ayetleri arasında)"),
]
for d in D:
    if 'ar' not in d or not d['ar']: raise SystemExit('eksik arapça: ' + d['title'])
for i, d in enumerate(D): d['id'] = i + 1
dump('dualar', {'cats': [{'id': c, 'tr': t, 'en': e} for c, t, e in CATS], 'items': D})

dump('esma', [{'n': i + 1, 'ar': a, 'okunus': o, 'tr': t, 'en': e} for i, (a, o, t, e) in enumerate(ESMA)])

H = [
 ("Ameller ancak niyetlere göredir. Herkese ancak niyet ettiği şey vardır.","Actions are only by intentions, and every person will have only what they intended.","Buhârî, Bed'ü'l-vahy 1; Müslim, İmâre 155"),
 ("Sizin en hayırlınız Kur'an'ı öğrenen ve öğretendir.","The best of you are those who learn the Qur'an and teach it.","Buhârî, Fezâilü'l-Kur'ân 21"),
 ("Kolaylaştırınız, zorlaştırmayınız; müjdeleyiniz, nefret ettirmeyiniz.","Make things easy and do not make them difficult; give glad tidings and do not drive people away.","Buhârî, İlim 11; Müslim, Cihâd 8"),
 ("Müslüman, dilinden ve elinden Müslümanların güvende olduğu kimsedir.","The Muslim is the one from whose tongue and hand the Muslims are safe.","Buhârî, Îmân 4; Müslim, Îmân 64"),
 ("Sizden biriniz kendisi için istediğini kardeşi için de istemedikçe (gerçek anlamda) iman etmiş olmaz.","None of you truly believes until he loves for his brother what he loves for himself.","Buhârî, Îmân 7; Müslim, Îmân 71"),
 ("Allah'a ve ahiret gününe iman eden ya hayır söylesin ya da sussun.","Whoever believes in Allah and the Last Day, let him speak good or remain silent.","Buhârî, Edeb 31; Müslim, Îmân 74"),
 ("Temizlik imanın yarısıdır.","Purity is half of faith.","Müslim, Tahâret 1"),
 ("Güzel söz sadakadır.","A good word is charity.","Buhârî, Cihâd 128; Müslim, Zekât 56"),
 ("Merhamet etmeyene merhamet olunmaz.","Whoever does not show mercy will not be shown mercy.","Buhârî, Edeb 18; Müslim, Fezâil 65"),
 ("Güçlü kimse güreşte rakibini yenen değil, öfkelendiği zaman kendine hâkim olandır.","The strong one is not the one who overcomes others in wrestling, but the one who controls himself when angry.","Buhârî, Edeb 76; Müslim, Birr 107"),
 ("Din nasihattir (samimiyettir).","The religion is sincerity (naṣīḥa).","Müslim, Îmân 95"),
 ("Kardeşinin yüzüne gülümsemen senin için bir sadakadır.","Your smiling in the face of your brother is charity for you.","Tirmizî, Birr 36"),
 ("Allah katında amellerin en sevimlisi, az da olsa devamlı olanıdır.","The most beloved deeds to Allah are those done regularly, even if they are few.","Buhârî, Rikâk 18; Müslim, Müsâfirîn 218"),
 ("İki nimet vardır ki insanların çoğu bunlarda aldanmıştır: sağlık ve boş vakit.","There are two blessings which many people lose: health and free time.","Buhârî, Rikâk 1"),
 ("Birinizin kapısının önünde bir nehir olsa ve her gün o nehirde beş defa yıkansa, üzerinde kir kalır mı? İşte beş vakit namaz da böyledir; Allah onlarla günahları siler.","If there were a river at the door of one of you in which he bathed five times a day, would any dirt remain on him? That is the likeness of the five prayers, by which Allah wipes away sins.","Buhârî, Mevâkît 6; Müslim, Mesâcid 283"),
 ("Oruç bir kalkandır.","Fasting is a shield.","Buhârî, Savm 2; Müslim, Sıyâm 162"),
 ("Kim inanarak ve sevabını Allah'tan umarak Ramazan orucunu tutarsa geçmiş günahları bağışlanır.","Whoever fasts Ramadan out of faith and seeking reward, his previous sins will be forgiven.","Buhârî, Îmân 28; Müslim, Müsâfirîn 175"),
 ("Hayâ imandandır.","Modesty is part of faith.","Buhârî, Îmân 16; Müslim, Îmân 57"),
 ("Mümin mümin için, parçaları birbirini tutan bir bina gibidir.","The believer to another believer is like a building whose parts support one another.","Buhârî, Salât 88; Müslim, Birr 65"),
 ("İnsan ölünce ameli kesilir; ancak üç şey müstesna: sadaka-i câriye, faydalanılan ilim ve kendisine dua eden hayırlı evlat.","When a person dies, his deeds come to an end except for three: ongoing charity, beneficial knowledge, or a righteous child who prays for him.","Müslim, Vasiyyet 14"),
 ("İnsanlara teşekkür etmeyen Allah'a şükretmiş olmaz.","Whoever does not thank people has not thanked Allah.","Ebû Dâvûd, Edeb 11; Tirmizî, Birr 35"),
 ("Allah sizin suretlerinize ve mallarınıza bakmaz; kalplerinize ve amellerinize bakar.","Allah does not look at your appearance or your wealth, but He looks at your hearts and your deeds.","Müslim, Birr 34"),
 ("Dünyada bir garip veya bir yolcu gibi ol.","Be in this world as though you were a stranger or a traveller.","Buhârî, Rikâk 3"),
 ("Kim bir hayra öncülük ederse, onu yapanın sevabı kadar sevap alır.","Whoever guides to good will have a reward like that of the one who does it.","Müslim, İmâre 133"),
 ("Kişi sevdiği ile beraberdir.","A person will be with those whom he loves.","Buhârî, Edeb 96; Müslim, Birr 165"),
 ("Zulümden sakının; çünkü zulüm kıyamet gününde karanlıklardır.","Beware of injustice, for injustice will be darkness on the Day of Resurrection.","Buhârî, Mezâlim 8; Müslim, Birr 56"),
 ("Bir Müslümanın, kardeşine üç günden fazla küs durması helal değildir.","It is not lawful for a Muslim to shun his brother for more than three days.","Buhârî, Edeb 62; Müslim, Birr 25"),
 ("Sadaka malı eksiltmez.","Charity does not decrease wealth.","Müslim, Birr 69"),
 ("Kim ilim öğrenmek için bir yola girerse, Allah ona cennete giden yolu kolaylaştırır.","Whoever takes a path seeking knowledge, Allah will make easy for him a path to Paradise.","Müslim, Zikir 38"),
 ("Allah güzeldir, güzeli sever.","Allah is beautiful and loves beauty.","Müslim, Îmân 147"),
 ("Sizin en hayırlılarınız ahlakı en güzel olanlarınızdır.","The best of you are those with the best character.","Buhârî, Menâkıb 23; Müslim, Fezâil 68"),
]
dump('hadis', [{'tr': t, 'en': e, 'src': s} for t, e, s in H])

AYET = [(2,152),(2,153),(2,186),(2,286),(3,139),(3,159),(13,28),(14,7),(16,97),(16,128),(17,23),(20,114),(29,45),(29,69),(39,53),(40,60),
        (49,10),(49,13),(50,16),(57,4),(65,3),(93,5),(94,5),(94,6),(103,3),(112,1),(2,45),(3,200),(5,8),(6,162),(7,56),(21,87),(25,63),(31,17),
        (47,7),(59,18),(67,2),(2,255),(3,190),(4,36)]
dump('ayet', [ayah(s, a) | {'s': s, 'a': a, 'name': SURAH_TR[s]} for s, a in AYET])

Z = [
 ("سُبْحَانَ اللَّهِ","Sübhânallâh","Allah her türlü noksanlıktan uzaktır.","Glory be to Allah.",33,"Müslim, Mesâcid 146"),
 ("الْحَمْدُ لِلَّهِ","Elhamdülillâh","Hamd Allah'a mahsustur.","Praise be to Allah.",33,"Müslim, Mesâcid 146"),
 ("اللَّهُ أَكْبَرُ","Allâhü ekber","Allah en büyüktür.","Allah is the Greatest.",33,"Müslim, Mesâcid 146"),
 ("لَا إِلَٰهَ إِلَّا اللَّهُ","Lâ ilâhe illallâh","Allah'tan başka ilah yoktur.","There is no god but Allah.",100,"Tirmizî, Deavât 9"),
 ("أَسْتَغْفِرُ اللَّهَ","Estağfirullâh","Allah'tan bağışlanma dilerim.","I seek Allah's forgiveness.",100,"Müslim, Zikir 41"),
 ("سُبْحَانَ اللَّهِ وَبِحَمْدِهِ","Sübhânallâhi ve bi-hamdih","Allah'ı hamd ile tesbih ederim.","Glory be to Allah and praise be to Him.",100,"Buhârî, Deavât 65; Müslim, Zikir 28"),
 ("لَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللَّهِ","Lâ havle ve lâ kuvvete illâ billâh","Güç ve kuvvet ancak Allah'ın yardımıyladır.","There is no power and no strength except with Allah.",33,"Buhârî, Deavât 51; Müslim, Zikir 44"),
 ("اللَّهُمَّ صَلِّ عَلَىٰ مُحَمَّدٍ","Allâhümme salli alâ Muhammed","Allah'ım! Muhammed'e salât eyle.","O Allah, send blessings upon Muhammad.",100,"Müslim, Salât 70"),
 ("حَسْبُنَا اللَّهُ وَنِعْمَ الْوَكِيلُ","Hasbünallâhü ve ni'me'l-vekîl","Allah bize yeter, O ne güzel vekildir.","Allah is sufficient for us, and He is the best Disposer of affairs.",33,"Âl-i İmrân 3:173"),
]
dump('zikir', [{'ar': a, 'okunus': o, 'tr': t, 'en': e, 'count': c, 'src': s} for a, o, t, e, c, s in Z])
