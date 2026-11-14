<div align="center">

# Reminder+

<img src="./app/src/main/res/mipmap-xxxhdpi/ic_launcher.webp" width="120px" style="border-radius:12px"/>

### 一款 Material 3 风格的安卓纪念日 / 倒数日 / 便签管理应用

![Android](https://img.shields.io/badge/Platform-Android-green?logo=android)
![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-blue)
![Material 3](https://img.shields.io/badge/Design-Material%203-6750A4)
![Version](https://img.shields.io/badge/Version-1.0.1-blue)
![License](https://img.shields.io/badge/License-GPLv3-orange)

</div>

---

## 项目简介

**Reminder+** 是一款简洁的安卓纪念日 / 倒数日管理应用，支持倒数与正数两种模式，帮助你轻松掌握重要日期。应用完全使用 **Jetpack Compose** 构建，全面对齐 Material 3 设计标准。

在本仓库（个人自用分支）中，额外加入了**便签模块**，并调整了分类命名与应用标识。

**上游归属链：**

```
lentikr/Reminder  →  ybhgl/Reminder  →  ZerooTTwo/Reminderplus（本仓库，Reminder+）
```

本仓库在 [ybhgl/Reminder](https://github.com/ybhgl/Reminder)（其本身 fork 自 [lentikr/Reminder](https://github.com/lentikr/Reminder)）的基础上继续开发，上游已有的全部功能均予保留，感谢两位原作者的贡献。

## 应用信息

| 项 | 值 |
| --- | --- |
| 应用名 | Reminder+ |
| applicationId | `com.zt.reminderplus` |
| 版本 | 1.0.1（versionCode 2） |
| 最低系统 | Android 9（API 28） |
| 架构 | arm64-v8a |
| 包体 | 约 7 MB |

> [!CAUTION]
> 由于包名（`com.zt.reminderplus`）与上游 `com.ybhgl.reminder` 不同，系统会视其为**全新应用**，可与原版 Reminder **共存安装**，但**不共享数据**。
> 如需迁移原版数据：先在原版中「设置 → 备份与恢复」导出 JSON 备份，再在 Reminder+ 中导入。

## 本分支的改动

### 📝 便签模块（新增）

- **独立分类：** 便签使用自己的分类体系（与提醒的标签互不影响），可增删改、每个分类可选 15 种颜色
- **列表与检索：** 关键词搜索（标题 + 正文）、按分类筛选、置顶、显示分类色点与更新时间
- **编辑：** 标题可留空（自动取正文首个非空行作标题）、正文多行、单选分类；返回即自动保存
- **删除：** 二次确认，删除后不可恢复
- **入口：** 首页顶栏的便签图标（搜索图标左侧）

### ☁️ 便签与 WebDAV

- 便签已纳入应用**统一的备份数据**（`BackupData.notes` / `noteCategories`），手动备份、自动备份、加密 zip 打包都天然包含便签
- 便签页顶栏两个按钮：
  - **上传**：与「设置 → 备份与恢复 → 立即备份到 WebDAV」等价，直接上传一份完整备份（含便签）到 WebDAV 根目录，**不依赖「自动备份」开关**
  - **恢复**：扫描 WebDAV 根目录与 `Auto/` 子目录，取最新的一份备份，**仅把其中的便签合并回本机**，不会覆盖本机的提醒数据
- 前置条件：只需在「设置 → 备份与恢复」填好并保存 WebDAV 服务器 / 账号 / 密码 / 路径；若上传失败，请确认「设置 → 权限管理」中已允许本应用联网

> 1.0.1 修复：1.0.0 中便签上传误判为「必须开启 WebDAV 自动备份」，导致只配置了 WebDAV 账号时无法上传。

### 👁 标签 / 便签分类隐藏

- 提醒标签与便签分类都支持「隐藏/显示」：标签管理页每行的眼睛按钮、便签右上角便签管理里的眼睛按钮
- **隐藏即连同内容一起隐藏**：隐藏某标签后该标签下的提醒不再出现在首页；隐藏某便签分类后该分类下的便签同样不显示
- 「未分类」在该分类下没有任何内容时会自动隐藏；便签的「全部」标签可在便签管理里单独隐藏（只隐藏标签，不影响内容）

### 🗑 保险柜已移除

- 保险柜（隐私收纳 + 独立密码）已整体删除，原柜内提醒升级后**自动迁回首页**，不会丢失
- 应用锁（手势密码 / 指纹 / 禁止截图）保持不变

### 🗂 分类命名调整

- 首页底栏第三类由「生日」改为「**纪念日**」
- 设置页「默认页面」「首页分类」的相关措辞同步调整
- 详情页的「生日档案 / 生日列表 / N 岁生日」保持原样（那里确实指生日）

### ⚙️ 应用标识

- 应用名改为 `Reminder+`，applicationId 改为 `com.zt.reminderplus`，版本 `1.0.1 (2)`
- 内部 Kotlin 命名空间仍为 `com.ybhgl.reminder`（保持与上游同步的便利，仅安装标识独立）
- 数据库版本 `17 → 18`：新增 `notes`、`note_categories` 两张表

## 功能概览

### 📌 倒数 / 正数 / 纪念日

- **倒数日与正数日：** 倒数日提醒生日、纪念日等未来事件；正数日记录"来到世界的第 N 天"；支持农历并可选自动换算下一个农历日期
- **分组与置顶：** 为每个提醒设置分类快速整理，重要提醒置顶显示
- **重复周期：** 支持设置重复周期（x 天、x 周、x 月、x 年）
- **分享与导出：** 以图片形式保存或分享纪念日
- **外观设置：** 自动 / 浅色 / 深色三种主题模式；深色模式下可启用纯黑主题适配 AMOLED；卡片化与列表两种布局

### ✨ 上游已有特性（本分支同样具备）

- **界面焕然一新：** 全新的视觉设计与流畅动画；可自由定制每条提醒的颜色、图标和背景，模糊与液态玻璃质感效果更加通透真实，还能统一显示所有事件、滚动时自动收起底栏，大屏和横屏下显示效果也更好
- **日历与黄历：** 首页右滑即可打开日历视图，月视图同步显示农历、节气与节日，并自动同步在线节假日，放假与补班一眼可见；点选日期可查看当天宜忌与提醒、直接新建提醒，还提供老黄历详情与择日页面，宜忌、值神等术语点开即有白话解释
- **隐私更安全：** 支持手势密码和指纹、面部解锁，离开应用再回来需要验证，还能禁止截屏录屏，防止日程被偷看
- **区间事件：** 倒数日可设置结束时间形成时间区间，按「还有 x 天 → 就是今天 → 第 x 天 → 已过 x 天」四个阶段展示，小组件与通知同步显示，适合假期、项目周期等场景
- **日期计算器：** 支持日期推算与日期间隔两种模式，公历农历双历显示、快捷天数按钮，计算结果一键就能存成提醒
- **回收站：** 删除的事项先移入回收站，可搜索、多选批量还原或彻底删除，还能设置自动清理周期，误删也能轻松找回
- **提醒更贴心：** 支持公农历生日提醒，自动显示生肖、星座和年龄；到点精确提醒，重启手机也不会漏掉；能自动同步到手机日历，还能在"提醒管理"里集中查看和修改所有提醒
- **数据不丢失：** 一键备份到自己的网盘或手机本地，内容加密保存，换机、重装都能轻松恢复
- **桌面小组件：** 提供多种尺寸的桌面小组件，文字大小自动调整，支持按标签筛选与双列布局，2x2 小组件自适应桌面比例并可个性化外观，重要日子一眼可见
- **分享与记录：** 可以把提醒做成精美图片卡片保存或分享给朋友，支持光栅玻璃质感的个性化背景；详情页翻一翻卡片就能查看备注
- **通知新玩法：** 多种通知样式可选：普通通知、实时提醒，小米手机还支持灵动岛
- **更多细节：** 自定义启动页、权限管理与联网控制、预测性返回手势、应用内检查更新、多标签整理与高级搜索，以及大量体验细节优化

## 构建

本仓库使用 GitHub Actions 构建，工作流为仓库内的 `.github/workflows/release.yml`：

1. 在仓库 Settings → Secrets and variables → Actions 中配置
   - `SIGNING_KEY_ENCODED`：release keystore 的 Base64
   - `SIGNING_CONFIG`：`local.properties` 内容（`storeFile=app.key`、`storePassword`、`keyAlias`、`keyPassword`）
2. 推送 `v*` 形式的 tag（例如 `v1.0.0`），自动构建签名 release APK 并创建 Release
3. 产物：`Reminder-v<versionName>-arm64-v8a-release.apk`

本地构建需要 JDK 21 + Android SDK（platform 37 / build-tools 37），执行 `./gradlew assembleRelease`。

## 开源致谢

本项目的实现受益于以下上游项目与开源组件，感谢所有贡献者：

- [ybhgl/Reminder](https://github.com/ybhgl/Reminder)：本仓库的直接上游；
- [lentikr/Reminder](https://github.com/lentikr/Reminder)：上游原始项目；
- [Tyme](https://github.com/6tail/tyme4kt)：强大的公农历 / 干支 / 星座换算日历工具库；
- [Accompanist](https://github.com/google/accompanist)：权限申请与分页指示组件；
- [Capturable](https://github.com/PatilShreyas/Capturable)：Compose 界面截图分享；
- [compose-m3-picker](https://github.com/Seo-4d696b75/compose-m3-picker)：Material 3 风格滚轮选择器；
- [HyperNotification](https://github.com/xzakota/HyperNotification)：小米超级岛通知适配；
- [InstallerX Revived](https://github.com/wxxsfxyzm/InstallerX-Revived)：小米超级岛通知实现参考；
- [Shizuku](https://github.com/RikkaApps/Shizuku-API)：高级系统通知能力支持；
- [AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass)：Compose 液态玻璃效果支持。

## 许可证与第三方声明

本项目主体代码以 [GNU General Public License v3](LICENSE) 发布，继承自上游。任何二次分发或衍生作品须遵循 GPLv3 条款同样开源。

本仓库为**个人自用分支**，仅自行构建安装使用，不对外分发；若后续公开分发改版 APK，需一并公开修改后的完整源码并保留原始版权声明。

第三方库不因本项目的 GPLv3 而自动转为 GPLv3，各自遵循其原始许可证（主要为 Apache License 2.0 与 MIT）。完整的依赖清单、版本号、许可证类型及项目链接，请查看应用内 **设置 → 关于 → 开放源代码许可** 页面。

## 软件界面预览

> 以下截图来自上游版本，界面风格与本项目一致；本分支新增的**便签页**尚未截图。

<table align="center">
  <tr>
    <td><img src="./screenshots/home.jpg" width="200" alt="主界面"></td>
    <td><img src="./screenshots/edit.jpg" width="200" alt="新建和编辑提醒"></td>
    <td><img src="./screenshots/settings.jpg" width="200" alt="设置页面"></td>
    <td><img src="./screenshots/search.jpg" width="200" alt="搜索页面"></td>
  </tr>
</table>

<table align="center">
  <tr>
    <td><img src="./screenshots/share.jpg" width="200" alt="分享页面"></td>
    <td><img src="./screenshots/backup.jpg" width="200" alt="备份页面"></td>
    <td><img src="./screenshots/tag.jpg" width="200" alt="标签页面"></td>
    <td><img src="./screenshots/notification.jpg" width="200" alt="通知页面"></td>
  </tr>
</table>

<p align="center">
  <img src="./screenshots/share.png" width="300" alt="分享卡片效果">
</p>

## 反馈

本仓库为个人自用分支，不接收外部 Pull Request。上游项目的 bug 反馈与功能建议请提交至：

- [ybhgl/Reminder Issues](https://github.com/ybhgl/Reminder/issues)
- [lentikr/Reminder](https://github.com/lentikr/Reminder)

## 免责声明

本项目 fork 自开源项目并仅供学习、研究和个人使用。使用本项目进行日程提醒、日历同步、WebDAV 云端备份等功能时，请自行妥善保管账号凭据与备份数据，并遵守当地法律法规及相关服务的使用条款。项目维护者不对数据丢失、第三方服务可用性或因使用本项目产生的任何后果承担责任。
