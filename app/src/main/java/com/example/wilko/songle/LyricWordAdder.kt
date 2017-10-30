package com.example.wilko.songle

import android.content.Context
import android.os.AsyncTask
import java.lang.ref.WeakReference

/**
 * Created by wilko on 10/29/2017.
 */

class LyricWordAdder(val row: Int, val column: Int, val wContext : WeakReference<Context>) :
        AsyncTask<String, Void, Boolean>() {

    override fun doInBackground(vararg urls: String): Boolean{
        val context = wContext.get()
        if (context != null) {
            val dbSongHandler = MySongDBHandler(context)
            val originalLyrics = dbSongHandler.getProp(getCurrSong(context), "lyric")
            //@todo in a batch, rather than 1 by 1
            val newString = StringBuilder()
            val lines = getLyric(context).split("\n")
            val origLines = originalLyrics.split("\n")
            for (lineNo in (0..lines.size-1)){
                val words = lines[lineNo].split("\t")
                val origWords = origLines[lineNo].split(" ")
                for (wordNo in (0..words.size-1)){
                    if ((lineNo + 1 == row) && (wordNo + 1 == column)){
                        if (wordNo == words.size -1){
                            newString.append(origWords[wordNo])
                        } else {
                            newString.append(origWords[wordNo]+"\t")
                        }
                    } else {
                        if (wordNo == words.size -1){
                            newString.append(words[wordNo])
                        } else {
                            newString.append(words[wordNo]+"\t")
                        }
                    }
                }
                if (lineNo != lines.size-1){
                    newString.append("\n")
                }
            }
            saveLyric(context, newString.toString())
            return true
        }
        return false
    }

    fun saveLyric(context: Context, string: String){
        val sharedPref = context.getSharedPreferences("permStrs", Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        editor.putString("lyric", string)
        editor.apply()
    }

    fun getLyric(context: Context): String{
        val sharedPref = context.getSharedPreferences("permStrs", Context.MODE_PRIVATE)
        return sharedPref.getString("lyric", "")
    }

    fun getCurrSong(context: Context): Int{
        val sharedPref = context.getSharedPreferences("permInts", Context.MODE_PRIVATE)
        return sharedPref.getInt("currentSong", 0)
    }
}