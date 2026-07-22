package com.abdownloadmanager.shared.downloaderinui.ytdlp.edit

import com.abdownloadmanager.shared.downloaderinui.DownloadSize
import com.abdownloadmanager.shared.downloaderinui.edit.EditDownloadInputs
import com.abdownloadmanager.shared.downloaderinui.edit.DownloadConflictDetector
import com.abdownloadmanager.shared.downloaderinui.ytdlp.YtdlpLinkChecker
import com.abdownloadmanager.shared.downloaderinui.ytdlp.YtdlpCredentialsToItemMapper
import com.abdownloadmanager.shared.downloaderinui.ytdlp.describeAsStringSource
import com.abdownloadmanager.shared.ui.configurable.Configurable
import com.abdownloadmanager.shared.ui.configurable.item.EnumConfigurable
import com.abdownloadmanager.shared.util.SizeAndSpeedUnitProvider
import ir.amirab.downloader.downloaditem.DownloadJobExtraConfig
import ir.amirab.downloader.downloaditem.ytdlp.YtdlpDownloadItem
import ir.amirab.downloader.downloaditem.ytdlp.YtdlpDownloadCredentials
import ir.amirab.downloader.downloaditem.ytdlp.YtdlpQuality
import ir.amirab.downloader.downloaditem.ytdlp.YtdlpResponseInfo
import ir.amirab.util.compose.StringSource
import ir.amirab.util.compose.asStringSource
import ir.amirab.util.flow.mapTwoWayStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class YtdlpEditDownloadInputs(
    currentDownloadItem: MutableStateFlow<YtdlpDownloadItem>,
    editedDownloadItem: MutableStateFlow<YtdlpDownloadItem>,
    private val sizeAndSpeedUnitProvider: SizeAndSpeedUnitProvider,
    mapper: YtdlpCredentialsToItemMapper,
    conflictDetector: DownloadConflictDetector,
    scope: CoroutineScope,
    linkCheckerFactory: com.abdownloadmanager.shared.downloaderinui.LinkCheckerFactory<YtdlpDownloadCredentials, YtdlpResponseInfo, DownloadSize.Bytes, YtdlpLinkChecker>,
    editDownloadCheckerFactory: com.abdownloadmanager.shared.downloaderinui.edit.EditDownloadCheckerFactory<YtdlpDownloadItem, YtdlpDownloadCredentials, YtdlpResponseInfo, DownloadSize.Bytes, YtdlpLinkChecker>,
) : EditDownloadInputs<
        YtdlpDownloadItem,
        YtdlpDownloadCredentials,
        YtdlpResponseInfo,
        DownloadSize.Bytes,
        YtdlpLinkChecker,
        YtdlpCredentialsToItemMapper
        >(
    currentDownloadItem = currentDownloadItem,
    editedDownloadItem = editedDownloadItem,
    mapper = mapper,
    scope = scope,
    conflictDetector = conflictDetector,
    linkCheckerFactory = linkCheckerFactory,
    editDownloadCheckerFactory = editDownloadCheckerFactory,
) {
    val quality: MutableStateFlow<YtdlpQuality> = credentials.mapTwoWayStateFlow(
        map = { it.quality },
        unMap = { copy(quality = it) },
    )

    fun setQuality(quality: YtdlpQuality) {
        this.quality.value = quality
    }

    override val downloadJobConfig: StateFlow<DownloadJobExtraConfig?> = MutableStateFlow(null)

    override val configurableList: List<Configurable<*>> = listOf(
        EnumConfigurable(
            title = "Quality".asStringSource(),
            description = "Choose the video quality, or download audio only as MP3".asStringSource(),
            backedBy = quality,
            possibleValues = YtdlpQuality.entries,
            describe = { it.describeAsStringSource() },
        )
    )

    override fun applyEditedItemTo(item: YtdlpDownloadItem) {
        val edited = editedDownloadItem.value
        item.folder = edited.folder
        item.name = edited.name
        item.link = edited.link
        item.contentLength = edited.contentLength
        item.downloadPage = edited.downloadPage
        item.quality = edited.quality
    }

    override fun downloadSizeToStringSource(downloadSize: DownloadSize.Bytes): StringSource {
        return downloadSize.asStringSource(sizeAndSpeedUnitProvider.sizeUnit.value)
    }
}
