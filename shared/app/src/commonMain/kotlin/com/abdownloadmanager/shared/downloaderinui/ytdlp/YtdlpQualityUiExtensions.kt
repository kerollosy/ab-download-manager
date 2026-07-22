package com.abdownloadmanager.shared.downloaderinui.ytdlp

import ir.amirab.downloader.downloaditem.ytdlp.YtdlpQuality
import ir.amirab.util.compose.StringSource
import ir.amirab.util.compose.asStringSource

fun YtdlpQuality.describeAsStringSource(): StringSource = when (this) {
    YtdlpQuality.Best -> "Best available".asStringSource()
    YtdlpQuality.P2160 -> "2160p (4K)".asStringSource()
    YtdlpQuality.P1440 -> "1440p (2K)".asStringSource()
    YtdlpQuality.P1080 -> "1080p (Full HD)".asStringSource()
    YtdlpQuality.P720 -> "720p (HD)".asStringSource()
    YtdlpQuality.P480 -> "480p".asStringSource()
    YtdlpQuality.P360 -> "360p".asStringSource()
    YtdlpQuality.AudioMp3 -> "Audio only (MP3)".asStringSource()
}
