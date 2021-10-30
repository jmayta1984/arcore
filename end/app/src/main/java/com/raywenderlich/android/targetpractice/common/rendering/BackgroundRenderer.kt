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
import android.opengl.GLES11Ext
import android.opengl.GLES20
import com.google.ar.core.Coordinates2d
import com.google.ar.core.Frame
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * This class renders the AR background from camera feed. It creates and hosts the texture given to
 * ARCore to be filled with the camera image.
 */
class BackgroundRenderer {

  private var quadCoords: FloatBuffer? = null
  private var quadTexCoords: FloatBuffer? = null
  private var quadProgram = 0
  private var quadPositionParam = 0
  private var quadTexCoordParam = 0
  var textureId = -1
    private set

  /**
   * Allocates and initializes OpenGL resources needed by the background renderer. Must be called on
   * the OpenGL thread, typically in [GLSurfaceView.Renderer.onSurfaceCreated].
   *
   * @param context Needed to access shader source.
   */
  @Throws(IOException::class)
  fun createOnGlThread(context: Context?) {
    if (context != null) {
      // Generate the background texture.
      val textures = IntArray(1)
      GLES20.glGenTextures(1, textures, 0)
      textureId = textures[0]

      val textureTarget = GLES11Ext.GL_TEXTURE_EXTERNAL_OES
      GLES20.glBindTexture(textureTarget, textureId)
      GLES20.glTexParameteri(textureTarget, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
      GLES20.glTexParameteri(textureTarget, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
      GLES20.glTexParameteri(textureTarget, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
      GLES20.glTexParameteri(textureTarget, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)

      val numVertices = 4
      if (numVertices != QUAD_COORDS.size / COORDS_PER_VERTEX) {
        throw RuntimeException("Unexpected number of vertices in BackgroundRenderer.")
      }

      val bbCoords =
          ByteBuffer.allocateDirect(QUAD_COORDS.size * FLOAT_SIZE)
      bbCoords.order(ByteOrder.nativeOrder())
      quadCoords = bbCoords.asFloatBuffer()
      quadCoords!!.put(QUAD_COORDS)
      quadCoords!!.position(0)

      val bbTexCoordsTransformed =
          ByteBuffer.allocateDirect(numVertices * TEXCOORDS_PER_VERTEX * FLOAT_SIZE)
      bbTexCoordsTransformed.order(ByteOrder.nativeOrder())
      quadTexCoords = bbTexCoordsTransformed.asFloatBuffer()

      val vertexShader = ShaderUtil.loadGLShader(
          TAG,
          context,
          GLES20.GL_VERTEX_SHADER,
          VERTEX_SHADER_NAME
      )

      val fragmentShader = ShaderUtil.loadGLShader(
          TAG,
          context,
          GLES20.GL_FRAGMENT_SHADER,
          FRAGMENT_SHADER_NAME
      )

      quadProgram = GLES20.glCreateProgram()
      GLES20.glAttachShader(quadProgram, vertexShader)
      GLES20.glAttachShader(quadProgram, fragmentShader)
      GLES20.glLinkProgram(quadProgram)
      GLES20.glUseProgram(quadProgram)
      ShaderUtil.checkGLError(TAG, "Program creation")

      quadPositionParam = GLES20.glGetAttribLocation(quadProgram, "a_Position")
      quadTexCoordParam = GLES20.glGetAttribLocation(quadProgram, "a_TexCoord")
      ShaderUtil.checkGLError(TAG, "Program parameters")
    }
  }

  /**
   * Draws the AR background image. The image will be drawn such that virtual content rendered with
   * the matrices provided by [com.google.ar.core.Camera.getViewMatrix] and
   * [com.google.ar.core.Camera.getProjectionMatrix] will
   * accurately follow static physical objects. This must be called **before** drawing virtual
   * content.
   *
   * @param frame The current `Frame` as returned by [Session.update].
   */
  fun draw(frame: Frame) {
    if (frame.hasDisplayGeometryChanged()) {
      frame.transformCoordinates2d(
          Coordinates2d.OPENGL_NORMALIZED_DEVICE_COORDINATES,
          quadCoords,
          Coordinates2d.TEXTURE_NORMALIZED,
          quadTexCoords
      )
    }

    if (frame.timestamp == 0L) {
      return
    }

    draw()
  }

  /**
   * Draws the camera image using the currently configured [BackgroundRenderer.quadTexCoords]
   * image texture coordinates.
   *
   *
   * The image will be center cropped if the camera sensor aspect ratio does not match the screen
   * aspect ratio, which matches the cropping behavior of [ ][Frame.transformCoordinates2d].
   */
  @Suppress("unused")
  fun draw(
      imageWidth: Int,
      imageHeight: Int,
      screenAspectRatio: Float,
      cameraToDisplayRotation: Int
  ) {
    // Crop the camera image to fit the screen aspect ratio.
    val imageAspectRatio = imageWidth.toFloat() / imageHeight
    val croppedWidth: Float
    val croppedHeight: Float
    if (screenAspectRatio < imageAspectRatio) {
      croppedWidth = imageHeight * screenAspectRatio
      croppedHeight = imageHeight.toFloat()
    } else {
      croppedWidth = imageWidth.toFloat()
      croppedHeight = imageWidth / screenAspectRatio
    }

    val u = (imageWidth - croppedWidth) / imageWidth * 0.5f
    val v = (imageHeight - croppedHeight) / imageHeight * 0.5f
    val texCoordTransformed: FloatArray
    texCoordTransformed = when (cameraToDisplayRotation) {
      90 -> floatArrayOf(1 - u, 1 - v, u, 1 - v, 1 - u, v, u, v)
      180 -> floatArrayOf(1 - u, v, 1 - u, 1 - v, u, v, u, 1 - v)
      270 -> floatArrayOf(u, v, 1 - u, v, u, 1 - v, 1 - u, 1 - v)
      0 -> floatArrayOf(u, 1 - v, u, v, 1 - u, 1 - v, 1 - u, v)
      else -> throw IllegalArgumentException("Unhandled rotation: $cameraToDisplayRotation")
    }

    // Write image texture coordinates.
    quadTexCoords!!.position(0)
    quadTexCoords!!.put(texCoordTransformed)

    draw()
  }

  /**
   * Draws the camera background image using the currently configured [ ][BackgroundRenderer.quadTexCoords] image texture coordinates.
   */
  private fun draw() {
    // Ensure position is rewound before use.
    quadTexCoords!!.position(0)

    GLES20.glDisable(GLES20.GL_DEPTH_TEST)
    GLES20.glDepthMask(false)
    GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
    GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
    GLES20.glUseProgram(quadProgram)

    // Set the vertex positions.
    GLES20.glVertexAttribPointer(
        quadPositionParam,
        COORDS_PER_VERTEX,
        GLES20.GL_FLOAT,
        false,
        0,
        quadCoords
    )

    // Set the texture coordinates.
    GLES20.glVertexAttribPointer(
        quadTexCoordParam,
        TEXCOORDS_PER_VERTEX,
        GLES20.GL_FLOAT,
        false,
        0,
        quadTexCoords
    )

    // Enable vertex arrays
    GLES20.glEnableVertexAttribArray(quadPositionParam)
    GLES20.glEnableVertexAttribArray(quadTexCoordParam)
    GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

    // Disable vertex arrays
    GLES20.glDisableVertexAttribArray(quadPositionParam)
    GLES20.glDisableVertexAttribArray(quadTexCoordParam)

    // Restore the depth state for further drawing.
    GLES20.glDepthMask(true)
    GLES20.glEnable(GLES20.GL_DEPTH_TEST)
    ShaderUtil.checkGLError(TAG, "BackgroundRendererDraw")
  }

  companion object {
    private val TAG = BackgroundRenderer::class.java.simpleName

    // Shader names.
    private const val VERTEX_SHADER_NAME = "shaders/screenquad.vert"
    private const val FRAGMENT_SHADER_NAME = "shaders/screenquad.frag"

    private const val COORDS_PER_VERTEX = 2
    private const val TEXCOORDS_PER_VERTEX = 2
    private const val FLOAT_SIZE = 4
    private val QUAD_COORDS = floatArrayOf(
        -1.0f, -1.0f, -1.0f, +1.0f, +1.0f, -1.0f, +1.0f, +1.0f
    )
  }

}