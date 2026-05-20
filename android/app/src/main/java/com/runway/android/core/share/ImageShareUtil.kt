package com.runway.android.core.share

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

object ImageShareUtil {

    private const val CACHE_DIR = "share_images"
    private const val FILE_NAME = "runway_run.png"

    fun saveToCache(context: Context, bitmap: Bitmap): Uri? = runCatching {
        val dir = File(context.cacheDir, CACHE_DIR).also { it.mkdirs() }
        val file = File(dir, FILE_NAME)
        file.outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 95, out)
        }
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }.getOrNull()

    fun buildShareIntent(uri: Uri): Intent =
        Intent.createChooser(
            Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            },
            null,
        )
}
