package cn.nekocode.murmur.ui.main

import android.os.AsyncTask
import android.util.Log
import cn.nekocode.murmur.service.MusicService
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread

/**
 * Created by nekocode on 16/9/16.
 */
class TimingTask : AsyncTask<Unit, Any, Unit> {
    var view: Contract.View? = null
    var oldSpeed: Float = 0.6f

    constructor(view: Contract.View?) : super() {
        this.view = view
    }

    override fun onCancelled() {
        view = null
        super.onCancelled()
    }

    override fun doInBackground(vararg params: Unit): Unit {
        while(view != null) {
            val time = MusicService.instance?.let {

                if(!it.isSongPlaying())
                    return@let null

                val rest = it.getRestTime()
                val m = rest / 60
                val s = rest % 60

                if(m != 0 && s != 0) "$m:$s" else null
            }

            val speed = MusicService.instance?.fft?.let {
                val spectrum = 10

                val model = ByteArray(it.size / 2 + 1)
                model[0] = Math.abs(it[0].toInt()).toByte()

                var i = 2
                var j = 1
                while (j < spectrum) {
                    model[j] = Math.hypot(it[i].toDouble(), it[i + 1].toDouble()).toByte()

                    i += 2
                    j ++
                }

                j = 0
                var average = 0f
                while (j < spectrum) {
                    average += model[j]

                    j ++
                }

                average /= spectrum
                0.6f + (average / 40f)

            } ?: 0.6f

            if (speed > oldSpeed) {
                oldSpeed += 0.1f
            } else if (speed < oldSpeed) {
                oldSpeed -= 0.1f
            }

            Log.d("SPEED", oldSpeed.toString())
            publishProgress(time, oldSpeed)

            Thread.sleep(500)
        }
    }

    override fun onProgressUpdate(vararg values: Any?) {
        val time = values[0]
        val speed = values[1]

        if (time != null) {
            view?.onTimeChanged(time as String)
        }

        if (speed != null) {
            view?.onBeatSpeedChanged(speed as Float)
        }
    }

}
