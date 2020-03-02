package com.ptktop.openfilefromstorageandroid.manager

import android.content.Context
import android.os.Environment
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import okio.*
import java.io.File
import java.io.IOException
import java.io.InterruptedIOException
import java.util.concurrent.TimeUnit

class DownloadFileAll(private val context : Context,
                      private val listener: DownloadListener, private val url: String,
                      private val fileTarget: String, private val fileName: String
) {

    fun run() {
        getDownloadObservable
            .subscribeOn(Schedulers.newThread())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(downloadObserver)
    }

    private val getDownloadObservable =
        Observable.fromCallable {
            val request = Request.Builder()
                .url(url)
                .build()
            setUpOkHttp().newCall(request).execute()
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())

    private val downloadObserver: Observer<Response> =
        object : Observer<Response> {
            override fun onSubscribe(d: Disposable) {}
            override fun onNext(response: Response) {
                if (response.isSuccessful) {
                    storageFile(response.body)
                } else {
                    listener.downloadFileComplete(false, "On Next >>> " + response.body)
                }
            }

            override fun onError(e: Throwable) {
                listener.downloadFileComplete(false, "On Error >>> " + e.message)
            }

            override fun onComplete() {
                listener.downloadFileComplete(true, "Complete")
            }
        }

    private fun setUpOkHttp(): OkHttpClient {
        val interceptor = HttpLoggingInterceptor()
        interceptor.level = HttpLoggingInterceptor.Level.BODY
        return OkHttpClient.Builder()
            .readTimeout(30, TimeUnit.MINUTES)
            .connectTimeout(30, TimeUnit.MINUTES)
            .addInterceptor(interceptor)
            .addNetworkInterceptor { chain ->
                val originalResponse = chain.proceed(chain.request())
                originalResponse.newBuilder()
                    .body(DownloadResponseBody(originalResponse.body, listener))
                    .build()
            }
            .build()
    }

    private fun storageFile(responseBody: ResponseBody?) {
        try {
            if (responseBody != null) {
                val file = File(Environment.getExternalStorageDirectory().toString() + fileTarget, fileName)
//                val file = File(context.applicationContext.filesDir.absolutePath.toString() + fileTarget, fileName)
                val sink = file.sink().buffer()
                sink.writeAll(responseBody.source())
                sink.close()
                listener.storageFileComplete(true, "Complete")
            }
        } catch (e: InterruptedIOException) {
            listener.storageFileComplete(false, "InterruptedIOException >>> " + e.message)
        } catch (e: IOException) {
            listener.storageFileComplete(false, "IOException >>> " + e.message)
        }
    }

    private class DownloadResponseBody(
        private val responseBody: ResponseBody?,
        private val downloadListener: DownloadListener?
    ) :
        ResponseBody() {
        private var bufferedSource: BufferedSource? = null
        override fun contentType(): MediaType? {
            return responseBody!!.contentType()
        }

        override fun contentLength(): Long {
            return responseBody!!.contentLength()
        }

        override fun source(): BufferedSource {
            if (bufferedSource == null) {
                bufferedSource = source(responseBody!!.source()).buffer()
            }
            return bufferedSource!!
        }

        private fun source(source: Source): Source {
            return object : ForwardingSource(source) {
                var totalBytesRead = 0L
                @Throws(IOException::class)
                override fun read(sink: Buffer, byteCount: Long): Long {
                    val bytesRead = super.read(sink, byteCount)
                    // read() returns the number of bytes read, or -1 if this source is exhausted.
                    totalBytesRead += if (bytesRead != -1L) bytesRead else 0
                    downloadListener!!.downloadProcess(
                        totalBytesRead,
                        responseBody!!.contentLength(),
                        bytesRead == -1L
                    )
                    return bytesRead
                }
            }
        }

    }

    interface DownloadListener {
        fun downloadProcess(
            bytesRead: Long,
            contentLength: Long,
            done: Boolean
        )

        fun downloadFileComplete(complete: Boolean, message: String?)

        fun storageFileComplete(complete: Boolean, message: String?)
    }
}