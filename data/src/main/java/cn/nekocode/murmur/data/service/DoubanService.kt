package cn.nekocode.murmur.data.service

import cn.nekocode.murmur.data.DataLayer
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.Cache
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * @author nekocode (nekocode.cn@gmail.com)
 */
internal object DoubanService {
    // OkHttp Config
    const val RESPONSE_CACHE_FILE: String = "DOUBAN_SERVICE_CACHE"
    const val RESPONSE_CACHE_SIZE = 10 * 1024 * 1024L
    const val HTTP_CONNECT_TIMEOUT = 10L
    const val HTTP_READ_TIMEOUT = 30L
    const val HTTP_WRITE_TIMEOUT = 10L

    const val API_HOST_URL = "https://api.douban.com"
    const val TOKEN_HOST_URL = "https://www.douban.com"

    const val APP_NAME = "radio_android"
    const val VERSION = "642"
    const val KEY = "02f7751a55066bcb08e65f4eff134361"
    const val SECRET = "63cf04ebd7b0ff3b"
    const val REDIRECT_URI = "http://douban.fm"
    const val PUSH_DEVICE_ID = "534fa03e331b42dbb7487e8784ce50cbbf0acf13"   // len: 40

    val HTTP_CLIENT: OkHttpClient = OkHttpClient.Builder()
            .apply {
                DataLayer.APP?.let {
                    cache(Cache(File(it.cacheDir, RESPONSE_CACHE_FILE),
                            RESPONSE_CACHE_SIZE))
                }
            }
            .connectTimeout(HTTP_CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(HTTP_WRITE_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(HTTP_READ_TIMEOUT, TimeUnit.SECONDS)
            .build()

    val GSON: Gson = GsonBuilder().setDateFormat("yyyy'-'MM'-'dd'T'HH':'mm':'ss'.'SSS'Z'").create()

    val API_REST_ADAPTER: Retrofit = Retrofit.Builder()
            .baseUrl(API_HOST_URL)
            .addConverterFactory(GsonConverterFactory.create(GSON))
            .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
            .client(HTTP_CLIENT)
            .build()

    val TOKEN_REST_ADAPTER: Retrofit = Retrofit.Builder()
            .baseUrl(TOKEN_HOST_URL)
            .addConverterFactory(GsonConverterFactory.create(GSON))
            .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
            .client(HTTP_CLIENT)
            .build()

}
