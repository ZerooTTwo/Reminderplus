# 便签功能说明（Reminder+ 自用分支）

本分支在 [ybhgl/Reminder](https://github.com/ybhgl/Reminder) 基础上新增了「便签」模块，并把应用改为可共存安装的 **Reminder+**。

## 一、功能

- **入口**：首页顶栏的便签图标（搜索图标左侧）→ 全屏便签列表。
- **分类**：独立的「便签分类」，与提醒使用的标签互不影响；分类可增删改、可选 15 种颜色。
- **列表**：搜索（标题 + 正文）、按分类筛选、置顶、删除（二次确认）、显示分类色点与更新时间。
- **编辑**：标题可留空（自动取正文首行作标题）、正文多行、分类单选；返回即保存。
- **WebDAV**：便签已纳入应用统一的备份数据，顶栏两个按钮：
  - ☁️ 上传：强制触发一次统一备份（含便签），走既有的加密 zip + WebDAV 上传链路；
  - ⬇️ 恢复：拉取云端最新一次自动备份，**只把其中的便签合并回本机**（不覆盖提醒数据）。

### WebDAV 使用前提

先在「设置 → 备份与恢复」中：

1. 填写 WebDAV 服务器、账号、密码、路径；
2. 打开「WebDAV 自动备份」开关（便签页的上传复用这条链路，未开启时会提示）。

云端目录结构为 `<你的路径>/Auto/reminder-autobackup-yyyyMMdd-HHmmss.zip`，受「自动备份保留份数」限制。

## 二、数据

- 数据库版本 `17 → 18`，新增 `notes`、`note_categories` 两张表，迁移逻辑见 `ReminderDatabase.MIGRATION_17_18`。
- 备份 JSON（`BackupData`）新增 `notes` 与 `noteCategories` 两个可空字段：新版本能读旧备份，旧版本读新备份会因未知字段失败（本分支自用，可忽略）。
- 删除分类时该分类下的便签会回到「未分类」，不会丢数据。

## 三、构建发版（使用仓库自带的 `release.yml`）

本分支**不新增**构建工作流，直接复用仓库已有的 `.github/workflows/release.yml`：打 `v*` tag 时构建**签名 release APK** 并创建 Release。

### 1. 准备签名密钥

```bash
keytool -genkeypair -v -keystore reminder-release.jks \
  -alias reminder -keyalg RSA -keysize 2048 -validity 10000
```

### 2. 在 fork 仓库的 Settings → Secrets and variables → Actions 中添加：

| Secret | 内容 |
| --- | --- |
| `SIGNING_KEY_ENCODED` | keystore 文件的 Base64（Windows PowerShell：`[Convert]::ToBase64String([IO.File]::ReadAllBytes("reminder-release.jks"))`） |
| `SIGNING_CONFIG` | `local.properties` 内容，四行：`storeFile=app.key` / `storePassword=...` / `keyAlias=reminder` / `keyPassword=...` |
| `PERSONAL_ACCESS_TOKEN` | 仅 `auto-tag.yml` 需要（用 PAT 推 tag）；若手动打 tag 可不配 |

> `storeFile=app.key` 是相对 app 模块的路径，因为 `release.yml` 会把 keystore 解码到 `app/app.key`。

### 3. 触发构建

- **自动**：向 `main` 提交，且提交信息匹配 `build(config): bump version to X.Y.Z`（仓库的 `auto-tag.yml` 会自动打 `vX.Y.Z` tag，进而触发 `release.yml`）；
- **手动**：`git tag v4.0.0-notes && git push origin v4.0.0-notes`；
- 也可以在 Actions 页面用 `workflow_dispatch` 手动填 tag 名。

构建产物：Release 页面的 `Reminder-v4.0.0-notes-arm64-v8a-release.apk`（仅 arm64-v8a）。

## 四、安装提示

应用名 `Reminder+`，`applicationId = com.dswork.reminder`，与原版 `com.ybhgl.reminder` 不冲突，可共存安装。但两者数据独立，原版数据需在原版内导出 JSON 备份后，在 Reminder+ 中导入。

## 五、本分支改动清单

新增：

- `data/NoteItem.kt`、`data/NoteCategoryItem.kt`、`data/NoteDao.kt`、`data/NoteCategoryDao.kt`、`data/NoteRepository.kt`
- `ui/notes/NotesViewModel.kt`、`ui/notes/NotesScreen.kt`、`ui/notes/NoteEditScreen.kt`

修改：

- `data/ReminderDatabase.kt`：实体注册、版本 18、`MIGRATION_17_18`、DAO 访问器
- `data/AppContainer.kt`、`ui/common/ViewModelProvider.kt`：注入 `NoteRepository` / `NotesViewModel`
- `data/ReminderItem.kt`：`BackupData` 增加 `notes`、`noteCategories`
- `data/BackupDataBuilder.kt`：备份时从数据库带上便签
- `data/BackupPreferences.kt`：仅有便签无提醒时也允许自动备份
- `ui/settings/BackupAndRestoreViewModel.kt`：恢复时一并恢复便签（覆盖 / 智能合并两条路径）
- `MainActivity.kt`：`Routes.NOTES` / `Routes.NOTE_EDIT_PATTERN`、路由注册、页面分发、首页顶栏入口
- `app/build.gradle.kts`：`applicationId`、`versionCode 400`、`versionName 4.0.0-notes`
- `res/values/strings.xml`：`app_name` → `Reminder+`

## 六、许可证

上游为 GPLv3。本分支仅自用、不再分发，因此没有对外提供源码的义务；若日后要分发这份改版，需按 GPLv3 公开修改后的完整源码并保留原始版权声明。
