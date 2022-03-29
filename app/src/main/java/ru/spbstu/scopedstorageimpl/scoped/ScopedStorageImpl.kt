package ru.spbstu.scopedstorageimpl.scoped

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.annotation.WorkerThread
import androidx.core.content.contentValuesOf
import java.io.File
import java.io.InputStream

@RequiresApi(value = 29)
internal class ScopedStorageImpl(
    private val appContext: Context,
    private val userMediaDirectoryName: String,
) : ScopedStorage {

    private val contentResolver by lazy { appContext.contentResolver }

    @WorkerThread
    override fun getNewVideoGalleryUri(): Uri? {
        val collection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val contentValues = generateContentValues(
            dir = Environment.DIRECTORY_MOVIES,
            fileName = getVideoFileName(),
            mimeType = ScopedStorage.MimeType.VIDEO_MP4,
        )

        return contentResolver.insert(collection, contentValues)
    }

    override fun copyInputStreamToUri(inputStream: InputStream, uri: Uri): Uri {
        contentResolver.openOutputStream(uri, "w")?.use {
            val buffer = ByteArray(1024)
            while (inputStream.read(buffer) > 0) {
                it.write(buffer)
            }
            it.flush()
        }
        return uri
    }

    @WorkerThread
    override fun getNewDownloadFileUri(fileName: String, mimeType: ScopedStorage.MimeType): Uri? {
        val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val contentValues = generateContentValues(
            dir = Environment.DIRECTORY_DOWNLOADS,
            fileName = fileName,
            mimeType = mimeType,
        )

        return contentResolver.insert(collection, contentValues)
    }

    @WorkerThread
    override fun getNewImageGalleryUri(gif: Boolean): Uri? {
        val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val contentValues = generateContentValues(
            dir = Environment.DIRECTORY_PICTURES,
            fileName = getPhotoFileName(gif),
            mimeType = if (gif) ScopedStorage.MimeType.IMAGE_GIF else ScopedStorage.MimeType.IMAGE_JPEG
        )

        return contentResolver.insert(collection, contentValues)
    }

    override fun getNewDocumentUri(fileName: String): Uri? {
        val collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val contentValues = generateContentValues(
            dir = Environment.DIRECTORY_DOCUMENTS,
            fileName = fileName,
            mimeType = ScopedStorage.MimeType.TEXT_PLAIN
        )

        return contentResolver.insert(collection, contentValues)
    }

    override fun deleteResource(uri: Uri): Boolean {
        return contentResolver.delete(uri, null, null) > 0
    }

    @WorkerThread
    override fun saveMediaToGallery(bitmap: Bitmap, fileName: String): Uri? {
        val contentValues = generateContentValues(
            dir = Environment.DIRECTORY_PICTURES,
            fileName = fileName,
            mimeType = ScopedStorage.MimeType.IMAGE_JPEG,
        )

        bitmap.byteCount.let {
            contentValues.put(MediaStore.MediaColumns.SIZE, it)
        }
        bitmap.width.let {
            contentValues.put(MediaStore.MediaColumns.WIDTH, it)
        }
        bitmap.height.let {
            contentValues.put(MediaStore.MediaColumns.HEIGHT, it)
        }
        contentValues.put(MediaStore.Images.Media.IS_PENDING, 1)

        val uri = contentResolver.insert(
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
            contentValues
        ) ?: return null

        contentResolver.openOutputStream(uri, "w").use {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
        }
        contentValues.clear()
        contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)

        contentResolver.update(uri, contentValues, null, null)

        sendBroadcastToGallery(appContext, uri)

        return uri
    }

    /**
     * Генерирует мета информацию
     */
    private fun generateContentValues(
        dir: String,
        fileName: String,
        mimeType: ScopedStorage.MimeType
    ): ContentValues {
        val date = System.currentTimeMillis() / 1000
        val dirDest = File(dir, userMediaDirectoryName)

        return contentValuesOf(
            MediaStore.MediaColumns.DISPLAY_NAME to fileName,
            MediaStore.MediaColumns.MIME_TYPE to mimeType.value,
            MediaStore.MediaColumns.DATE_ADDED to date,
            MediaStore.MediaColumns.DATE_MODIFIED to date,
            MediaStore.MediaColumns.RELATIVE_PATH to "$dirDest${File.separator}",
        )
    }
}