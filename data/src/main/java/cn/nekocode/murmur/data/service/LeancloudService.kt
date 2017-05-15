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
internal object LeancloudService {
    // OkHttp Config
    const val RESPONSE_CACHE_FILE: String = "LEANCLOUD_SERVICE_CACHE"
    const val RESPONSE_CACHE_SIZE = 10 * 1024 * 1024L
    const val HTTP_CONNECT_TIMEOUT = 10L
    const val HTTP_READ_TIMEOUT = 30L
    const val HTTP_WRITE_TIMEOUT = 10L

    const val API_HOST_URL = "https://api.leancloud.cn/1.1/"
    const val APP_ID = "njtyqtww55i0fikg9zgzuq5cayrbi7u85uiolfjoadch2pse"
    const val APP_KEY = "ld1826dyuz53gxd4vx84j60lq9mg5860ksznirff41y2sau9"

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

    val REST_ADAPTER: Retrofit = Retrofit.Builder()
            .baseUrl(API_HOST_URL)
            .addConverterFactory(GsonConverterFactory.create(GSON))
            .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
            .client(HTTP_CLIENT)
            .build()

    class ResponseWrapper<out T>(val results: List<T>)
}
