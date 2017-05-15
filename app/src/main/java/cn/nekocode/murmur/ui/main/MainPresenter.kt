package cn.nekocode.murmur.ui.main

import android.os.Bundle
import android.os.Parcelable
import cn.nekocode.kotgo.component.rx.RxBus
import cn.nekocode.kotgo.component.ui.KtPresenter
import cn.nekocode.murmur.data.DO.Murmur
import cn.nekocode.murmur.data.DO.MurmurParcel
import cn.nekocode.murmur.data.DO.douban.DoubanSong
import cn.nekocode.murmur.data.DO.douban.Session
import cn.nekocode.murmur.data.DO.douban.SessionParcel
import cn.nekocode.murmur.data.DO.douban.SongParcel
import cn.nekocode.murmur.data.exception.DoubanException
import cn.nekocode.murmur.data.repo.DoubanRepo
import cn.nekocode.murmur.data.repo.MurmurRepo
import cn.nekocode.murmur.data.repo.SettingRepo
import cn.nekocode.murmur.service.MusicService
import cn.nekocode.murmur.util.Util.randomPick
import rx.Observable
import java.util.*

/**
 * @author nekocode (nekocode.cn@gmail.com)
 */
class MainPresenter() : KtPresenter<Contract.View>(), Contract.Presenter {
    companion object {
        private const val KEY_SAVED_SESSION = "session"
        private const val KEY_SAVED_MURMURS = "murmurs"
        private const val KEY_SAVED_MURMURS_PLAYING = "murmurs_playing"
        private const val KEY_SAVED_SONG_PLAYING = "song_playing"
    }

    var view: Contract.View? = null

    var session: Session? = null
    val murmurs = ArrayList<Murmur>()
    val playingMurmurs = ArrayList<Murmur>()
    var playingSong: DoubanSong.Song? = null
    var timingTextTask: TimingTextTask? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 恢复现场
        savedInstanceState?.let {
            session = it.getParcelable<SessionParcel>(KEY_SAVED_SESSION).data
            murmurs.addAll(it.getParcelableArrayList<MurmurParcel>(KEY_SAVED_MURMURS).map { it.data })
            playingMurmurs.addAll(it.getParcelableArrayList<MurmurParcel>(KEY_SAVED_MURMURS_PLAYING).map { it.data })
            playingSong = it.getParcelable<SongParcel>(KEY_SAVED_SONG_PLAYING).data
        }

        // 订阅播放结束事件
        // TODO: 16/4/11 控制播放循环
        RxBus.subscribe(String::class.java) {
            if (it.equals("Finished")) {

            }
        }
    }

    override fun onViewCreated(view: Contract.View?, savedInstanceState: Bundle?) {
        this.view = view

        if (savedInstanceState != null) {
            // 恢复现场
            view?.onMurmursChanged(murmurs, playingMurmurs)
            view?.onSongChanged(playingSong!!)

        } else {
            // 检查是否已经登录
            val cachedUser = DoubanRepo.getCachedUserInfo()
            if (cachedUser == null) {
                view?.showLoginDialog()
            } else {
                login(cachedUser.first, cachedUser.second)
            }
        }

        // 异步获取歌曲剩余时间
        timingTextTask = TimingTextTask(view).apply { execute() }
    }

    /**
     * 保存现场
     */
    override fun onSaveInstanceState(outState: Bundle?) {
        outState?.putParcelable("session", SessionParcel(session))
        outState?.putParcelableArrayList(
                "murmurs",
                murmurs.map { MurmurParcel(it) } as ArrayList<out Parcelable>
        )
        outState?.putParcelableArrayList(
                "playingMurmurs",
                playingMurmurs.map { MurmurParcel(it) } as ArrayList<out Parcelable>
        )
        outState?.putParcelable("playingSong", SongParcel(playingSong))

        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        // 终止获取歌曲剩余时间的任务
        timingTextTask?.cancel(true)
        super.onDestroyView()
    }

    /**
     * 停止所有歌曲和白噪音
     */
    override fun stopAll() {
        MusicService.instance?.pauseSong()
        MusicService.instance?.stopAllMurmurs()
    }

    /**
     * 登陆豆瓣
     */
    override fun login(email: String, pwd: String) {
        DoubanRepo.login(email, pwd).onUI().bindLifecycle(this).subscribe({
            session = it
            view?.onLoginSuccess()
            fetchData()

        }, {
            when (it) {
                is DoubanException -> view?.showToast(it.err)
                else -> {
                    if (it.message != null)
                        view?.showToast(it.message!!)
                }
            }

            view?.onLoginFailed()
        })
    }

    /**
     * 获取歌曲和白噪音
     */
    private fun fetchData() {
        Observable.combineLatest(MurmurRepo.getMurmurs(), DoubanRepo.nextSong(session!!), {
            murmurs, song ->
            Pair(murmurs, song)

        }).onUI().bindLifecycle(this).subscribe({
            murmurs.addAll(it.first)

            val selectedMurmurs = SettingRepo.loadSelectedMurmursIDs()
            if (selectedMurmurs == null) {
                // 随机选择两个白噪音
                playingMurmurs.addAll(murmurs.randomPick(2))
                SettingRepo.saveSelectedMurmursIDs(playingMurmurs.map { it.id })

            } else {
                // 读取之前保存的白噪音设置
                playingMurmurs.addAll(murmurs.filter { selectedMurmurs.contains(it.id) })
            }

            MusicService.instance?.playMurmurs(playingMurmurs)
            view?.onMurmursChanged(murmurs, playingMurmurs)

            playingSong = it.second
            MusicService.instance?.playSong(playingSong!!)
            view?.onSongChanged(playingSong!!)

        }, errorHandler)
    }

    /**
     * 切换白噪音
     */
    override fun changeMurmur(murmur: Murmur, play: Boolean) {
        if (murmur in playingMurmurs) {
            if (!play) {
                playingMurmurs.remove(murmur)
            }
        } else {
            if (play) {
                playingMurmurs.add(murmur)
            }
        }

        // 保存白噪音设置
        SettingRepo.saveSelectedMurmursIDs(playingMurmurs.map { it.id })

        // 播放白噪音
        MusicService.instance?.playMurmurs(playingMurmurs)
        view?.onMurmursChanged(murmurs, playingMurmurs)
    }

    /**
     * 切换歌曲
     */
    override fun nextSong() {
        DoubanRepo.nextSong(session!!).onUI().bindLifecycle(this).subscribe({
            playingSong = it
            MusicService.instance?.playSong(playingSong!!)
            view?.onSongChanged(playingSong!!)
        }, errorHandler)
    }

    /**
     * 通用错误处理
     */
    val errorHandler: (Throwable) -> Unit = {
        when (it) {
            is DoubanException -> {
                if (it.err.equals("invalid_token")) {
                    view?.showToast("You token has been invalid.\nYou must login again.")
                    view?.showLoginDialog()

                } else {
                    view?.showToast(it.err)
                }
            }

            else -> {
                if (it.message != null)
                    view?.showToast(it.message!!)
            }
        }

    }
}