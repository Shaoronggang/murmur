package cn.nekocode.murmur.data.util

import android.content.Context
import io.paperdb.Paper
import rx.Observable
import rx.schedulers.Schedulers
import java.util.concurrent.TimeUnit

/**
 * @author nekocode (nekocode.cn@gmail.com)
 */
object CacheUtil {
    private class Cacheable<out T : Any>(val cached: T, val cachedTime: Long)

    /**
     * 初始化
     */
    fun init(context: Context) {
        Paper.init(context)
    }

    /**
     * 读取 Cache
     */
    fun <T : Any> rxRead(key: String, lifetime: Int = -1, timeUnit: TimeUnit = TimeUnit.HOURS): Observable<T?> {
        return Observable.fromCallable {
            read<T>(key, lifetime, timeUnit)
        }.subscribeOn(Schedulers.io())
    }

    fun <T : Any> read(key: String, lifetime: Int = -1, timeUnit: TimeUnit = TimeUnit.HOURS): T? {
        val cached = _read<Any>(key) ?: return null

        val rlt = if (lifetime == -1) {
            // 未设置时限
            cached.cached

        } else if (System.currentTimeMillis() - timeUnit.toMillis(cached.cachedTime) < lifetime) {
            // 缓存未超时
            cached.cached

        } else {
            null
        }

        return rlt as T?
    }

    /**
     * 写入 Cache
     */
    fun <T : Any> rxWrite(key: String, origin: T?): Observable<Unit> {
        return Observable.fromCallable {
            write<T>(key, origin)
        }.subscribeOn(Schedulers.io())
    }

    fun <T : Any> write(key: String, origin: T?) {
        if (origin != null) {
            val caching = Cacheable(origin, System.currentTimeMillis())
            write(key, caching)

        } else {
            write(key, null)
        }
    }

    /**
     * 清除 Cache
     */
    fun rxClear(): Observable<Unit> {
        return Observable.fromCallable {
            clear()
        }.subscribeOn(Schedulers.io())
    }

    fun clear() {
        Paper.book().destroy()
    }

    /**
     * 内部函数
     */

    private fun <T : Any> _read(key: String): Cacheable<T>? {
        try {
            return Paper.book().read<Cacheable<T>?>(key)
        } catch (e: Exception) {
            _write(key, null) // 出错的话清空数据
            return null
        }
    }

    private fun _write(key: String, origin: Any?) {
        try {
            if (origin == null) {
                Paper.book().delete(key)
            } else {
                Paper.book().write(key, origin)
            }
        } catch (e: Exception) {

        }
    }
}