package io.github.takusan23.mediaprojectionrecodesample

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.*
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import java.io.File


class MyService : Service() {

    override fun onBind(p0: Intent?): IBinder? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    lateinit var mMediaRecorder: MediaRecorder
    lateinit var projectionManager: MediaProjectionManager
    lateinit var projection: MediaProjection

    val id = "rec_id"

    lateinit var data: Intent
    var code = 114

    var height = 1000
    var width = 1000
    var dpi = 1000

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        data = intent?.getParcelableExtra("data")!!
        code = intent.getIntExtra("code", 114)
        height = intent.getIntExtra("height", 114)
        width = intent.getIntExtra("width", 114)
        dpi = intent.getIntExtra("dpi", 114)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val notificationManager =
                application.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(id, "録画通知", NotificationManager.IMPORTANCE_DEFAULT)

            notificationManager.createNotificationChannel(channel)

            //新規→サービス　から作るとAndroidManifestのServiceに自動で登録してくれるんだけど
            //余計なパラメーター？が入ったせいで「startForeground」が動かなかったのでメモ

            val notification = NotificationCompat.Builder(applicationContext, id)
                .setContentText("録画です")
                .setContentTitle("録画")
                .setSmallIcon(R.drawable.ic_movie_creation_black_24dp)
                .build()

            startForeground(1, notification)

            //録画
            if (Build.VERSION.SDK_INT >= 29) startREC()
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        println("終了")
        try {
            mMediaRecorder.stop()
            mMediaRecorder.release()
        } catch (e: RuntimeException) {
            e.printStackTrace()
        }

    }


    @RequiresApi(29)
    fun startREC() {

        projectionManager =
            getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        //codeはActivity.RESULT_OKとかが入る。騙された。時間返せ！
        projection =
            projectionManager.getMediaProjection(code, data)

//        mMediaRecorder = MediaRecorder()
//        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
//        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE)
//        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
//        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
//        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
//        mMediaRecorder.setVideoEncodingBitRate(1024 * 1000)
//        mMediaRecorder.setVideoFrameRate(30)
//        mMediaRecorder.setVideoSize(1400, 2800)
//        mMediaRecorder.setAudioSamplingRate(32000)
//        mMediaRecorder.setOutputFile(getFilePath())
//        mMediaRecorder.prepare()
//
//        val virtualDisplay = projection.createVirtualDisplay(
//            "recode",
//            width,
//            height,
//            dpi,
//            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
//            mMediaRecorder.surface,
//            null,
//            null
//        )
//
//        //開始
//        mMediaRecorder.start()

        val audioPlaybackCaptureConfiguration =
            AudioPlaybackCaptureConfiguration.Builder(projection)
                .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                .addMatchingUsage(AudioAttributes.USAGE_GAME)
                .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
                .build()

        val sampleRate = 48000
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val pcm16 = AudioFormat.ENCODING_PCM_16BIT

        val pcmBufSize = AudioRecord.getMinBufferSize(
            sampleRate,
            channelConfig,
            pcm16
        ) * 5

        val fullAudioRecorder = AudioRecord.Builder()
                .setAudioPlaybackCaptureConfig(audioPlaybackCaptureConfiguration)
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(sampleRate)
                        .setEncoding(pcm16)
                        .setChannelMask(channelConfig)
                        .build()
                )
                .setBufferSizeInBytes(pcmBufSize)
                .build()

        fullAudioRecorder.startRecording()

        HandlerThread("ff2").let {
            it.start()
            Handler(it.looper).post {
                while (true) {
                    val buffer = ShortArray(pcmBufSize / 2)
                    val got = fullAudioRecorder.read(buffer, 0, buffer.size)
                    Log.i("b-BestRec", "${buffer[0]} ${buffer[1]}")
                }
            }
        }
    }

    fun getFilePath(): File {
        //ScopedStorageで作られるサンドボックスへのぱす
        val scopedStoragePath = getExternalFilesDir(null)
        //写真ファイル作成
        val file = File("${scopedStoragePath?.path}/test.mp4")
        return file
    }

}
