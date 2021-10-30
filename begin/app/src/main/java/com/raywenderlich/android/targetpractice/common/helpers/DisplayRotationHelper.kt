/*
 * Copyright 2017 Google Inc. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Modifications Copyright (c) 2020 Razeware LLC
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
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.raywenderlich.android.targetpractice.common.helpers

import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.display.DisplayManager
import android.hardware.display.DisplayManager.DisplayListener
import android.view.Display
import android.view.Surface
import android.view.WindowManager
import com.google.ar.core.Session

/**
 * Helper to track the display rotations. In particular, the 180 degree rotations are not notified
 * by the onSurfaceChanged() callback, and thus they require listening to the android display
 * events.
 *
 * @param context the Android [Context].
 */
class DisplayRotationHelper(context: Context) : DisplayListener {

  private var viewportChanged = false
  private var viewportWidth = 0
  private var viewportHeight = 0
  private val display: Display
  private val displayManager: DisplayManager =
      context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
  private val cameraManager: CameraManager =
      context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
  private val windowManager =
      context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

  init {
    display = windowManager.defaultDisplay
  }

  /**
   * Registers the display listener. Should be called from [Activity.onResume].
   */
  fun onResume() {
    displayManager.registerDisplayListener(this, null)
  }

  /**
   * Unregisters the display listener. Should be called from [Activity.onPause].
   */
  fun onPause() {
    displayManager.unregisterDisplayListener(this)
  }

  /**
   * Records a change in surface dimensions. This will be later used by [ ][.updateSessionIfNeeded]. Should be called from [ ].
   *
   * @param width  the updated width of the surface.
   * @param height the updated height of the surface.
   */
  fun onSurfaceChanged(width: Int, height: Int) {
    viewportWidth = width
    viewportHeight = height
    viewportChanged = true
  }

  /**
   * Updates the session display geometry if a change was posted either by [ ][.onSurfaceChanged] call or by [.onDisplayChanged] system callback. This
   * function should be called explicitly before each call to [Session.update]. This
   * function will also clear the 'pending update' (viewportChanged) flag.
   *
   * @param session the [Session] object to update if display geometry changed.
   */
  fun updateSessionIfNeeded(session: Session) {
    if (viewportChanged) {
      val displayRotation = display.rotation
      session.setDisplayGeometry(displayRotation, viewportWidth, viewportHeight)
      viewportChanged = false
    }
  }

  /**
   * Returns the aspect ratio of the GL surface viewport while accounting for the display rotation
   * relative to the device camera sensor orientation.
   */
  @Suppress("unused")
  fun getCameraSensorRelativeViewportAspectRatio(cameraId: String): Float {
    val aspectRatio: Float
    val cameraSensorToDisplayRotation = getCameraSensorToDisplayRotation(cameraId)
    aspectRatio = when (cameraSensorToDisplayRotation) {
      90, 270 -> viewportHeight.toFloat() / viewportWidth.toFloat()
      0, 180 -> viewportWidth.toFloat() / viewportHeight.toFloat()
      else -> throw RuntimeException("Unhandled rotation: $cameraSensorToDisplayRotation")
    }
    return aspectRatio
  }

  /**
   * Returns the rotation of the back-facing camera with respect to the display. The value is one of
   * 0, 90, 180, 270.
   */
  private fun getCameraSensorToDisplayRotation(cameraId: String): Int {
    val characteristics: CameraCharacteristics
    characteristics = try {
      cameraManager.getCameraCharacteristics(cameraId)
    } catch (e: CameraAccessException) {
      throw RuntimeException("Unable to determine display orientation", e)
    }
    // Camera sensor orientation.
    val sensorOrientation =
        characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)
    // Current display orientation.
    val displayOrientation = toDegrees(display.rotation)
    sensorOrientation?.let {
      // Make sure we return 0, 90, 180, or 270 degrees.
      return (sensorOrientation - displayOrientation + 360) % 360
    } ?: throw RuntimeException("Unable to determine sensor orientation")
  }

  private fun toDegrees(rotation: Int): Int {
    return when (rotation) {
      Surface.ROTATION_0 -> 0
      Surface.ROTATION_90 -> 90
      Surface.ROTATION_180 -> 180
      Surface.ROTATION_270 -> 270
      else -> throw RuntimeException("Unknown rotation $rotation")
    }
  }

  override fun onDisplayAdded(displayId: Int) {}

  override fun onDisplayRemoved(displayId: Int) {}

  override fun onDisplayChanged(displayId: Int) {
    viewportChanged = true
  }
}