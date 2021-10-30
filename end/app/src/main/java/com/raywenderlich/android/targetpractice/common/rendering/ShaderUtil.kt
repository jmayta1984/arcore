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

package com.raywenderlich.android.targetpractice.common.rendering

import android.content.Context
import android.opengl.GLES20
import android.util.Log
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

/**
 * Shader helper functions.
 */
class ShaderUtil {

  companion object {
    /**
     * Converts a raw text file, saved as a resource, into an OpenGL ES shader.
     *
     * @param type     The type of shader we will be creating.
     * @param filename The filename of the asset file about to be turned into a shader.
     * @return The shader object handler.
     */
    @Throws(IOException::class)
    fun loadGLShader(tag: String?, context: Context, type: Int, filename: String): Int {
      val code = readShaderFileFromAssets(context, filename)
      var shader = GLES20.glCreateShader(type)
      GLES20.glShaderSource(shader, code)
      GLES20.glCompileShader(shader)
      // Get the compilation status.
      val compileStatus = IntArray(1)
      GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0)
      // If the compilation failed, delete the shader.
      if (compileStatus[0] == 0) {
        Log.e(tag, "Error compiling shader: " + GLES20.glGetShaderInfoLog(shader))
        GLES20.glDeleteShader(shader)
        shader = 0
      }
      if (shader == 0) {
        throw RuntimeException("Error creating shader.")
      }
      return shader
    }

    /**
     * Checks if we've had an error inside of OpenGL ES, and if so what that error is.
     *
     * @param label Label to report in case of error.
     * @throws RuntimeException If an OpenGL error is detected.
     */
    fun checkGLError(tag: String?, label: String) {
      var lastError = GLES20.GL_NO_ERROR
      // Drain the queue of all errors.
      var error: Int
      while (GLES20.glGetError().also { error = it } != GLES20.GL_NO_ERROR) {
        Log.e(tag, "$label: glError $error")
        lastError = error
      }
      if (lastError != GLES20.GL_NO_ERROR) {
        throw RuntimeException("$label: glError $lastError")
      }
    }

    /**
     * Converts a raw shader file into a string.
     *
     * @param filename The filename of the shader file about to be turned into a shader.
     * @return The context of the text file, or null in case of error.
     */
    @Throws(IOException::class)
    private fun readShaderFileFromAssets(context: Context, filename: String): String {
      context.assets.open(filename).use { inputStream ->
        BufferedReader(InputStreamReader(inputStream)).use { reader ->
          val sb = StringBuilder()

          do {
            // Return early if line is empty
            val line: String? = reader.readLine() ?: return sb.toString()

            val tokens = line!!.split(" ")
                .dropLastWhile { it.isEmpty() }.toTypedArray()
            if (tokens.isNotEmpty() && tokens[0] == "#include") {
              var includeFilename = tokens[1]
              includeFilename = includeFilename.replace("\"", "")
              if (includeFilename == filename) {
                throw IOException("Do not include the calling file.")
              }
              sb.append(readShaderFileFromAssets(context, includeFilename))
            } else {
              sb.append(line).append("\n")
            }
          } while (line != null)

          return sb.toString()
        }
      }
    }
  }

}