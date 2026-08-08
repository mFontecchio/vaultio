package com.mrhayami.vaultio.data.update

import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException

class ApkDownloader(
    private val okHttpClient: OkHttpClient
) {
    /**
     * Downloads [url] to [destination], replacing any existing file.
     * [onProgress] receives bytes downloaded and total bytes (or -1 if unknown).
     */
    fun download(
        url: String,
        destination: File,
        userAgent: String,
        onProgress: ((bytesRead: Long, contentLength: Long) -> Unit)? = null
    ) {
        destination.parentFile?.mkdirs()
        val partial = File(destination.parentFile, "${destination.name}.partial")
        if (partial.exists()) partial.delete()
        if (destination.exists()) destination.delete()

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", userAgent)
            .get()
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Download failed: HTTP ${response.code}")
            }
            val body = response.body ?: throw IOException("Empty download body")
            val contentLength = body.contentLength()
            var bytesRead = 0L
            body.byteStream().use { input ->
                partial.outputStream().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        bytesRead += read
                        onProgress?.invoke(bytesRead, contentLength)
                    }
                }
            }
            if (!partial.renameTo(destination)) {
                partial.copyTo(destination, overwrite = true)
                partial.delete()
            }
            if (!destination.exists() || destination.length() == 0L) {
                destination.delete()
                throw IOException("Downloaded APK is empty")
            }
        }
    }

    companion object {
        private const val DEFAULT_BUFFER_SIZE = 8 * 1024
    }
}
