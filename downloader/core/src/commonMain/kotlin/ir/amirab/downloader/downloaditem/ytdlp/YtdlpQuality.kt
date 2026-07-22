package ir.amirab.downloader.downloaditem.ytdlp

import kotlinx.serialization.Serializable

/**
 * A quality preset for yt-dlp downloads.
 *
 * [heightCapPx] caps the requested vertical resolution; yt-dlp will automatically
 * pick the best available format at or below this height for the given video
 * (falling back gracefully if the exact resolution doesn't exist), so we don't
 * need to know a video's exact available formats ahead of time to offer a
 * fixed list of choices in the UI.
 *
 * [audioOnly] requests audio-only extraction re-encoded to MP3 instead of a
 * video download.
 */
@Serializable
enum class YtdlpQuality(
    val heightCapPx: Int? = null,
    val audioOnly: Boolean = false,
) {
    Best(heightCapPx = null, audioOnly = false),
    P2160(heightCapPx = 2160),
    P1440(heightCapPx = 1440),
    P1080(heightCapPx = 1080),
    P720(heightCapPx = 720),
    P480(heightCapPx = 480),
    P360(heightCapPx = 360),
    AudioMp3(audioOnly = true);

    /**
     * Builds the `-f` format-selector string yt-dlp should use for this quality.
     * Not used for [AudioMp3] downloads, which use `-x --audio-format mp3` instead.
     */
    fun toFormatSelector(): String {
        val cap = heightCapPx
        return if (cap == null) {
            "bestvideo+bestaudio/best"
        } else {
            "bestvideo[height<=$cap]+bestaudio/best[height<=$cap]"
        }
    }

    companion object {
        val Default = Best
    }
}
