package cn.nekocode.murmur.data.repo

import cn.nekocode.murmur.data.DO.douban.Session
import cn.nekocode.murmur.data.DO.douban.DoubanSong
import cn.nekocode.murmur.data.service.Api.DoubanFM
import cn.nekocode.murmur.data.service.Api.DoubanToken
import cn.nekocode.murmur.data.util.CacheUtil
import rx.Observable
import rx.schedulers.Schedulers
import java.util.*

/**
 * @author nekocode (nekocode.cn@gmail.com)
 */
object DoubanRepo {

    fun getCachedUserInfo(): Pair<String, String>? {
        return CacheUtil.read<Pair<String, String>>("user")
    }

    fun login(email: String, pwd: String): Observable<Session> =
            DoubanToken.API.login(email, pwd)
                    .subscribeOn(Schedulers.io())
                    .doOnNext {
                        if (!it.accessToken.isNullOrBlank()) {
                            CacheUtil.write("user", Pair(email, pwd))
                        }
                    }

    fun getSongs(session: Session) {
        val auth = "Bearer ${session.accessToken}"

        // 从网络获取红心歌曲
        DoubanFM.API.getRedHeartSongIds(auth)
                .map {
                    val builder = StringBuilder()
                    it.songs
                            .filter(DoubanSong.Id::playable) // 筛选出可播放的歌曲
                            .forEach {
                                builder.append(it.sid).append("|")
                            }

                    builder.deleteCharAt(builder.length - 1).toString()

                }
                .flatMap {
                    // 根据歌曲 id 列表获取详细详细
                    DoubanFM.API.getSongs(auth, it)
                            .doOnNext {
                                // 洗牌算法
                                Collections.shuffle(it)
                            }
                }
    }

}