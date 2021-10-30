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

import android.app.Activity
import android.view.WindowManager
import com.google.ar.core.Camera
import com.google.ar.core.TrackingFailureReason
import com.google.ar.core.TrackingState

/**
 * Gets human readibly tracking failure reasons and suggested actions.
 */
class TrackingStateHelper(private val activity: Activity) {

  private var previousTrackingState: TrackingState? = null

  /**
   * Keep the screen unlocked while tracking, but allow it to lock when tracking stops.
   */
  fun updateKeepScreenOnFlag(trackingState: TrackingState) {
    if (trackingState == previousTrackingState) {
      return
    }
    previousTrackingState = trackingState
    when (trackingState) {
      TrackingState.PAUSED, TrackingState.STOPPED -> activity.runOnUiThread {
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
      }
      TrackingState.TRACKING -> activity.runOnUiThread {
        activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
      }
    }
  }

  companion object {
    private const val INSUFFICIENT_FEATURES_MESSAGE =
        "Can't find anything. Aim device at a surface with more texture or color."
    private const val EXCESSIVE_MOTION_MESSAGE = "Moving too fast. Slow down."
    private const val INSUFFICIENT_LIGHT_MESSAGE =
        "Too dark. Try moving to a well-lit area."
    private const val BAD_STATE_MESSAGE =
        "Tracking lost due to bad internal state. Please try restarting the AR experience."

    fun getTrackingFailureReasonString(camera: Camera): String {
      return when (val reason = camera.trackingFailureReason) {
        TrackingFailureReason.NONE -> ""
        TrackingFailureReason.BAD_STATE -> BAD_STATE_MESSAGE
        TrackingFailureReason.INSUFFICIENT_LIGHT -> INSUFFICIENT_LIGHT_MESSAGE
        TrackingFailureReason.EXCESSIVE_MOTION -> EXCESSIVE_MOTION_MESSAGE
        TrackingFailureReason.INSUFFICIENT_FEATURES -> INSUFFICIENT_FEATURES_MESSAGE
        else -> "Unknown tracking failure reason: $reason"
      }
    }
  }

}