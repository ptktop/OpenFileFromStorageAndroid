package com.ptktop.openfilefromstorageandroid.data.network

import android.annotation.SuppressLint
import com.ptktop.openfilefromstorageandroid.data.network.api.PictureApi
import io.reactivex.schedulers.Schedulers

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class ServiceCreateCall {

    companion object {
        @SuppressLint("StaticFieldLeak")
        private var instance: ServiceCreateCall? = null

        fun getInstance() =
            instance ?: synchronized(this) {
                instance ?: ServiceCreateCall().also { instance = it }
            }
    }

    private fun setUpOkHttp(): OkHttpClient {
        val interceptor = HttpLoggingInterceptor()
        interceptor.level = HttpLoggingInterceptor.Level.BODY
        return OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .readTimeout(30, TimeUnit.MINUTES)
            .connectTimeout(30, TimeUnit.MINUTES)
            .build()
    }

    private fun setUpRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://picsum.photos")
            .addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()))
            .addConverterFactory(GsonConverterFactory.create())
            .client(setUpOkHttp())
            .build()
    }

    fun getPictureApiService(): PictureApi {
        return setUpRetrofit().create(PictureApi::class.java)
    }
}