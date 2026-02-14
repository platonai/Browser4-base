package ai.platon.pulsar.sdk.e2e.sites

import ai.platon.pulsar.sdk.integration.KotlinSdkIntegrationTestBase
import ai.platon.pulsar.sdk.v0.AgenticSession
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

@Tag("E2ETest")
@Tag("RequiresServer")
@Tag("Slow")
@Tag("ManualOnly")
@Disabled("ManualOnly")
class XiaoHongShuE2ETest : KotlinSdkIntegrationTestBase() {

    /**
     * ## XHS-UX-001 发布与编辑：可用性与可恢复
     *
     * **目标**：验证发布流程顺畅、编辑入口可发现、保存反馈明确
     *
     * **步骤（3~5）**
     *
     * 1. 进入发布页，选择 1 张图片并做一次轻量编辑（滤镜/贴纸二选一）
     * 2. 输入文案与 1~3 个话题，点击发布
     * 3. 进入个人主页打开刚发布的笔记，从“更多/…”进入编辑
     * 4. 修改 1 处文案并删除 1 个话题，保存
     *
     * **期望结果**
     *
     * - 发布/保存均有明确反馈（成功/失败原因/重试）
     * - 编辑入口不隐蔽，命名与位置稳定
     * - 修改后内容在详情页与个人页同步展示
     */
    @Test
    fun testPublishAndEdit() = runBlocking {
        createSession()
        val session = AgenticSession(client)
        val driver = session.driver

        // 1. 进入发布页，选择 1 张图片并做一次轻量编辑（滤镜/贴纸二选一）
        driver.navigateTo("https://www.xiaohongshu.com/publish")
        var result = session.run("Select 1 image and apply a filter or sticker")
        assertTrue(result.success, "Failed to select image and apply filter: ${result.message}")

        // 2. 输入文案与 1~3 个话题，点击发布
        result = session.run("Input text content and 1-3 topics, then click publish")
        assertTrue(result.success, "Failed to publish note: ${result.message}")

        // 3. 进入个人主页打开刚发布的笔记，从“更多/…”进入编辑
        driver.navigateTo("https://www.xiaohongshu.com/user/profile")
        result = session.run("Open the latest published note, then go to edit mode from 'More' menu")
        assertTrue(result.success, "Failed to enter edit mode: ${result.message}")

        // 4. 修改 1 处文案并删除 1 个话题，保存
        result = session.run("Modify the text and remove 1 topic, then save")
        assertTrue(result.success, "Failed to save changes: ${result.message}")
    }

    /**
     * ## XHS-UX-002 搜索与发现：相关性与排序可解释
     *
     * **目标**：验证搜索结果相关性、排序/筛选入口清晰、发现频道结构合理
     *
     * **步骤（3~5）**
     *
     * 1. 在首页搜索框输入明确意图关键词（例如“通勤穿搭 小个子”）
     * 2. 查看结果页相关性与默认排序；切换一次排序（如最热/最新）
     * 3. 返回首页连续浏览推荐流 10~15 条内容
     * 4. 进入发现/频道，打开一个垂类并浏览顶部细分标签
     *
     * **期望结果**
     *
     * - 搜索结果与关键词意图匹配，空结果有合理引导
     * - 排序/筛选入口可发现，切换后结果变化合理
     * - 频道结构清晰，返回路径不迷路
     */
    @Test
    fun testSearchAndDiscovery() = runBlocking {
        createSession()
        val session = AgenticSession(client)
        val driver = session.driver

        // 1. 在首页搜索框输入明确意图关键词（例如“通勤穿搭 小个子”）
        driver.navigateTo("https://www.xiaohongshu.com/")
        val searchResult = session.act("Search for '通勤穿搭 小个子'")
        assertTrue(searchResult.success, "Failed to search: ${searchResult.message}")

        // 2. 查看结果页相关性与默认排序；切换一次排序（如最热/最新）
        val sortResult = session.run("Check search results relevance, then switch sort order to 'Latest' or 'Most Popular'")
        assertTrue(sortResult.success, "Failed to check results and sort: ${sortResult.message}")

        // 3. 返回首页连续浏览推荐流 10~15 条内容
        driver.navigateTo("https://www.xiaohongshu.com/")
        val browseResult = session.run("Browse 10-15 items in the recommendation feed")
        assertTrue(browseResult.success, "Failed to browse feed: ${browseResult.message}")

        // 4. 进入发现/频道，打开一个垂类并浏览顶部细分标签
        val discoveryResult = session.run("Go to Discovery channel, open a category and browse sub-tags")
        assertTrue(discoveryResult.success, "Failed to browse discovery channel: ${discoveryResult.message}")
    }

