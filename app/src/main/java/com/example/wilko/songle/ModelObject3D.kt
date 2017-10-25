package com.example.wilko.songle

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import javax.microedition.khronos.opengles.GL10

/**
 * Created by wilko on 10/2/2017.
 */

class ModelObject3D(private val context : Context, private val id : Int) {
    lateinit private var vertices : FloatArray
    lateinit private var indices : ShortArray
    private var vBuff : ByteBuffer
    private var vFloatBuff : FloatBuffer
    private var iBuff : ByteBuffer
    private var iShortBuff: ShortBuffer

    init {
        loadMesh()
        vBuff = ByteBuffer.allocateDirect(vertices.size*4)
        vBuff.order(ByteOrder.nativeOrder())

        vFloatBuff = vBuff.asFloatBuffer()
        vFloatBuff.put(vertices)
        vFloatBuff.position(0)

        iBuff = ByteBuffer.allocateDirect(indices.size*2)
        iBuff.order(ByteOrder.nativeOrder())

        iShortBuff = iBuff.asShortBuffer()
        iShortBuff.put(indices)
        iShortBuff.position(0)
    }

    fun draw(gl: GL10){
        //gl.glFrontFace(GL10.GL_CW)
        //gl.glEnable(GL10.GL_CULL_FACE)
        //gl.glCullFace(GL10.GL_BACK)
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY)
        gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vFloatBuff)
        gl.glDrawElements(GL10.GL_TRIANGLES, indices.size, GL10.GL_UNSIGNED_SHORT, iShortBuff)
        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY)
        //gl.glDisable(GL10.GL_CULL_FACE)
    }

    fun loadMesh(){

        var verticesList = ArrayList<Float>()
        val indecesList = ArrayList<Int>()

        try {
            val fileInputStream = context.resources.openRawResource(id)
            val fileStreamReader = InputStreamReader(fileInputStream)
            val meshReader = BufferedReader(fileStreamReader)
            var line : String? = meshReader.readLine()

            while(line != null){

                val tokens : MutableList<String> = line.split(" ") as MutableList<String>

                line = meshReader.readLine()

                tokens.removeAll { x -> x.equals("")}


                if (tokens.size == 0 || tokens[0].equals("#"))
                    continue
                else if (tokens[0].equals("v")){
                    for (i in tokens.indices){
                        if (i == 0) continue
                        verticesList.add(tokens[i].toFloat())
                    }
                }
                else if (tokens[0].equals("f")){
                    if (tokens.size == 4){
                        for (i in tokens.indices){
                            if (i == 0) continue
                            indecesList.add(tokens[i].split("/").first().toShort() - 1)
                        }
                    }
                    else if (tokens.size == 5){ //case face is a square
                        for (i in tokens.indices){
                            if ((i == 0) or (i == 2)) continue //forming one triangle
                            indecesList.add(tokens[i].split("/").first().toShort() - 1)
                        }
                        for (i in tokens.indices){
                            if ((i == 0) or (i == 4)) continue //and another
                            indecesList.add(tokens[i].split("/").first().toShort() - 1)
                        }
                    }

                }
            }

            meshReader.close()
            fileStreamReader.close()
            fileInputStream.close()

        }
        catch (e : Exception){
            e.printStackTrace()
            //exitProcess(1)//@todo
        }

        indices = ShortArray(indecesList.size)
        for (i in indices.indices) {
            indices.set(i, indecesList[i].toShort())
        }

        vertices = FloatArray(verticesList.size)
        for (i in vertices.indices){
            vertices.set(i, verticesList[i])
        }
    }
}