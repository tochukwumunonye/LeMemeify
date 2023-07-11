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

package com.raywenderlich.android.lememeify.ui.images

import android.content.pm.PackageManager
import android.graphics.Rect
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.raywenderlich.android.lememeify.R
import com.raywenderlich.android.lememeify.Utils
import com.raywenderlich.android.lememeify.databinding.FragmentMainBinding
import com.raywenderlich.android.lememeify.ui.MainAction

private const val NUMBER_OF_COLUMNS = 5
private const val REQUEST_PERMISSION_MEDIA = 100

class MainFragment : Fragment() {

  private val viewModel: MainViewModel by viewModels()

  private var permissionDenied = false
  private val imageAdapter by lazy {
    ImageAdapter { clickedImage ->
      Utils.navigateToDetails(requireView(), clickedImage)
    }
  }

  private lateinit var binding: FragmentMainBinding

  override fun onCreateView(inflater: LayoutInflater, group: ViewGroup?, state: Bundle?): View? {
    viewModel.actions.observe(viewLifecycleOwner, Observer { handleAction(it) })
    binding = FragmentMainBinding.inflate(inflater)
    binding.rvImages.adapter = imageAdapter
    return binding.root
  }

  override fun onActivityCreated(savedInstanceState: Bundle?) {
    super.onActivityCreated(savedInstanceState)
    setupToolbar()
    setupUiComponents()
  }

  override fun onResume() {
    super.onResume()
    if (!Utils.hasStoragePermission(requireContext())) {
      if (!permissionDenied) {
        viewModel.requestStoragePermissions()
      }
    }
    viewModel.loadImages()
  }

  override fun onPause() {
    permissionDenied = false
    super.onPause()
  }

  override fun onRequestPermissionsResult(code: Int, permission: Array<out String>, res: IntArray) {
    when (code) {
      REQUEST_PERMISSION_MEDIA -> {
        when {
          res.isEmpty() -> {
            //Do nothing, app is resuming
          }
          res[0] == PackageManager.PERMISSION_GRANTED -> {
            setupUiComponents()
          }
          else -> {
            permissionDenied = true
            Toast.makeText(requireContext(),
                R.string.toast_permission_media, Toast.LENGTH_SHORT).show()
          }
        }
      }
    }
  }

  private fun handleAction(action: MainAction) {
    when (action) {
      is MainAction.ImagesChanged -> imageAdapter.submitList(action.images)
      MainAction.StoragePermissionsRequested -> Utils.requestStoragePermission(this,
          REQUEST_PERMISSION_MEDIA)
    }
  }

  private fun setupToolbar() {
    val appCompatActivity = activity as AppCompatActivity
    appCompatActivity.setSupportActionBar(binding.tbMain)
    appCompatActivity.setTitle(R.string.app_header)
  }

  private fun setupUiComponents() {
    val spacing = resources.getDimensionPixelSize(R.dimen.grid_space) / 2
    binding.rvImages.apply {
      setHasFixedSize(true)
      setPadding(spacing, spacing, spacing, spacing)
      addItemDecoration(object : RecyclerView.ItemDecoration() {
        override fun getItemOffsets(rect: Rect,
            view: View,
            parent: RecyclerView,
            state: RecyclerView.State) {
          rect.set(spacing, spacing, spacing, spacing)
        }
      })
      layoutManager = GridLayoutManager(requireContext(),
          NUMBER_OF_COLUMNS)
    }
  }
}