    /**
     * ## XHS-UX-003 互动（赞/藏/评）：入口清晰与状态一致
     *
     * **目标**：确保互动响应及时、状态同步、撤销易用
     *
     * **步骤（3~5）**
     *
     * 1. 打开任意笔记详情，执行：点赞、收藏、发布一条短评论
     * 2. 回到个人页，进入“赞过/收藏/评论”相关入口
     * 3. 验证对应列表中可找到该笔记/评论
     * 4. 返回原笔记取消点赞并移除收藏
     *
     * **期望结果**
     *
     * - 互动入口清晰，点击反馈及时
     * - 状态在详情/列表/个人页一致
     * - 撤销操作生效及时，无残留状态
     */
    @Test
    fun testInteraction() = runBlocking {
        createSession()
        val session = AgenticSession(client)
        val driver = session.driver

        // 1. 打开任意笔记详情，执行：点赞、收藏、发布一条短评论
        driver.navigateTo("https://www.xiaohongshu.com/explore")
        var result = session.run("Open any note, like it, collect it, and post a short comment")
        assertTrue(result.success, "Failed to interact with note: ${result.message}")

        // 2. 回到个人页，进入“赞过/收藏/评论”相关入口
        driver.navigateTo("https://www.xiaohongshu.com/user/profile")
        result = session.run("Check 'Liked', 'Collected', and 'Commented' lists for the note")
        assertTrue(result.success, "Failed to verify interactions in profile: ${result.message}")

        // 4. 返回原笔记取消点赞并移除收藏
        result = session.run("Open the note again and unlike/uncollect it")
        assertTrue(result.success, "Failed to undo interactions: ${result.message}")
    }

    /**
     * ## XHS-UX-004 私信与客服：路径可发现与问题自助
     *
     * **目标**：验证私信入口可发现、客服/帮助中心路径清晰、反馈提交可用
     *
     * **步骤（3~5）**
     *
     * 1. 从作者主页进入“发消息”，发送一条文本
     * 2. 进入消息列表确认会话入口与未读状态可理解
     * 3. 进入“设置 → 帮助与客服”，搜索一个问题关键词（例如“账号申诉”）
     * 4. 找到“意见反馈”入口并提交一条测试反馈（不含隐私信息）
     *
     * **期望结果**
     *
     * - 消息发送与到达反馈明确，会话列表信息清晰
     * - 帮助中心可通过搜索快速定位指引
     * - 反馈提交后有成功提示与后续查看入口（如产品提供）
     */
    @Test
    fun testMessageAndService() = runBlocking {
        createSession()
        val session = AgenticSession(client)
        val driver = session.driver

        // 1. 从作者主页进入“发消息”，发送一条文本
        driver.navigateTo("https://www.xiaohongshu.com/explore")
        var result = session.run("Find a user profile, click 'Message', and send a text message")
        assertTrue(result.success, "Failed to send message: ${result.message}")

        // 2. 进入消息列表确认会话入口与未读状态可理解
        driver.navigateTo("https://www.xiaohongshu.com/notification")
        result = session.run("Check message list for the conversation")
        assertTrue(result.success, "Failed to check message list: ${result.message}")

        // 3. 进入“设置 → 帮助与客服”，搜索一个问题关键词（例如“账号申诉”）
        driver.navigateTo("https://www.xiaohongshu.com/settings")
        result = session.run("Go to Settings -> Help & Service, and search for 'Account Appeal'")
        assertTrue(result.success, "Failed to search help center: ${result.message}")

        // 4. 找到“意见反馈”入口并提交一条测试反馈（不含隐私信息）
        result = session.run("Find 'Feedback' entry and submit a test feedback")
        assertTrue(result.success, "Failed to submit feedback: ${result.message}")
    }

    /**
     * ## XHS-UX-005 内容到商品：链路连贯与退出无负担
     *
     * **目标**：验证从内容进入商品与订单确认页的连续体验（不含真实支付）
     *
     * **步骤（3~5）**
     *
     * 1. 打开一条带商品卡片/商品标签的笔记，点击商品入口
     * 2. 在商品详情页查看价格、规格、评价/关联内容等核心信息
     * 3. 选择规格进入订单确认页
     * 4. 返回退出流程，回到内容或首页
     *
     * **期望结果**
     *
     * - 商品入口清晰，不会误导用户操作
     * - 核心信息布局清晰，规格/库存/价格联动合理
     * - 退出路径明确，退出后无异常弹窗或卡死状态
     */
    @Test
    fun testContentToProduct() = runBlocking {
        createSession()
        val session = AgenticSession(client)
        val driver = session.driver

        // 1. 打开一条带商品卡片/商品标签的笔记，点击商品入口
        driver.navigateTo("https://www.xiaohongshu.com/explore")
        var result = session.run("Find a note with a product link/card and click it")
        assertTrue(result.success, "Failed to find product note: ${result.message}")

        // 2. 在商品详情页查看价格、规格、评价/关联内容等核心信息
        result = session.run("Check price, specs, and reviews on product detail page")
        assertTrue(result.success, "Failed to check product details: ${result.message}")

        // 3. 选择规格进入订单确认页
        result = session.run("Select a spec and go to order confirmation page")
        assertTrue(result.success, "Failed to go to order confirmation: ${result.message}")

        // 4. 返回退出流程，回到内容或首页
        driver.goBack()
        driver.goBack()
    }
}
