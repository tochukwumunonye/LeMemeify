/*
 * Copyright (c) 2020 Razeware LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * Notwithstanding the foregoing, you may not use, copy, modify, merge, publish,
 * distribute, sublicense, create a derivative work, and/or sell copies of the
 * Software in any work that is designed, intended, or marketed for pedagogical or
 * instructional purposes related to programming, coding, application development,
 * or information technology.  Permission for such use, copying, modification,
 * merger, publication, distribution, sublicensing, creation of derivative works,
 * or sale is expressly withheld.
 *
 * This project and source code may use libraries or frameworks that are
 * released under various Open-Source licenses. Use of those libraries and
 * frameworks are governed by their own individual licenses.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.raywenderlich.android.lememeify

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.raywenderlich.android.lememeify.model.Image
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream


private const val TAG = "FileOperations"
private const val QUALITY = 100

object FileOperations {

  /**
   * We're using DATA column which has been deprecated due to scoped storage. When active/ the
   * app is running on Android 11 it won't be possible to get the image file path. You'll need to
   * access [MediaStore.Images.Media.EXTERNAL_CONTENT_URI] in order to get it's [Uri].
   */
  @Suppress("deprecation")
  suspend fun queryImagesOnDevice(context: Context): List<Image> {
    val images = mutableListOf<Image>()

    withContext(Dispatchers.IO) {

      val projection = arrayOf(MediaStore.Images.Media._ID,
          MediaStore.Images.Media.DATA,
          MediaStore.Images.Media.DISPLAY_NAME,
          MediaStore.Images.Media.SIZE,
          MediaStore.Images.Media.MIME_TYPE,
          MediaStore.Images.Media.WIDTH,
          MediaStore.Images.Media.HEIGHT,
          MediaStore.Images.Media.DATE_MODIFIED)

      val sortOrder = "${MediaStore.Images.Media.DATE_MODIFIED} DESC"

      context.contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
          projection,
          null,
          null,
          sortOrder)?.use { cursor ->

        while (cursor.moveToNext()) {
          val id = cursor.getLong(cursor.getColumnIndex(MediaStore.Images.Media._ID))
          val path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA))
          val name = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME))
          val size = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.SIZE))
          val width = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.WIDTH))
          val height = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.HEIGHT))
          val date = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATE_MODIFIED))

          // Discard invalid images that might exist on the device
          if (size == null) {
            continue
          }

          images += Image(id, path, name, size, width, height, date)
        }

        cursor.close()
      }
    }

    return images
  }

  /**
   * We're using [Environment.getExternalStorageState] dir that has been deprecated due to scoped
   * storage. When active/ the app is running on Android 11 it won't be possible to save an image
   * using file paths - you should do it via an accessible [Uri]]
   */
  @Suppress("deprecation")
  suspend fun saveImage(context: Context, bitmap: Bitmap, format: CompressFormat): Boolean {
    val externalDir = Environment.getExternalStorageDirectory().path
    val dir = File(externalDir, context.getString(
        R.string.app_name))

    ensureDirExists(
        dir)

    val extension = Utils.getImageExtension(
        format)
    val file = File(dir, "${System.currentTimeMillis()}.$extension")
    return saveImage(
        context, file, bitmap, format)
  }

  suspend fun saveImage(context: Context, file: File, bitmap: Bitmap,
      format: CompressFormat): Boolean {
    var result = false

    withContext(Dispatchers.IO) {
      try {
        result = FileOutputStream(file).use {
          bitmap.compress(format,
              QUALITY, it)
          it.close()
          true
        }

      } catch (e: Exception) {
        Log.e(TAG, "Unable to save image. Reason: ${e.message}")
      }

      sendScanFileBroadcast(
          context, file)
    }

    return result
  }

  fun deleteImage(context: Context, image: Image): Boolean {
    val file = File(image.path)
    val result = file.delete()
    sendScanFileBroadcast(
        context, file)
    return result
  }

  private fun ensureDirExists(dir: File) {
    if (!dir.exists()) {
      dir.mkdirs()
    }
  }

  /**
   * Due to the developments on scoped storage [Intent.ACTION_MEDIA_SCANNER_SCAN_FILE] was
   * deprecated on API 29. This broadcast will no longer be needed to call after an image
   * creation/ update once the code is migrated to use [MediaStore].
   */
  @Suppress("deprecation")
  private fun sendScanFileBroadcast(context: Context, file: File) {
    val intent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
    intent.data = Uri.fromFile(file)
    context.sendBroadcast(intent)
  }
}