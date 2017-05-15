package cn.nekocode.murmur.data.repo

import cn.nekocode.murmur.data.DO.Murmur
import cn.nekocode.murmur.data.DataLayer
import cn.nekocode.murmur.data.service.Api.Leancloud
import cn.nekocode.murmur.data.util.CacheUtil
import rx.Observable
import rx.schedulers.Schedulers

/**
 * @author nekocode (nekocode.cn@gmail.com)
 */
object MurmurRepo {

    fun getMurmurs(): Observable<List<Murmur>> =
            Leancloud.API.getMurmurs(50, "-updatedAt")
                    .subscribeOn(Schedulers.io())
                    .map {
                        val murmurs = it.results
                        murmurs.forEach {
                            it.file.url = DataLayer.MEDIA_PROXY.getProxyUrl(it.file.url)
                        }

                        CacheUtil.write("murmurs", murmurs)
                        murmurs
                    }
                    .onErrorResumeNext { err ->
                        CacheUtil.rxRead<List<Murmur>>("murmurs")
                                .map {
                                    it ?: throw err
                                }
                    }

}