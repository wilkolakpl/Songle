package com.example.wilko.songle

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import javax.microedition.khronos.opengles.GL10

/**
 * Created by wilko on 10/2/2017.
 *
 * A hard-coded 3D open GL cube, with a purple gradient on it.
 *
 * credits to Bucky Roberts, whose YouTube tutorial guide was followed in the creation of this class
 * https://www.youtube.com/watch?v=u58DwKPzBoY
 */

class ModelCube {
    private var vertices: FloatArray = floatArrayOf(
            2F,-5F,-2F,
            2F,-7F,-2F,
            -2F,-7F,-2F,
            -2F,-5F,-2F,
            2F,-5F,2F,
            2F,-7F,2F,
            -2F,-7F,2F,
            -2F,-5F,2F)

    private var indexes: ShortArray = shortArrayOf(3,4,0,0,4,1,3,0,1,3,7,4,7,6,4,7,3,6,
                                                   3,1,2,1,6,2,6,3,2,1,4,5,5,6,1,6,5,4)

    private var colors: FloatArray = floatArrayOf(
            1F,1F,1F,1F,
            .3F,.2F,.6F,1F,
            .3F,.2F,.6F,1F,
            1F,1F,1F,1F,
            1F,1F,1F,1F,
            .3F,.2F,.6F,1F,
            .3F,.2F,.6F,1F,
            1F,1F,1F,1F)

    private var vBuff : ByteBuffer
    private var v2Buff : FloatBuffer

    private var iBuff : ByteBuffer
    private var i2Buff : ShortBuffer

    private var cBuff : ByteBuffer
    private var c2Buff : FloatBuffer

    init {
        vBuff = ByteBuffer.allocateDirect(vertices.size*4)
        vBuff.order(ByteOrder.nativeOrder())

        v2Buff = vBuff.asFloatBuffer()
        v2Buff.put(vertices)
        v2Buff.position(0)

        iBuff = ByteBuffer.allocateDirect(indexes.size*2)
        iBuff.order(ByteOrder.nativeOrder())

        i2Buff = iBuff.asShortBuffer()
        i2Buff.put(indexes)
        i2Buff.position(0)

        cBuff = ByteBuffer.allocateDirect(colors.size*4)
        cBuff.order(ByteOrder.nativeOrder())

        c2Buff = cBuff.asFloatBuffer()
        c2Buff.put(colors)
        c2Buff.position(0)
    }

    fun draw(gl: GL10){
        gl.glFrontFace(GL10.GL_CW)
        gl.glEnable(GL10.GL_CULL_FACE)
        gl.glCullFace(GL10.GL_FRONT)
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY)
        gl.glEnableClientState(GL10.GL_COLOR_ARRAY)
        gl.glVertexPointer(3, GL10.GL_FLOAT, 0, v2Buff)
        gl.glColorPointer(4, GL10.GL_FLOAT, 0, c2Buff)
        gl.glDrawElements(GL10.GL_TRIANGLES, indexes.size, GL10.GL_UNSIGNED_SHORT, i2Buff)
        gl.glDisableClientState(GL10.GL_COLOR_ARRAY)
        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY)
        gl.glDisable(GL10.GL_CULL_FACE)
    }
}