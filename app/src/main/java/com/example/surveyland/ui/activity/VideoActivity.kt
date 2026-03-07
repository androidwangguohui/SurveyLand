package com.example.surveyland.ui.activity

import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.buddha.activity.BaseActivity
import com.example.surveyland.databinding.ActivityVideoBinding
import com.example.surveyland.ui.view.AppToast

class VideoActivity: BaseActivity() {

    private lateinit var mActivityVideoBinding: ActivityVideoBinding
    private lateinit var player: ExoPlayer

    @OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mActivityVideoBinding = ActivityVideoBinding.inflate(layoutInflater)
        setContentView(mActivityVideoBinding.root)
        var videoUrl = "https://www.example.com/video.mp4"
        videoUrl = intent.getStringExtra("videoUrl").toString()
        player = ExoPlayer.Builder(this).build()
        mActivityVideoBinding.playerView.player = player
        // 设置控制栏自动隐藏的延迟时间，单位毫秒
        mActivityVideoBinding.playerView.controllerShowTimeoutMs = 1000
        val mediaItem = MediaItem.fromUri(videoUrl)

        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()

//        val duration = player.duration//播放时长
//        player.pause()//暂停

        player.addListener(object : Player.Listener {

            override fun onPlayerError(error: PlaybackException) {

                when (error.errorCode) {

                    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED -> {
                        // 网络连接失败
                        error("请打开网络连接")
                    }

                    PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS -> {
                        // HTTP错误
                        error()
                    }

                    PlaybackException.ERROR_CODE_DECODER_INIT_FAILED -> {
                        // 解码器初始化失败
                        error()
                    }

                    PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND -> {
                        // 视频不存在
                        error()
                    }

                    else -> {
                        // 其他错误
                        error()
                    }
                }
            }
        })

    }
    private fun error(msg:String = "播放失败，请稍后重试") {
        AppToast.show(this@VideoActivity, msg)
        finish()
    }
    override fun onDestroy() {
        super.onDestroy()
        player.release()
    }

}