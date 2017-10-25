package com.example.wilko.songle

import android.content.Context
import android.os.AsyncTask
import org.xmlpull.v1.XmlPullParserException
import java.io.*
import java.net.HttpURLConnection
import java.net.URL

/**
 * Created by wilko on 10/13/2017.
 */

abstract class DownloadTask<E>(private val caller : DownloadCompleteListener<E>) :
        AsyncTask<String, Void, E?>() {

    override fun doInBackground(vararg urls: String): E? {
        return try {
            loadFromNetwork(urls[0])
        } catch (e: IOException) {
            //"Unable to load content. Check your network connection"//@todo
            null
        } catch (e: XmlPullParserException) {
            //"Error parsing XML"//@todo
            null
        }
    }

    abstract fun loadFromNetwork(urlString: String): E

    @Throws(IOException::class)
    fun downloadUrl(urlString: String): InputStream {
        val url = URL(urlString)
        val conn = url.openConnection() as HttpURLConnection
        // Also available: HttpsURLConnection
        conn.readTimeout = 10000 // milliseconds
        conn.connectTimeout = 15000 // milliseconds
        conn.requestMethod = "GET"
        conn.doInput = true
        // Starts the query
        conn.connect()
        return conn.inputStream
    }

    override fun onPostExecute(result: E?) {
        super.onPostExecute(result)
        caller.downloadComplete(result)
    }

    @Throws(IOException::class)
    fun fromStreamtoFile(context: Context, cacheFileName: String, inputStream: InputStream){
        try {
            val file = File(context.cacheDir, cacheFileName)
            val output = FileOutputStream(file)
            try {
                val buffer = ByteArray(4 * 1024)
                var read = inputStream.read(buffer)
                while (read != -1) {
                    output.write(buffer, 0, read)
                    read = inputStream.read(buffer)
                }
                output.flush()
            } finally {
                output.close()
            }
        } finally {
            inputStream.close()
        }
    }

    @Throws(IOException::class)
    fun fromStreamtoString(inputStream: InputStream): String{
        try {
            val bufferSize = 1024
            val buffer = CharArray(bufferSize)
            val out = StringBuilder()
            val reader = InputStreamReader(inputStream, "UTF-8")
            while (true) {
                val int = reader.read(buffer, 0, bufferSize)
                if (int < 0){
                    break
                }
                out.append(buffer, 0, int)
            }
            return out.toString()
        } finally {
            inputStream.close()
        }
    }
}