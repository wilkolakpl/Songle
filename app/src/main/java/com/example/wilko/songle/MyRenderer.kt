package com.example.wilko.songle

import android.content.Context
import android.opengl.GLSurfaceView
import android.opengl.GLU
import android.os.SystemClock
import javax.microedition.khronos.opengles.GL10
import javax.microedition.khronos.egl.EGLConfig

/**
 * Created by wilko on 10/2/2017.
 */

class MyRenderer(context : Context) : GLSurfaceView.Renderer {
    private var color = 0F
    private var colorVelocity = 1F/60F
    private var stateFlag = 0

    private var modelCube: ModelCube = ModelCube()
    private var hand: ModelObject3D = ModelObject3D(context, R.raw.hand)
    private var handfu: ModelObject3D = ModelObject3D(context, R.raw.handfu)
    private var handye: ModelObject3D = ModelObject3D(context, R.raw.handye)

    override fun onSurfaceCreated(gl: GL10, eglConfig: EGLConfig){
        gl.glDisable(GL10.GL_DITHER)
        gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_FASTEST)
        gl.glClearDepthf(1F)
    }

    override fun onDrawFrame(gl: GL10){
        if (color > 1 || color < 0){
            colorVelocity = -colorVelocity
        }
        color += colorVelocity

        gl.glClear(GL10.GL_COLOR_BUFFER_BIT or GL10.GL_DEPTH_BUFFER_BIT)
        gl.glMatrixMode(GL10.GL_MODELVIEW)
        gl.glLoadIdentity()
        GLU.gluLookAt(gl, 0F, 2F, -9F, 0F,0F,0F, 0F,2F,0F)

        gl.glClearColor(color*0.5F, color*0.5F, color*0.2F, 1F)


        val time : Long = SystemClock.uptimeMillis() % 4000L
        val angle : Float = .09F * time.toInt()

        gl.glRotatef(angle,0F,1F,0F)

        if (stateFlag == 0){
            hand.draw(gl)
        } else if (stateFlag == 1){
            handfu.draw(gl)
        } else if (stateFlag == 2){
            handye.draw(gl)
        }
        modelCube.draw(gl)

    }
    override fun onSurfaceChanged(gl: GL10, width : Int, height : Int){
        gl.glViewport(0,0, width, height)
        val ratio : Float = width/height.toFloat()
        gl.glMatrixMode(GL10.GL_PROJECTION)
        gl.glLoadIdentity()
        gl.glFrustumf(-ratio, ratio, -1F, 1F, 1F, 100F)
    }

    fun changeStateFlag(newState : Int){
        stateFlag = newState
    }
}