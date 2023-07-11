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

package com.raywenderlich.android.lememeify.ui.details

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.raywenderlich.android.lememeify.FileOperations
import com.raywenderlich.android.lememeify.Utils
import com.raywenderlich.android.lememeify.model.Image
import com.raywenderlich.android.lememeify.ui.ImageDetailAction
import kotlinx.coroutines.launch
import java.io.File

private val DEFAULT_BITMAP_COMPRESS_FORMAT = Bitmap.CompressFormat.JPEG

class DetailsViewModel(application: Application) : AndroidViewModel(application) {

  private val _actions = MutableLiveData<ImageDetailAction>()
  val actions: LiveData<ImageDetailAction> get() = _actions

  fun saveImage(path: String?, bitmap: Bitmap) {
    viewModelScope.launch {

      if (path == null) {
        FileOperations.saveImage(getApplication(), bitmap,
            DEFAULT_BITMAP_COMPRESS_FORMAT)
      } else {
        val format = Utils.getImageFormat(path)
        FileOperations.saveImage(getApplication(), File(path), bitmap, format)
      }

      _actions.postValue(ImageDetailAction.ImageSaved)
    }
  }

  fun updateImage(image: Image, bitmap: Bitmap) {
    viewModelScope.launch {

      val format = Utils.getImageFormat(image.path)
      FileOperations.saveImage(getApplication(),
          File(image.path),
          bitmap,
          format)

      _actions.postValue(ImageDetailAction.ImageUpdated)
    }
  }

  fun deleteImage(image: Image) {
    viewModelScope.launch {
      FileOperations.deleteImage(getApplication(), image)
      _actions.postValue(ImageDetailAction.ImageDeleted)
    }
  }
}