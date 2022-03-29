package ru.spbstu.scopedstorageimpl.scoped

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.WorkerThread
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * Сторадж для сохранения фото и видео в галерею
 */
@RequiresApi(value = 29)
public interface ScopedStorage {

    /**
     * Получить Uri для сохранения видео в галерею
     */
    @WorkerThread
    public fun getNewVideoGalleryUri(): Uri?

    /**
     * Записать InputStream в Uri
     */
    @WorkerThread
    public fun copyInputStreamToUri(inputStream: InputStream, uri: Uri): Uri

    /**
     * Получить Uri для сохранения картинки в галерею
     */
    @WorkerThread
    public fun getNewImageGalleryUri(gif: Boolean): Uri?

    /**
     * Сохраняем медиа в общую галерею
     */
    @WorkerThread
    public fun saveMediaToGallery(bitmap: Bitmap, fileName: String): Uri?

    /**
     * Получить Uri для сохранения файла в папке Downloads
     */
    @WorkerThread
    public fun getNewDownloadFileUri(fileName: String, mimeType: MimeType): Uri?

    /**
     * Получить Uri для сохранения документа
     */
    @WorkerThread
    public fun getNewDocumentUri(fileName: String): Uri?

    /**
     * Удаление файла по Uri
     */
    @WorkerThread
    public fun deleteResource(uri: Uri): Boolean

    /**
     * Сформировать имя для нового видео
     */
    public fun getVideoFileName(): String {
        val suffix = getTimestamp(Date())
        return "MOV_$suffix.mp4"
    }

    private fun getTimestamp(date: Date): String {
        val df = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH)
        return df.format(date)
    }

    /**
     * Сформировать имя для новой картинки
     */
    public fun getPhotoFileName(gif: Boolean): String {
        val timeStamp = getTimestamp(Date())
        val extension = if (gif) "gif" else "jpg"
        return "IMG_$timeStamp.$extension"
    }

    /**
     * Отправляет бродкаст о том, что мы что-то записали в галерею
     */
    public fun sendBroadcastToGallery(context: Context, uri: Uri) {
        val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
        mediaScanIntent.data = uri
        try {
            context.sendBroadcast(mediaScanIntent)
        } catch (e: java.lang.Exception) {
            Log.e(TAG, "sendBroadcastToGallery: failed for uri $uri", e)
        }
    }

    public companion object {
        private val TAG = ScopedStorage::class.java.simpleName

        @Suppress("NEWER_VERSION_IN_SINCE_KOTLIN")
        @JvmStatic
        @SinceKotlin("999.9")
        public fun create(
            context: Context,
            userMediaDirectoryName: String
        ): ScopedStorage = ScopedStorage(context, userMediaDirectoryName)
    }

    enum class MimeType(val value: String) {
        UNKNOWN("unknown"),
        IMAGE_JPEG("image/jpeg"),
        IMAGE_PNG("image/png"),
        IMAGE_WEBP("image/webp"),
        IMAGE_GIF("image/gif"),
        IMAGE_ANY("image/*"),
        IMAGE_HEIC("image/heic"),
        VIDEO_MP4("video/mp4"),
        VIDEO_ANY("video/*"),
        TEXT_PLAIN("text/plain"),
        TEXT_VCARD("text/x-vcard");

        fun equalsString(mimeType: String?): Boolean = mimeType?.startsWith(value, ignoreCase = true) ?: false

        override fun toString(): String = value

        companion object {
            @JvmStatic
            fun fromString(mimeType: String?): MimeType =
                values().firstOrNull { it.value.equals(mimeType, ignoreCase = true) } ?: UNKNOWN

            @JvmStatic
            fun isImage(mimeType: String?): Boolean = !mimeType.isNullOrEmpty() &&
                    mimeType.startsWith("image/", ignoreCase = true) &&
                    !mimeType.contains("djvu", ignoreCase = true)


            @JvmStatic
            fun isVideo(mimeType: String?): Boolean =
                !mimeType.isNullOrEmpty() && mimeType.startsWith("video/", ignoreCase = true)
        }

    }
}

@RequiresApi(value = 29)
public fun ScopedStorage(
    context: Context,
    userMediaDirectoryName: String
): ScopedStorage {
    return ScopedStorageImpl(context, userMediaDirectoryName)
}
