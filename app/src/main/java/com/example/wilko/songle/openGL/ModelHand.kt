package com.example.wilko.songle.openGL

import android.content.Context
import android.util.Log
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import javax.microedition.khronos.opengles.GL10

/**
 * Created by wilko on 10/2/2017.
 *
 * A 3D hand open GL model.
 *
 * credits to Karl (ThinMatrix), whose YouTube tutorial guide was followed
 * in the creation of the obj parser: https://www.youtube.com/watch?v=YKFYtekgnP8
 */

class ModelHand(private val context : Context, private val id : Int) {
    val TAG = "ModelHand"

    lateinit private var vertices : FloatArray
    lateinit private var indices : ShortArray
    private var vBuff : ByteBuffer
    private var vFloatBuff : FloatBuffer
    private var iBuff : ByteBuffer
    private var iShortBuff: ShortBuffer

    init {
        loadMesh()
        vBuff = ByteBuffer.allocateDirect(vertices.size*4) // byte sized, so *4
        vBuff.order(ByteOrder.nativeOrder())

        vFloatBuff = vBuff.asFloatBuffer()
        vFloatBuff.put(vertices)
        vFloatBuff.position(0)

        iBuff = ByteBuffer.allocateDirect(indices.size*2) // half byte sized, so *2
        iBuff.order(ByteOrder.nativeOrder())

        iShortBuff = iBuff.asShortBuffer()
        iShortBuff.put(indices)
        iShortBuff.position(0)
    }

    fun draw(gl: GL10){
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY)
        gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vFloatBuff)
        gl.glDrawElements(GL10.GL_TRIANGLES, indices.size, GL10.GL_UNSIGNED_SHORT, iShortBuff)
        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY)
    }

    // populating vertices and indices from an obj file
    fun loadMesh(){

        val verticesList = ArrayList<Float>()
        val indicesList = ArrayList<Int>()

        try {
            val fileInputStream = context.resources.openRawResource(id)
            val fileStreamReader = InputStreamReader(fileInputStream)
            val meshReader = BufferedReader(fileStreamReader)
            var line : String? = meshReader.readLine()

            while(line != null){
                // parsing the obj file to extract vertices and their indices
                val tokens : MutableList<String> = line.split(" ") as MutableList<String>

                line = meshReader.readLine()

                tokens.removeAll { x -> x.equals("")}


                if (tokens.size == 0 || tokens[0].equals("#"))
                    continue // skipping comments
                else if (tokens[0].equals("v")){
                    for (i in tokens.indices){
                        if (i == 0) continue // i == 0 on the letter "v", so skipping
                        verticesList.add(tokens[i].toFloat()) // otherwise adding to vertices
                    }
                }
                else if (tokens[0].equals("f")){
                    if (tokens.size == 4){
                        for (i in tokens.indices){
                            if (i == 0) continue
                            indicesList.add(tokens[i].split("/").first().toShort() - 1)
                            // -1 is necessary as our renderer takes zero indexed vertices
                        }
                    }
                    // the obj file I prepared also has square faces, unsupported by the renderer:
                    else if (tokens.size == 5){ //case face is a square
                        for (i in tokens.indices){
                            if ((i == 0) or (i == 2)) continue //forming one triangle
                            indicesList.add(tokens[i].split("/").first().toShort() - 1)
                        }
                        for (i in tokens.indices){
                            if ((i == 0) or (i == 4)) continue //and another
                            indicesList.add(tokens[i].split("/").first().toShort() - 1)
                        }
                    }

                }
            }

            meshReader.close()
            fileStreamReader.close()
            fileInputStream.close()

        }
        catch (e : IOException){
            Log.e(TAG, "couldn't load hand objs from resources")
        }

        //initialize and populate the indices and vertices arrays
        indices = ShortArray(indicesList.size)
        for (i in indices.indices) {
            indices.set(i, indicesList[i].toShort())
        }

        vertices = FloatArray(verticesList.size)
        for (i in vertices.indices){
            vertices.set(i, verticesList[i])
        }
    }
}