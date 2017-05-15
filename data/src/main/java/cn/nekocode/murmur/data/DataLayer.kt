package cn.nekocode.murmur.data

import android.content.Context
import cn.nekocode.murmur.data.util.CacheUtil
import com.danikula.videocache.HttpProxyCacheServer

/**
 * @author nekocode (nekocode.cn@gmail.com)
 */
object DataLayer {
    internal var APP: Context? = null
    lateinit var MEDIA_PROXY: HttpProxyCacheServer

    fun init(context: Context) {
        DataLayer.APP = context.applicationContext

        MEDIA_PROXY = HttpProxyCacheServer.Builder(APP)
                .maxCacheSize(512 * 1024 * 1024)
                .build()

        CacheUtil.init(context)
    }
}