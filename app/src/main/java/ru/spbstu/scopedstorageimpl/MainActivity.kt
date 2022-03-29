package ru.spbstu.scopedstorageimpl

import android.graphics.Bitmap
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import ru.spbstu.scopedstorageimpl.databinding.ActivityMainBinding
import ru.spbstu.scopedstorageimpl.scoped.ScopedStorage

class MainActivity : AppCompatActivity() {

    private var _binding: ActivityMainBinding? = null
    private val binding: ActivityMainBinding
        get() = _binding!!

    private lateinit var scopedStorage: ScopedStorage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        scopedStorage = ScopedStorage(applicationContext, "ScopedStorageTest")

        var savedUri: Uri? = null
        binding.btnDownloads.setOnClickListener {
            val uri =
                scopedStorage.getNewDownloadFileUri("TestVideo2", ScopedStorage.MimeType.VIDEO_MP4)
            savedUri = uri
            Log.d("ScopedStorage", "Created uri for video in directory Downloads = $uri")
        }
        binding.btnDocuments.setOnClickListener {
            val uri = scopedStorage.getNewDocumentUri("TestDocument")
            Log.d("ScopedStorage", "Created uri for document in directory Documents = $uri")
        }
        binding.btnDelete.setOnClickListener {
            savedUri?.let {
                val res = scopedStorage.deleteResource(it)
                Log.d("ScopedStorage", "Deleted file: $res")
            }
        }
        binding.btnImages.setOnClickListener {
            val uri = scopedStorage.getNewImageGalleryUri(false)
            Log.d("ScopedStorage", "Created uri for image in directory Images = $uri")
        }

        binding.btnGlide.setOnClickListener {
            Glide.with(this)
                .asBitmap()
                .load("https://cdn.pixabay.com/photo/2015/04/23/22/00/tree-736885__480.jpg")
                .addListener(object : RequestListener<Bitmap> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Bitmap>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        TODO("Not yet implemented")
                    }

                    override fun onResourceReady(
                        resource: Bitmap?,
                        model: Any?,
                        target: Target<Bitmap>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        val uri = scopedStorage.saveMediaToGallery(resource!!, "ImageFromInternet")
                        Log.d("ScopedStorage", "Downloaded image from internet, uri = $uri")
                        return true
                    }
                })
                .preload()
        }
    }
}