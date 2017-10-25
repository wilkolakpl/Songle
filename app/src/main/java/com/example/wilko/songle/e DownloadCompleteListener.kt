package com.example.wilko.songle

/**
 * Created by wilko on 10/13/2017.
 */

interface DownloadCompleteListener<E> {
    fun downloadComplete(result: E?)
}