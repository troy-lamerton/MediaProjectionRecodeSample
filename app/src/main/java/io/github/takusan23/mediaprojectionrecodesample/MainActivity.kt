package io.github.takusan23.mediaprojectionrecodesample

import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File


class MainActivity : AppCompatActivity() {

    val code = 114
    lateinit var projectionManager: MediaProjectionManager
    lateinit var myService: MyService

    @TargetApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        myService = MyService()

        //権限取得
        projectionManager =
            getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        rec_button.setOnClickListener {

            startActivityForResult(projectionManager.createScreenCaptureIntent(), code)

        }

        //停止機構
        stop_button.setOnClickListener {

            val intent = Intent(this, myService::class.java)
            stopService(intent)

        }

        webView.settings.javaScriptEnabled = true
        webView.webChromeClient = WebChromeClient()
        webView.webViewClient = WebViewClient()
        webView.settings.javaScriptCanOpenWindowsAutomatically = true
//        webView.settings.pluginState = WebSettings.PluginState.ON
        webView.settings.mediaPlaybackRequiresUserGesture = false

        val url = "https://www.youtube.com/watch?v=lcfcG31AgyE"
        webView.loadUrl(url)

    }

    @TargetApi(Build.VERSION_CODES.O)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == code) {
            if (data != null) {

                val intent = Intent(this, myService::class.java)
                intent.putExtra("code", resultCode)
                intent.putExtra("data", data)

                val metrics = resources.displayMetrics

                intent.putExtra("height", metrics.heightPixels)
                intent.putExtra("width", metrics.widthPixels)
                intent.putExtra("dpi", metrics.densityDpi)

                startForegroundService(intent)
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
