package com.example.wilko.songle.openGL

import android.content.Context
import android.opengl.GLSurfaceView
import android.opengl.GLU
import android.os.SystemClock
import com.example.wilko.songle.App
import com.example.wilko.songle.R
import org.jetbrains.anko.defaultSharedPreferences
import javax.microedition.khronos.opengles.GL10
import javax.microedition.khronos.egl.EGLConfig

/**
 * Created by wilko on 10/2/2017.
 *
 * A open GL renderer.
 *
 * credits to Bucky Roberts, whose YouTube tutorial guide was followed in the creation of this class
 * https://www.youtube.com/watch?v=u58DwKPzBoY
 */

class MyRenderer(context : Context) : GLSurfaceView.Renderer {
    private var color = 0F
    private var colorVelocity = 1F/60F
    private var stateFlag = 0

    private var modelCube: ModelCube = ModelCube()
    private var hand: ModelHand = ModelHand(context, R.raw.hand)
    private var handye: ModelHand = ModelHand(context, R.raw.handye)
    private lateinit var handfu: ModelHand

    init { // set the correct model, with or w/o profanity
        changeProfanity()
    }

    override fun onSurfaceCreated(gl: GL10, eglConfig: EGLConfig){
        gl.glDisable(GL10.GL_DITHER)
        gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_FASTEST)
        gl.glClearDepthf(1F)
    }

    override fun onDrawFrame(gl: GL10){
        // color change
        if (color > 1 || color < 0){
            colorVelocity = -colorVelocity
        }
        color += colorVelocity

        gl.glClear(GL10.GL_COLOR_BUFFER_BIT or GL10.GL_DEPTH_BUFFER_BIT)
        gl.glMatrixMode(GL10.GL_MODELVIEW)
        gl.glLoadIdentity()
        GLU.gluLookAt(gl, 0F, 2F, -9F, 0F,0F,0F, 0F,2F,0F)

        gl.glClearColor(color*0.5F, color*0.5F, color*0.2F, 1F)

        // rotation
        val time : Long = SystemClock.uptimeMillis() % 4000L
        val angle : Float = .09F * time.toInt()

        gl.glRotatef(angle,0F,1F,0F)

        // choosing to display one of the 3 hand models
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

    fun changeProfanity(){
        // set the correct model, with or w/o profanity
        val sharedPref = App.instance.defaultSharedPreferences
        val profanity = sharedPref.getBoolean("profanity", true)
        if (profanity){
            handfu = ModelHand(App.instance, R.raw.handfu)
        } else {
            handfu = ModelHand(App.instance, R.raw.handfu2)
        }
    }
}