package com.abdownloadmanager.shared.downloaderinui.ytdlp

import com.abdownloadmanager.shared.downloaderinui.LinkChecker
import com.abdownloadmanager.shared.downloaderinui.DownloadSize
import ir.amirab.downloader.downloaditem.ytdlp.YtdlpDownloadCredentials
import ir.amirab.downloader.downloaditem.ytdlp.YtdlpResponseInfo
import ir.amirab.downloader.downloaditem.ytdlp.YtdlpProcessManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.io.BufferedReader
import java.io.InputStreamReader

class YtdlpLinkChecker(
    initialCredentials: YtdlpDownloadCredentials
) : LinkChecker<YtdlpDownloadCredentials, YtdlpResponseInfo, DownloadSize.Bytes>(
    initialCredentials
) {
    private val _suggestedName = MutableStateFlow<String?>(null)
    override val suggestedName: StateFlow<String?> = _suggestedName.asStateFlow()

    private val _downloadSize = MutableStateFlow<DownloadSize.Bytes?>(null)
    override val downloadSize: StateFlow<DownloadSize.Bytes?> = _downloadSize.asStateFlow()

    override fun infoUpdated(responseInfo: YtdlpResponseInfo?) {
        if (responseInfo != null) {
            val title = responseInfo.title ?: "video"
            // The ext we get back here is already the *final* extension we intend
            // to produce (mp4 for video, mp3 for audio-only) - see actualCheck().
            val targetExt = responseInfo.ext ?: "mp4"
            _suggestedName.value = "$title.$targetExt"
            _downloadSize.value = if (responseInfo.size > 0) DownloadSize.Bytes(responseInfo.size) else null
        } else {
            _suggestedName.value = null
            _downloadSize.value = null
        }
    }

    override suspend fun actualCheck(credentials: YtdlpDownloadCredentials): YtdlpResponseInfo {
        return withContext(Dispatchers.IO) {
            YtdlpProcessManager.ensureExecutableInstalled()
            val quality = credentials.quality
            val formatArgs = if (quality.audioOnly) listOf("-f", "bestaudio/best")
                            else listOf("-f", quality.toFormatSelector())

            val proc = ProcessBuilder(
                listOf(YtdlpProcessManager.getExePath(), "--dump-json", "--no-playlist") +
                    formatArgs + listOf(credentials.link)
            ).start()
            val reader = BufferedReader(InputStreamReader(proc.inputStream))
            val sb = java.lang.StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                sb.append(line)
            }
            val exitCode = proc.waitFor()
            if (exitCode != 0) {
                val errReader = BufferedReader(InputStreamReader(proc.errorStream))
                val errSb = java.lang.StringBuilder()
                while (errReader.readLine().also { line = it } != null) {
                    errSb.append(line).append("\n")
                }
                throw Exception("yt-dlp failed with exit code $exitCode: ${errSb.toString().trim()}")
            }
            val jsonString = sb.toString()
            val jsonObject = Json.parseToJsonElement(jsonString).jsonObject
            val title = jsonObject["title"]?.jsonPrimitive?.content

            val requestedDownloads = jsonObject["requested_downloads"]?.jsonArray
            val size = requestedDownloads
                ?.sumOf {
                    it.jsonObject["filesize"]?.jsonPrimitive?.longOrNull
                        ?: it.jsonObject["filesize_approx"]?.jsonPrimitive?.longOrNull ?: 0L
                }
                ?.takeIf { it > 0 }
                ?: (jsonObject["filesize"]?.jsonPrimitive?.longOrNull
                    ?: jsonObject["filesize_approx"]?.jsonPrimitive?.longOrNull ?: -1L)

            val ext = if (quality.audioOnly) "mp3" else "mp4"  // matches what the job will actually produce
            YtdlpResponseInfo(isSuccessFul = true, title = title, ext = ext, size = size)
        }
    }
}
