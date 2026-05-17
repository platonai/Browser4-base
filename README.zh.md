# 馃 Browser4

---

[English](README.md) | 绠€浣撲腑鏂?
<!-- TOC -->
**鐩綍**
- [馃 Browser4](#-browser4)
    - [馃専 绠€浠媇(#-绠€浠?
        - [鉁?鏍稿績鑳藉姏](#-鏍稿績鑳藉姏)
    - [馃帴 婕旂ず瑙嗛](#-婕旂ず瑙嗛)
    - [馃挕 浣跨敤绀轰緥](#-浣跨敤绀轰緥)
        - [宸ヤ綔娴佽嚜鍔ㄥ寲](#宸ヤ綔娴佽嚜鍔ㄥ寲)
        - [LLM + X-SQL](#llm--x-sql)
        - [楂橀€熷苟琛屽鐞哴(#楂橀€熷苟琛屽鐞?
        - [鑷姩鎻愬彇](#鑷姩鎻愬彇)
    - [馃摝 妯″潡姒傝](#-妯″潡姒傝)
    - [馃摐 鏂囨。](#-鏂囨。)
    - [馃敡 浠ｇ悊 - 瑙ｉ櫎缃戠珯灏侀攣](#-浠ｇ悊---瑙ｉ櫎缃戠珯灏侀攣)
    - [鉁?鐗规€(#-鐗规€?
    - [馃 鏀寔涓庣ぞ鍖篯(#-鏀寔涓庣ぞ鍖?
<!-- /TOC -->

## 馃専 绠€浠?
馃挅 **Browser4锛氫负浣犵殑 AI 鎵撻€犵殑闂數鑸揩閫熴€佸崗绋嬪畨鍏ㄧ殑娴忚鍣ㄥ紩鎿?* 馃挅

### 鉁?鏍稿績鑳藉姏

* 馃 **娴忚鍣ㄨ嚜鍔ㄥ寲** 鈥?楂樻€ц兘鐨勫伐浣滄祦銆佸鑸拰鏁版嵁鎻愬彇鑷姩鍖栥€?* 鈿? **鏋佽嚧鎬ц兘** 鈥?瀹屽叏鍗忕▼瀹夊叏锛涙敮鎸佸崟鏈烘瘡澶?10 涓?~ 20 涓囨澶嶆潅椤甸潰璁块棶銆?* 馃К **鏁版嵁鎻愬彇** 鈥?LLM銆丮L 鍜岄€夋嫨鍣ㄧ殑娣峰悎鏂规锛屼粠娣蜂贡鐨勯〉闈腑鎻愬彇骞插噣鐨勬暟鎹€?
## 馃帴 婕旂ず瑙嗛

馃幀 YouTube:
[![瑙傜湅瑙嗛](https://img.youtube.com/vi/rJzXNXH3Gwk/0.jpg)](https://youtu.be/rJzXNXH3Gwk)

馃摵 Bilibili:
[https://www.bilibili.com/video/BV1fXUzBFE4L](https://www.bilibili.com/video/BV1fXUzBFE4L)

---

## 馃挕 浣跨敤绀轰緥

### 宸ヤ綔娴佽嚜鍔ㄥ寲

搴曞眰娴忚鍣ㄨ嚜鍔ㄥ寲鍜屾暟鎹彁鍙栵紝鎻愪緵缁嗙矑搴︽帶鍒躲€?
**鐗规€э細**
- 鍚屾椂鏀寔瀹炴椂 DOM 璁块棶鍜岀绾垮揩鐓цВ鏋?- 鐩存帴涓斿畬鏁寸殑 Chrome DevTools Protocol (CDP) 鎺у埗锛屽崗绋嬪畨鍏?- 绮剧‘鐨勫厓绱犱氦浜掞紙鐐瑰嚮銆佹粴鍔ㄣ€佽緭鍏ワ級
- 浣跨敤 CSS 閫夋嫨鍣?XPath 蹇€熸彁鍙栨暟鎹?
```kotlin
val session = AgenticContexts.getOrCreateSession()
val agent = session.companionAgent
val driver = session.getOrCreateBoundDriver()

// 鍔犺浇杈撳叆 URL 鎵€寮曠敤鐨勫垵濮嬮〉闈?var page = session.open(url)

// 浣跨敤鑷劧璇█鎸囦护椹卞姩娴忚鍣?agent.act("婊氬姩鍒拌瘎璁哄尯")
// 鐩存帴浠庡疄鏃?DOM 涓鍙栫涓€涓尮閰嶇殑璇勮鑺傜偣
val content = driver.selectFirstTextOrNull("#comments")

// 灏嗛〉闈㈠揩鐓т繚瀛樺埌鍐呭瓨鏂囨。涓互渚涚绾胯В鏋?var document = session.parse(page)
// 涓€娆℃€у皢 CSS 閫夋嫨鍣ㄦ槧灏勫埌缁撴瀯鍖栧瓧娈?var fields = session.extract(document, mapOf("title" to "#title"))

// 璁╀即闅忎唬鐞嗘墽琛屽姝ラ瀵艰埅/鎼滅储娴佺▼
val history = agent.run(
    "鍓嶅線 amazon.com锛屾悳绱?'smart phone'锛屾墦寮€璇勫垎鏈€楂樼殑鍟嗗搧椤甸潰"
)

// 灏嗘洿鏂板悗鐨勬祻瑙堝櫒鐘舵€佹崟鑾峰洖 PageSnapshot
page = session.capture(driver)
document = session.parse(page)
// 浠庢崟鑾风殑蹇収涓彁鍙栭澶栫殑灞炴€?fields = session.extract(document, mapOf("ratings" to "#ratings"))
```

### LLM + X-SQL

闈炲父閫傚悎楂樺鏉傚害鐨勬暟鎹彁鍙栨祦姘寸嚎锛屾秹鍙婃暟鍗佷釜瀹炰綋銆佹瘡涓疄浣撴暟鐧句釜瀛楁銆?
**浼樺娍锛?*
- 鐩告瘮浼犵粺鏂规硶锛岃兘澶熸彁鍙?10 鍊嶄互涓婄殑瀹炰綋鍜?100 鍊嶄互涓婄殑瀛楁
- 灏?LLM 鏅鸿兘涓庣簿纭殑 CSS 閫夋嫨鍣?XPath 鐩哥粨鍚?- 绫?SQL 璇硶锛岀啛鎮夌殑鏌ヨ鏂瑰紡

```kotlin
val context = AgenticContexts.create()
val sql = """
select
  llm_extract(dom, 'product name, price, ratings') as llm_extracted_data,
  dom_first_text(dom, '#productTitle') as title,
  dom_first_text(dom, '#bylineInfo') as brand,
  dom_first_text(dom, '#price tr td:matches(^Price) ~ td, #corePrice_desktop tr td:matches(^Price) ~ td') as price,
  dom_first_text(dom, '#acrCustomerReviewText') as ratings,
  str_first_float(dom_first_text(dom, '#reviewsMedley .AverageCustomerReviews span:contains(out of)'), 0.0) as score
from load_and_select('https://www.amazon.com/dp/B08PP5MSVB -i 1s -njr 3', 'body');
"""
val rs = context.executeQuery(sql)
println(ResultSetFormatter(rs, withHeader = true))
```

绀轰緥浠ｇ爜锛?
* [浣跨敤 X-SQL 浠庝簹椹€婂晢鍝侀〉闈㈡姄鍙?100+ 瀛楁](https://github.com/platonai/exotic-amazon/tree/main/src/main/resources/sites/amazon/crawl/parse/sql/crawl)
* [浣跨敤 X-SQL 鎶撳彇鎵€鏈夌被鍨嬬殑浜氶┈閫婄綉椤礭(https://github.com/platonai/exotic-amazon/tree/main/src/main/resources/sites/amazon/crawl/parse/sql/crawl)

### 楂橀€熷苟琛屽鐞?
閫氳繃骞惰娴忚鍣ㄦ帶鍒跺拰鏅鸿兘璧勬簮浼樺寲瀹炵幇鏋佽嚧鍚炲悙閲忋€?
**鎬ц兘锛?*
- 鍗曟満姣忓ぉ 1 涓?~ 2 涓囨澶嶆潅椤甸潰璁块棶
- 骞跺彂浼氳瘽绠＄悊
- 璧勬簮鎷︽埅浠ュ姞蹇〉闈㈠姞杞?
```kotlin
val args = "-refresh -dropContent -interactLevel fastest"
val blockingUrls = listOf("*.png", "*.jpg")
val links = LinkExtractors.fromResource("urls.txt")
    .map { ListenableHyperlink(it, "", args = args) }
    .onEach {
        it.eventHandlers.browseEventHandlers.onWillNavigate.addLast { page, driver ->
            driver.addBlockedURLs(blockingUrls)
        }
    }

session.submitAll(links)
```

馃幀 YouTube:
[![瑙傜湅瑙嗛](https://img.youtube.com/vi/_BcryqWzVMI/0.jpg)](https://www.youtube.com/watch?v=_BcryqWzVMI)

馃摵 Bilibili:
[https://www.bilibili.com/video/BV1kM2rYrEFC](https://www.bilibili.com/video/BV1kM2rYrEFC)


---

### 鑷姩鎻愬彇

鍩轰簬鑷洃鐫?鏃犵洃鐫ｆ満鍣ㄥ涔犵殑鑷姩銆佸ぇ瑙勬ā銆侀珮绮惧害瀛楁鍙戠幇涓庢彁鍙?鈥?鏃犻渶 LLM API 璋冪敤锛岄浂 Token锛岀‘瀹氭€т笖蹇€熴€?
**瀹冭兘鍋氫粈涔堬細**
- 楂樼簿搴﹀涔犲晢鍝?璇︽儏椤典笂鐨勬瘡涓€涓彲鎻愬彇瀛楁锛堥€氬父鏁板崄鍒版暟鐧句釜锛夈€?- 褰?Browser4 鍦?GitHub 涓婅揪鍒?10K stars 鏃跺紑婧愩€?
**涓轰粈涔堜笉浠呬粎浣跨敤 LLM锛?*
- LLM 鎻愬彇浼氬鍔犲欢杩熴€佹垚鏈拰 Token 闄愬埗銆?- 鍩轰簬 ML 鐨勮嚜鍔ㄦ彁鍙栨槸鏈湴鍖栥€佸彲澶嶇幇鐨勶紝骞跺彲鎵╁睍鍒版瘡澶?10 涓?~ 20 涓囬〉闈€?- 浣犱篃鍙互灏嗕袱鑰呯粨鍚堬細浣跨敤鑷姩鎻愬彇鑾峰彇缁撴瀯鍖栧熀绾挎暟鎹?+ LLM 杩涜璇箟澧炲己銆?
**蹇€熷懡浠わ紙PulsarRPAPro锛夛細**
```bash
# 娉ㄦ剰锛氶渶瑕?MongoDB
curl -L -o PulsarRPAPro.jar https://github.com/platonai/PulsarRPAPro/releases/download/v4.7.7/PulsarRPAPro.jar
```

**闆嗘垚鐘舵€侊細**
- 褰撳墠鍙€氳繃閰嶅椤圭洰 [PulsarRPAPro](https://github.com/platonai/PulsarRPAPro) 浣跨敤銆?- 鍘熺敓鐨?Browser4 API 鎺ュ叆姝ｅ湪瑙勫垝涓紱璇峰叧娉ㄥ彂甯冨姩鎬併€?
**鏍稿績浼樺娍锛?*
- 楂樼簿搴︼細>95% 鐨勫瓧娈佃鍙戠幇锛涘叾涓ぇ澶氭暟鍑嗙‘鐜?>99%锛堝湪娴嬭瘯鍩熷悕涓婄殑鍙傝€冨€硷級銆?- 瀵归€夋嫨鍣ㄥ彉鍖栧拰 HTML 鍣０鍏锋湁闊ф€с€?- 闆跺閮ㄤ緷璧栵紙鏃犻渶 API Key锛夆啋 澶ц妯′娇鐢ㄦ椂鎴愭湰鏁堢泭楂樸€?- 鍙В閲婏細鐢熸垚鐨勯€夋嫨鍣ㄥ拰 SQL 閫忔槑涓斿彲瀹¤銆?
馃懡 浣跨敤鏈哄櫒瀛︿範浠ｇ悊鎻愬彇鏁版嵁锛?
![鑷姩鎻愬彇缁撴灉蹇収](docs/assets/images/amazon.png)

锛堝嵆灏嗘帹鍑猴細鏇翠赴瀵岀殑浠撳簱鍐呯ず渚嬪拰鐩存帴鐨?API 鎺ュ彛銆傦級

---

---

## 鉁?鐗规€?
鐘舵€侊細[鍙敤] 宸插寘鍚湪浠撳簱涓紝[瀹為獙鎬 姝ｅ湪绉瀬杩唬涓紝[璁″垝涓璢 灏氭湭鍔犲叆浠撳簱锛孾鍙傝€僝 鎬ц兘鐩爣銆?
### 娴忚鍣ㄨ嚜鍔ㄥ寲涓?RPA
- [鍙敤] 鍩轰簬宸ヤ綔娴佺殑娴忚鍣ㄦ搷浣?- [鍙敤] 绮剧‘鐨勫崗绋嬪畨鍏ㄦ帶鍒讹紙婊氬姩銆佺偣鍑汇€佹彁鍙栵級
- [鍙敤] 鐏垫椿鐨勪簨浠跺鐞嗗櫒鍜岀敓鍛藉懆鏈熺鐞?
### 鏁版嵁鎻愬彇涓庢煡璇?- [鍙敤] 涓€琛屽懡浠ゅ畬鎴愭暟鎹彁鍙?- [鍙敤] 闈㈠悜 DOM/鍐呭鐨?X-SQL 鎵╁睍鏌ヨ璇█
- [瀹為獙鎬 缁撴瀯鍖栦笌闈炵粨鏋勫寲娣峰悎鎻愬彇锛圠LM & ML & 閫夋嫨鍣級

### 鎬ц兘涓庡彲鎵╁睍鎬?- [鍙敤] 楂樻晥鐨勫苟琛岄〉闈㈡覆鏌?- [鍙敤] 鍙嶅皝閿佽璁″拰鏅鸿兘閲嶈瘯
- [鍙傝€僝 鍦ㄦ櫘閫氱‖浠朵笂姣忓ぉ澶勭悊 10 涓? 澶嶆潅椤甸潰

### 闅愬尶涓庡彲闈犳€?- [瀹為獙鎬 楂樼骇鍙嶆満鍣ㄤ汉鎶€鏈?- [鍙敤] 閫氳繃 `PROXY_ROTATION_URL` 杩涜浠ｇ悊杞崲
- [鍙敤] 寮规€ц皟搴︿笌璐ㄩ噺淇濊瘉

### 寮€鍙戣€呬綋楠?- [鍙敤] 绠€娲佺殑 API 闆嗘垚锛圧EST銆佸師鐢熴€佹枃鏈懡浠わ級
- [鍙敤] 涓板瘜鐨勯厤缃垎灞?- [鍙敤] 娓呮櫚鐨勭粨鏋勫寲鏃ュ織鍜屾寚鏍?
### 瀛樺偍涓庣洃鎺?- [鍙敤] 鏀寔鏈湴鏂囦欢绯荤粺鍜?MongoDB锛堝彲鎵╁睍锛?- [鍙敤] 鍏ㄩ潰鐨勬棩蹇椾笌閫忔槑搴?
---

## 馃 鏀寔涓庣ぞ鍖?
鍔犲叆鎴戜滑鐨勭ぞ鍖猴紝鑾峰彇鏀寔銆佸弽棣堝拰鍚堜綔锛?
- **GitHub Discussions**锛氫笌寮€鍙戣€呭拰鍏朵粬鐢ㄦ埛浜ゆ祦銆?- **Issue Tracker**锛氭姤鍛?Bug 鎴栬姹傛柊鍔熻兘銆?- **绀句氦濯掍綋**锛氬叧娉ㄦ垜浠幏鍙栨渶鏂板姩鎬併€?
娆㈣繋璐＄尞锛佽鎯呰鍙傞槄 [CONTRIBUTING.md](CONTRIBUTING.md)銆?
---

## 馃摐 鏂囨。

瀹屾暣鏂囨。鍙湪 `docs/` 鐩綍鍜屾垜浠殑 [GitHub Pages 绔欑偣](https://platonai.github.io/browser4/) 涓煡闃呫€?
---

## 馃敡 浠ｇ悊閰嶇疆 - 瑙ｉ櫎缃戠珯灏侀攣

<details>

璁剧疆鐜鍙橀噺 `PROXY_ROTATION_URL` 涓轰綘鐨勪唬鐞嗘湇鍔″晢鎻愪緵鐨勮疆鎹?URL锛?
```shell
export PROXY_ROTATION_URL=https://your-proxy-provider.com/rotation-endpoint
```

姣忔璁块棶姝よ疆鎹?URL 鏃讹紝搴旇繑鍥炰竴涓垨澶氫釜鏂扮殑浠ｇ悊 IP銆?濡傛灉浣犻渶瑕佹绫?URL锛岃鑱旂郴浣犵殑浠ｇ悊鏈嶅姟鍟嗐€?
</details>

---

## 璁稿彲璇?
Apache 2.0 License銆傝鎯呰鍙傞槄 [LICENSE](LICENSE)銆?

