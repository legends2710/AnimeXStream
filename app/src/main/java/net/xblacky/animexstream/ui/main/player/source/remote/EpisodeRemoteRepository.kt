package net.xblacky.animexstream.ui.main.player.source.remote

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import net.xblacky.animexstream.utils.Result
import net.xblacky.animexstream.utils.Utils
import net.xblacky.animexstream.utils.di.DispatcherModule
import net.xblacky.animexstream.utils.model.EpisodeInfo
import net.xblacky.animexstream.utils.model.M3U8FromAjaxModel
import net.xblacky.animexstream.utils.parser.HtmlParser
import net.xblacky.animexstream.utils.rertofit.NetworkInterface
import okhttp3.ResponseBody
import javax.inject.Inject

@Suppress("BlockingMethodInNonBlockingContext")
class EpisodeRemoteRepository @Inject constructor(
    private val service: NetworkInterface.EpisodeDataService,
    @DispatcherModule.IoDispatcher val dispatcher: CoroutineDispatcher
) {

    suspend fun fetchEpisodeData(url: String): EpisodeInfo {
        return withContext(dispatcher) {
            val response = service.fetchEpisodeMediaUrl(Utils.getHeader(), url)
            HtmlParser.parseMediaUrl(response = response.string())
        }
    }

    //Return Params for Ajax Url
    suspend fun fetchAjaxUrl(url: String): String {
        return withContext(dispatcher) {
            val response = service.fetchAjaxUrlForM3U8(Utils.getHeader(), url)
            HtmlParser.parseEncryptAjaxParameters(response = response.string(),url)

        }
    }

    suspend fun fetchM3U8Url(url: String): ResponseBody {
        return withContext(dispatcher) {
            service.fetchM3U8Url(url)
        }
    }
}