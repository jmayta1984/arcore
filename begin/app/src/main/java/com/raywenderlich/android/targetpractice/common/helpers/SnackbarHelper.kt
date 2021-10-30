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
import com.google.android.material.snackbar.BaseTransientBottomBar.BaseCallback
import com.google.android.material.snackbar.Snackbar

/**
 * Helper to manage the sample snackbar. Hides the Android boilerplate code, and exposes simpler
 * methods.
 */
class SnackbarHelper {

  private var messageSnackbar: Snackbar? = null

  private var lastMessage = ""
  private val isShowing: Boolean
    get() = messageSnackbar != null

  /**
   * Shows a snackbar with a given message.
   */
  fun showMessage(activity: Activity, message: String) {
    if (!message.isEmpty() && (!isShowing || lastMessage != message)) {
      lastMessage = message
      show(activity, message, DismissBehavior.HIDE)
    }
  }

  /**
   * Shows a snackbar with a given message, and a dismiss button.
   */
  @Suppress("unused")
  fun showMessageWithDismiss(activity: Activity, message: String?) {
    show(activity, message, DismissBehavior.SHOW)
  }

  /**
   * Shows a snackbar with a given error message. When dismissed, will finish the activity. Useful
   * for notifying errors, where no further interaction with the activity is possible.
   */
  fun showError(activity: Activity, errorMessage: String?) {
    show(activity, errorMessage, DismissBehavior.FINISH)
  }

  /**
   * Hides the currently showing snackbar, if there is one. Safe to call from any thread. Safe to
   * call even if snackbar is not shown.
   */
  fun hide(activity: Activity) {
    if (!isShowing) {
      return
    }
    lastMessage = ""
    val messageSnackbarToHide = messageSnackbar
    messageSnackbar = null
    activity.runOnUiThread { messageSnackbarToHide!!.dismiss() }
  }

  private fun show(
      activity: Activity, message: String?, dismissBehavior: DismissBehavior
  ) {
    activity.runOnUiThread {
      messageSnackbar = Snackbar.make(
          activity.findViewById(android.R.id.content),
          message!!,
          Snackbar.LENGTH_INDEFINITE
      )

      messageSnackbar?.let {
        it.view.setBackgroundColor(BACKGROUND_COLOR)

        if (dismissBehavior != DismissBehavior.HIDE) {
          it.setAction(
              "Dismiss"
          ) { messageSnackbar!!.dismiss() }
          if (dismissBehavior == DismissBehavior.FINISH) {
            it.addCallback(
                object : BaseCallback<Snackbar?>() {
                  override fun onDismissed(
                      transientBottomBar: Snackbar?,
                      event: Int
                  ) {
                    super.onDismissed(transientBottomBar, event)
                    activity.finish()
                  }
                })
          }
        }
        it.show()
      }
    }
  }

  companion object {
    private const val BACKGROUND_COLOR = -0x40cdcdce

    enum class DismissBehavior {
      HIDE, SHOW, FINISH
    }
  }

}