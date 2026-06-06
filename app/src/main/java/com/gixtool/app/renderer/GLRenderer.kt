package com.gixtool.app.renderer

import android.content.Context
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.gixtool.app.util.PerformanceMonitor
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.sin

class GLRenderer(
    private val context: Context,
    private val performanceMonitor: PerformanceMonitor
) : GLSurfaceView.Renderer {

    // Transformation matrices
    private val mvpMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val rotationMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val tempMatrix = FloatArray(16)

    private var programId = 0
    private var cubeVbo = 0
    private var gridVbo = 0
    private var particleVbo = 0

    private var rotationAngle = 0f
    private var time = 0f
    private var screenWidth = 1
    private var screenHeight = 1

    // GLSL vertex shader – 3D/4D warp effect via W coordinate
    private val vertexShaderCode = """
        #version 300 es
        precision highp float;
        
        in vec4 a_Position;
        in vec4 a_Color;
        
        uniform mat4 u_MVPMatrix;
        uniform float u_Time;
        uniform float u_W;         // 4D "w" coordinate for hypercube projection
        
        out vec4 v_Color;
        out float v_Depth;
        
        void main() {
            // 4D rotation: rotate in XW plane for 4D effect
            float cosW = cos(u_W + u_Time * 0.3);
            float sinW = sin(u_W + u_Time * 0.3);
            
            vec4 pos4D = a_Position;
            float x4 = pos4D.x * cosW - pos4D.w * sinW;
            float w4 = pos4D.x * sinW + pos4D.w * cosW;
            pos4D.x = x4;
            pos4D.w = w4;
            
            // Project 4D -> 3D
            float wProj = 2.0 / (2.0 - pos4D.w);
            vec4 projected = vec4(pos4D.xyz * wProj, 1.0);
            
            // Pulse effect
            float pulse = 1.0 + 0.05 * sin(u_Time * 2.0);
            projected.xyz *= pulse;
            
            gl_Position = u_MVPMatrix * projected;
            gl_PointSize = 4.0;
            
            v_Color = a_Color;
            v_Depth = gl_Position.z;
        }
    """.trimIndent()

    private val fragmentShaderCode = """
        #version 300 es
        precision highp float;
        
        in vec4 v_Color;
        in float v_Depth;
        
        uniform float u_Time;
        
        out vec4 fragColor;
        
        void main() {
            // Neon glow based on depth
            float glow = clamp(1.0 - abs(v_Depth) * 0.3, 0.3, 1.0);
            vec3 neonColor = v_Color.rgb * glow;
            
            // Scanline effect
            float scanline = 0.9 + 0.1 * sin(gl_FragCoord.y * 3.14159 * 2.0 + u_Time * 5.0);
            
            fragColor = vec4(neonColor * scanline, v_Color.a * glow);
        }
    """.trimIndent()

    // Cube vertices: x,y,z,w (4D), r,g,b,a
    private val cubeVertices = floatArrayOf(
        // Front face - cyan
        -0.5f,  0.5f,  0.5f, 1f,   0f, 1f, 1f, 1f,
         0.5f,  0.5f,  0.5f, 1f,   0f, 1f, 1f, 1f,
         0.5f, -0.5f,  0.5f, 1f,   0f, 0.8f, 1f, 1f,
        -0.5f, -0.5f,  0.5f, 1f,   0f, 0.8f, 1f, 1f,
        -0.5f,  0.5f,  0.5f, 1f,   0f, 1f, 1f, 1f,
         0.5f, -0.5f,  0.5f, 1f,   0f, 0.8f, 1f, 1f,
        // Back face - magenta
        -0.5f,  0.5f, -0.5f, -1f,  1f, 0f, 1f, 1f,
         0.5f,  0.5f, -0.5f, -1f,  1f, 0f, 1f, 1f,
         0.5f, -0.5f, -0.5f, -1f,  0.8f, 0f, 1f, 1f,
        -0.5f, -0.5f, -0.5f, -1f,  0.8f, 0f, 1f, 1f,
        -0.5f,  0.5f, -0.5f, -1f,  1f, 0f, 1f, 1f,
         0.5f, -0.5f, -0.5f, -1f,  0.8f, 0f, 1f, 1f,
        // Top face - yellow
        -0.5f,  0.5f, -0.5f, 0f,   1f, 1f, 0f, 1f,
         0.5f,  0.5f, -0.5f, 0f,   1f, 1f, 0f, 1f,
         0.5f,  0.5f,  0.5f, 0f,   1f, 0.8f, 0f, 1f,
        -0.5f,  0.5f,  0.5f, 0f,   1f, 0.8f, 0f, 1f,
        -0.5f,  0.5f, -0.5f, 0f,   1f, 1f, 0f, 1f,
         0.5f,  0.5f,  0.5f, 0f,   1f, 0.8f, 0f, 1f,
        // Bottom face - orange
        -0.5f, -0.5f, -0.5f, 0f,   1f, 0.5f, 0f, 1f,
         0.5f, -0.5f, -0.5f, 0f,   1f, 0.5f, 0f, 1f,
         0.5f, -0.5f,  0.5f, 0f,   1f, 0.4f, 0f, 1f,
        -0.5f, -0.5f,  0.5f, 0f,   1f, 0.4f, 0f, 1f,
        -0.5f, -0.5f, -0.5f, 0f,   1f, 0.5f, 0f, 1f,
         0.5f, -0.5f,  0.5f, 0f,   1f, 0.4f, 0f, 1f,
        // Right face - green
         0.5f,  0.5f, -0.5f, 0f,   0f, 1f, 0.5f, 1f,
         0.5f,  0.5f,  0.5f, 0f,   0f, 1f, 0.5f, 1f,
         0.5f, -0.5f,  0.5f, 0f,   0f, 0.8f, 0.3f, 1f,
         0.5f, -0.5f, -0.5f, 0f,   0f, 0.8f, 0.3f, 1f,
         0.5f,  0.5f, -0.5f, 0f,   0f, 1f, 0.5f, 1f,
         0.5f, -0.5f,  0.5f, 0f,   0f, 0.8f, 0.3f, 1f,
        // Left face - blue
        -0.5f,  0.5f,  0.5f, 0f,   0.3f, 0.5f, 1f, 1f,
        -0.5f,  0.5f, -0.5f, 0f,   0.3f, 0.5f, 1f, 1f,
        -0.5f, -0.5f, -0.5f, 0f,   0.2f, 0.4f, 1f, 1f,
        -0.5f, -0.5f,  0.5f, 0f,   0.2f, 0.4f, 1f, 1f,
        -0.5f,  0.5f,  0.5f, 0f,   0.3f, 0.5f, 1f, 1f,
        -0.5f, -0.5f, -0.5f, 0f,   0.2f, 0.4f, 1f, 1f,
    )

    private val STRIDE = 8 * 4 // 8 floats * 4 bytes
    private val POSITION_OFFSET = 0
    private val COLOR_OFFSET = 4 * 4

    private var positionHandle = 0
    private var colorHandle = 0
    private var mvpHandle = 0
    private var timeHandle = 0
    private var wHandle = 0

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES30.glClearColor(0.02f, 0.02f, 0.08f, 1.0f)
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)

        programId = buildProgram(vertexShaderCode, fragmentShaderCode)

        positionHandle = GLES30.glGetAttribLocation(programId, "a_Position")
        colorHandle = GLES30.glGetAttribLocation(programId, "a_Color")
        mvpHandle = GLES30.glGetUniformLocation(programId, "u_MVPMatrix")
        timeHandle = GLES30.glGetUniformLocation(programId, "u_Time")
        wHandle = GLES30.glGetUniformLocation(programId, "u_W")

        val vboIds = IntArray(1)
        GLES30.glGenBuffers(1, vboIds, 0)
        cubeVbo = vboIds[0]

        val vertexBuffer: FloatBuffer = ByteBuffer
            .allocateDirect(cubeVertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(cubeVertices)
                position(0)
            }

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, cubeVbo)
        GLES30.glBufferData(
            GLES30.GL_ARRAY_BUFFER,
            cubeVertices.size * 4,
            vertexBuffer,
            GLES30.GL_STATIC_DRAW
        )
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES30.glViewport(0, 0, width, height)
        screenWidth = width
        screenHeight = height
        val ratio = width.toFloat() / height.toFloat()
        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1f, 1f, 2f, 20f)
    }

    override fun onDrawFrame(gl: GL10?) {
        performanceMonitor.onFrame()
        time += 0.016f
        rotationAngle += 0.5f

        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)

        Matrix.setLookAtM(viewMatrix, 0,
            0f, 0f, 4f,
            0f, 0f, 0f,
            0f, 1f, 0f)

        // Draw hypercube (3D visible portion)
        drawHypercube(1f)
        // Draw inner cube (smaller, rotated differently — 4D twin)
        drawHypercube(0.4f)
    }

    private fun drawHypercube(scale: Float) {
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.scaleM(modelMatrix, 0, scale, scale, scale)
        Matrix.setRotateM(rotationMatrix, 0, rotationAngle * (if (scale > 0.5f) 1f else -1.5f), 1f, 1f, 0.5f)
        Matrix.multiplyMM(tempMatrix, 0, modelMatrix, 0, rotationMatrix, 0)
        Matrix.multiplyMM(tempMatrix, 0, viewMatrix, 0, tempMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, tempMatrix, 0)

        GLES30.glUseProgram(programId)
        GLES30.glUniformMatrix4fv(mvpHandle, 1, false, mvpMatrix, 0)
        GLES30.glUniform1f(timeHandle, time)
        GLES30.glUniform1f(wHandle, if (scale > 0.5f) 0f else 1f)

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, cubeVbo)

        GLES30.glEnableVertexAttribArray(positionHandle)
        GLES30.glVertexAttribPointer(positionHandle, 4, GLES30.GL_FLOAT, false, STRIDE, POSITION_OFFSET)

        GLES30.glEnableVertexAttribArray(colorHandle)
        GLES30.glVertexAttribPointer(colorHandle, 4, GLES30.GL_FLOAT, false, STRIDE, COLOR_OFFSET)

        GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, cubeVertices.size / 8)

        GLES30.glDisableVertexAttribArray(positionHandle)
        GLES30.glDisableVertexAttribArray(colorHandle)
    }

    private fun buildProgram(vertSrc: String, fragSrc: String): Int {
        val vert = compileShader(GLES30.GL_VERTEX_SHADER, vertSrc)
        val frag = compileShader(GLES30.GL_FRAGMENT_SHADER, fragSrc)
        return GLES30.glCreateProgram().also { prog ->
            GLES30.glAttachShader(prog, vert)
            GLES30.glAttachShader(prog, frag)
            GLES30.glLinkProgram(prog)
        }
    }

    private fun compileShader(type: Int, src: String): Int {
        return GLES30.glCreateShader(type).also { shader ->
            GLES30.glShaderSource(shader, src)
            GLES30.glCompileShader(shader)
        }
    }
}
