package com.ybhgl.reminder.data

import android.content.Context
import com.ybhgl.reminder.util.BackupArchiveManager
import com.ybhgl.reminder.util.CardBackgroundImageManager
import com.ybhgl.reminder.util.FontManager
import com.ybhgl.reminder.util.WebDavClient
import com.ybhgl.reminder.util.WebDavResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * 一次性把完整备份上传到 WebDAV 根目录。
 *
 * 语义与「设置 → 备份与恢复 → 立即备份到 WebDAV」一致：
 * 只依赖 WebDAV 账号与路径，**不依赖「自动备份」开关**，
 * 便签因为已纳入 [BackupData] 而自然包含在内。
 */
object ManualWebDavBackup {

    data class Result(
        val success: Boolean,
        val message: String,
        val fileName: String = ""
    )

    /** 生成与「手动备份」一致的备份文件名 */
    /** 便签备份专用子目录（与自动备份的 Auto/ 完全分开，互不干扰） */
    const val NOTES_FOLDER = "Notes"

    /** 便签备份文件名：reminder-notes-yyyyMMdd-HHmmss.zip */
    fun generateFileName(): String {
        val timestamp = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss", Locale.getDefault()))
        return "reminder-notes-$timestamp${BackupArchiveManager.BACKUP_EXTENSION}"
    }

    /** 便签同步目录：WebDAV 路径 + /Notes */
    fun notesPath(path: String): String = when {
        path.isBlank() -> NOTES_FOLDER
        path.endsWith("/") -> "$path$NOTES_FOLDER"
        else -> "$path/$NOTES_FOLDER"
    }

    suspend fun upload(context: Context): Result = withContext(Dispatchers.IO) {
        val server = BackupPreferences.webDavServerFlow(context).first()
        val username = BackupPreferences.webDavUsernameFlow(context).first()
        val password = BackupPreferences.webDavPasswordFlow(context).first()
        val path = BackupPreferences.webDavPathFlow(context).first()

        if (server.isBlank() || username.isBlank() || password.isBlank()) {
            return@withContext Result(
                success = false,
                message = "请先在「设置 → 备份与恢复」中填写并保存 WebDAV 服务器、账号和密码"
            )
        }

        val database = ReminderDatabase.getDatabase(context)
        // 数据源：不含回收站
        val reminders = database.reminderDao().getBackupRemindersList()
        val notes = database.noteDao().getAllNotes()
        if (reminders.isEmpty() && notes.isEmpty()) {
            return@withContext Result(success = false, message = "没有可备份的数据")
        }

        val backupData = BackupDataBuilder.build(context, reminders, database.tagDao().getAllTags())
        val json = Json.encodeToString(BackupData.serializer(), backupData)

        val imageFiles = CardBackgroundImageManager.collectImageFiles(context, reminders)
        val fontFiles = FontManager.collectFontFiles(context, reminders)
        val encrypt = BackupPreferences.backupEncryptionEnabledFlow(context).first()
        val archiveBytes = BackupArchiveManager.encode(json, imageFiles, fontFiles, encrypt)
            ?: return@withContext Result(success = false, message = "备份失败：压缩包打包失败")

        val fileName = generateFileName()

        // 便签单独放 Notes/ 子目录，避免与自动备份（Auto/）互相干扰
        val uploadPath = notesPath(path)

        return@withContext when (val result = WebDavClient.uploadFileBytes(server, username, password, uploadPath, fileName, archiveBytes)) {
            is WebDavResult.Success -> {
                BackupPreferences.saveLastBackupTimestamp(context, System.currentTimeMillis())
                Result(
                    success = true,
                    message = "已上传到 WebDAV/${NOTES_FOLDER}：$fileName（含 ${notes.size} 条便签）",
                    fileName = fileName
                )
            }
            is WebDavResult.Failure -> {
                val hint = when (result.code) {
                    -1 -> "请求超时，请检查网络状况或服务器地址"
                    -2 -> "无法解析服务器地址，请检查 WebDAV 域名是否填写正确"
                    -3 -> "网络请求失败，请检查网络，并在「设置 → 权限管理」中确认已允许本应用联网（WebDAV）"
                    401, 403 -> "账号或密码不正确，或该目录没有写入权限"
                    404 -> "目录不存在，请检查 WebDAV 路径"
                    507 -> "服务器空间不足"
                    else -> "请检查账号、密码与目录写入权限"
                }
                Result(
                    success = false,
                    message = "上传失败（HTTP ${result.code}）：$hint" +
                        if (result.message.isNotBlank()) " · ${result.message}" else ""
                )
            }
        }
    }
}
