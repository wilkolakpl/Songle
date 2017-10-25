package com.example.wilko.songle

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import javax.microedition.khronos.opengles.GL10

/**
 * Created by wilko on 10/2/2017.
 */

class ModelTriangle {
    private var vertices: FloatArray = floatArrayOf(0F,4F,1F,2F,-1F,2F)
    private var indexes: ShortArray = shortArrayOf(0,1,2)
    private var colors: FloatArray = floatArrayOf(
            .3F,.2F,.6F,1F,
            0F,0F,0F,1F,
            0F,0F,0F,1F)

    private var vBuff : ByteBuffer = ByteBuffer.allocateDirect(vertices.size*4)
    private var v2Buff : FloatBuffer = vBuff.asFloatBuffer()

    private var iBuff : ByteBuffer = ByteBuffer.allocateDirect(indexes.size*2)
    private var i2Buff : ShortBuffer = iBuff.asShortBuffer()

    private var cBuff : ByteBuffer = ByteBuffer.allocateDirect(colors.size*4)
    private var c2Buff : FloatBuffer = cBuff.asFloatBuffer()

    init {
        vBuff.order(ByteOrder.nativeOrder())

        v2Buff.put(vertices)
        v2Buff.position(0)

        iBuff.order(ByteOrder.nativeOrder())

        i2Buff.put(indexes)
        i2Buff.position(0)

        cBuff.order(ByteOrder.nativeOrder())

        c2Buff.put(colors)
        c2Buff.position(0)
    }

    fun draw(gl: GL10){
        gl.glFrontFace(GL10.GL_CW)
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY)
        gl.glEnableClientState(GL10.GL_COLOR_ARRAY)
        gl.glVertexPointer(2, GL10.GL_FLOAT, 0, v2Buff)
        gl.glColorPointer(4, GL10.GL_FLOAT, 0, c2Buff)
        gl.glDrawElements(GL10.GL_TRIANGLES, indexes.size, GL10.GL_UNSIGNED_SHORT, i2Buff)
        gl.glDisableClientState(GL10.GL_COLOR_ARRAY)
        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY)
    }
}