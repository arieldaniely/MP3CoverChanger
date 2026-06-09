package com.mp3cover

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

data class Mp3FileItem(
    val uri: Uri,
    val fileName: String,
    val title: String?,
    val artist: String?,
    val album: String?,
    val coverArt: Bitmap?,
    val fileSizeKb: Long
) {
    companion object {

        fun fromUri(context: Context, uri: Uri): Mp3FileItem? {
            return try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(context, uri)

                val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                val artBytes = retriever.embeddedPicture
                val coverBitmap = artBytes?.let {
                    BitmapFactory.decodeByteArray(it, 0, it.size)
                }
                retriever.release()

                val fileName = getFileName(context, uri)
                val fileSize = getFileSize(context, uri)

                Mp3FileItem(
                    uri = uri,
                    fileName = fileName,
                    title = title,
                    artist = artist,
                    album = album,
                    coverArt = coverBitmap,
                    fileSizeKb = fileSize / 1024
                )
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

        fun writeCoverArt(context: Context, mp3Uri: Uri, imageUri: Uri): Boolean {
            return try {
                // Read image bytes
                val imageBytes = context.contentResolver.openInputStream(imageUri)?.use {
                    it.readBytes()
                } ?: return false

                // Compress image if too large (max 500KB)
                val compressedBytes = if (imageBytes.size > 500_000) {
                    compressImage(imageBytes)
                } else {
                    imageBytes
                }

                // Copy MP3 to temp file, modify, write back
                val tempInput = copyToTempFile(context, mp3Uri) ?: return false
                val tempOutput = File(context.cacheDir, "output_${System.currentTimeMillis()}.mp3")

                try {
                    val mp3File = com.mpatric.mp3agic.Mp3File(tempInput.absolutePath)

                    val id3v2Tag = if (mp3File.hasId3v2Tag()) {
                        mp3File.id3v2Tag
                    } else {
                        val tag = com.mpatric.mp3agic.ID3v24Tag()
                        mp3File.id3v2Tag = tag
                        tag
                    }

                    // Also copy metadata from v1 if exists
                    if (mp3File.hasId3v1Tag() && !mp3File.hasId3v2Tag()) {
                        val v1 = mp3File.id3v1Tag
                        id3v2Tag.title = v1.title
                        id3v2Tag.artist = v1.artist
                        id3v2Tag.album = v1.album
                    }

                    id3v2Tag.setAlbumImage(compressedBytes, "image/jpeg")
                    mp3File.save(tempOutput.absolutePath)

                    // Write back to original URI
                    context.contentResolver.openOutputStream(mp3Uri)?.use { out ->
                        tempOutput.inputStream().use { inp -> inp.copyTo(out) }
                    }

                    true
                } finally {
                    tempInput.delete()
                    tempOutput.delete()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }

        fun removeCoverArt(context: Context, mp3Uri: Uri): Boolean {
            return try {
                val tempInput = copyToTempFile(context, mp3Uri) ?: return false
                val tempOutput = File(context.cacheDir, "output_${System.currentTimeMillis()}.mp3")

                try {
                    val mp3File = com.mpatric.mp3agic.Mp3File(tempInput.absolutePath)

                    if (mp3File.hasId3v2Tag()) {
                        mp3File.id3v2Tag.clearAlbumImage()
                        mp3File.save(tempOutput.absolutePath)

                        context.contentResolver.openOutputStream(mp3Uri)?.use { out ->
                            tempOutput.inputStream().use { inp -> inp.copyTo(out) }
                        }
                    }

                    true
                } finally {
                    tempInput.delete()
                    tempOutput.delete()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }

        private fun copyToTempFile(context: Context, uri: Uri): File? {
            return try {
                val tempFile = File(context.cacheDir, "temp_${System.currentTimeMillis()}.mp3")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                }
                tempFile
            } catch (e: Exception) {
                null
            }
        }

        private fun compressImage(bytes: ByteArray): ByteArray {
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return bytes
            val scaled = Bitmap.createScaledBitmap(bitmap, 500, 500, true)
            val stream = java.io.ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 85, stream)
            return stream.toByteArray()
        }

        private fun getFileName(context: Context, uri: Uri): String {
            var name = "unknown.mp3"
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && idx >= 0) {
                    name = cursor.getString(idx)
                }
            }
            return name
        }

        private fun getFileSize(context: Context, uri: Uri): Long {
            var size = 0L
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val idx = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                if (cursor.moveToFirst() && idx >= 0) {
                    size = cursor.getLong(idx)
                }
            }
            return size
        }
    }
}
