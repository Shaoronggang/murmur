package cn.nekocode.murmur.service

import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.media.audiofx.Visualizer
import android.os.Binder
import cn.nekocode.kotgo.component.rx.RxBus
import cn.nekocode.murmur.data.DO.douban.SongS
import cn.nekocode.murmur.data.DO.Murmur

/**
 * Created by nekocode on 3/15/16.
 */
class MusicService: Service() {
    companion object {
        var instance: MusicService? = null
    }

    inner class MusicServiceBinder: Binder() {
        val service = this@MusicService
    }
    override fun onBind(intent: Intent?) = MusicServiceBinder()

    class SongPlayer(var song: SongS.Song?, var player: MediaPlayer)
    private val songPlayer = SongPlayer(null, MediaPlayer())
    private val murmurPlayers = hashMapOf<Murmur, MediaPlayer>()
    private var stopSong = false
    private var stopMurmurs = false
    var fftAverage: Float = 0.6f
    var fft: ByteArray? = null
    var oldVisualizer: Visualizer? = null

    fun playSong(song: SongS.Song) {
        stopSong = false

        if(song != songPlayer.song) {
            songPlayer.song = song

            val player = songPlayer.player
            player.reset()
            player.isLooping = true
            player.setDataSource(song.url)
            player.prepareAsync()
            player.setOnPreparedListener {
                if(!stopSong) {
                    it.start()
                    RxBus.send("Prepared")

                    fftAverage = 0.6f
                    oldVisualizer?.release()
                    // 捕获音频信息
                    oldVisualizer = Visualizer(player.audioSessionId).apply {
                        captureSize = Visualizer.getCaptureSizeRange()[1]

                        setDataCaptureListener(object: Visualizer.OnDataCaptureListener {
                            override fun onFftDataCapture(visualizer: Visualizer,
                                                          fft: ByteArray, samplingRate: Int) {
                                // TODO
                                this@MusicService.fft = fft

                            }

                            override fun onWaveFormDataCapture(visualizer: Visualizer,
                                                               waveform: ByteArray, samplingRate: Int) {
                            }
                        }, Visualizer.getMaxCaptureRate() / 2, false, true)

                        enabled = true
                    }
                }
            }

            // 音乐播放完后
            player.setOnCompletionListener {
                RxBus.send("Finished")
            }

        } else {
            if(!songPlayer.player.isPlaying) {
                songPlayer.player.start()
            }
        }
    }

    fun pauseSong() {
        stopSong = true
        songPlayer.player.pause()
    }

    fun playMurmurs(murmurs: List<Murmur>) {
        // 停止被去掉的白噪音
        murmurPlayers.filter {
            !murmurs.contains(it.key)

        }.forEach {
            it.value.stop()
        }

        // 如果所有白噪音都被停止的话,重新播放以前的
        murmurPlayers.filter {
            murmurs.contains(it.key) && !it.value.isPlaying

        }.forEach {
            val player = it.value

            player.reset()
            player.isLooping = true
            player.setDataSource(it.key.file.url)
            player.prepareAsync()
            player.setOnPreparedListener {
                if(!stopMurmurs)
                    it.start()
            }
        }

        // 开始播放新选中的白噪音
        stopMurmurs = false
        murmurs.filter {
            !murmurPlayers.containsKey(it)

        }.forEach {
            val player = murmurPlayers[it] ?: MediaPlayer()
            murmurPlayers[it] = player

            player.reset()
            player.isLooping = true
            player.setDataSource(it.file.url)
            player.prepareAsync()
            player.setOnPreparedListener {
                if(!stopMurmurs)
                    it.start()
            }
        }
    }

    fun stopAllMurmurs() {
        stopMurmurs = true
        murmurPlayers.forEach {
            it.value.stop()
        }
    }

    fun isSongPlaying(): Boolean = songPlayer.song != null && songPlayer.player.isPlaying

    fun getRestTime(): Int = (songPlayer.player.duration - songPlayer.player.currentPosition) / 1000

    override fun onDestroy() {
        super.onDestroy()
        songPlayer.player.release()

        murmurPlayers.forEach {
            it.value.release()
        }
        murmurPlayers.clear()
    }
}