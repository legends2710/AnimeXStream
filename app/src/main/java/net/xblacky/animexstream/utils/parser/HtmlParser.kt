package net.xblacky.animexstream.utils.parser

import com.google.gson.Gson
import io.realm.RealmList
import net.xblacky.animexstream.ui.main.home.source.InvalidAnimeTypeException
import net.xblacky.animexstream.utils.constants.C
import net.xblacky.animexstream.utils.model.*
import net.xblacky.animexstream.utils.parser.DeCipher.decryptAES
import net.xblacky.animexstream.utils.parser.DeCipher.encryptAes
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.select.Elements
import timber.log.Timber
import java.lang.NullPointerException
import java.util.regex.Pattern
import kotlin.Exception

class HtmlParser {

    companion object {

        fun parseRecentSubOrDub(
            response: String,
            typeValue: Int,
        ): ArrayList<AnimeMetaModel> {
            if (response.isEmpty())
                throw ParserEmptyDataException()
            val animeMetaModelList: ArrayList<AnimeMetaModel> = ArrayList()
            val document = Jsoup.parse(response)
            val lists = document?.getElementsByClass("items")?.first()?.select("li")
            lists?.forEachIndexed { index, anime ->
                try {
                    val animeInfo = anime.getElementsByClass("name").first().select("a")
                    val title = animeInfo.attr("title")
                    val episodeUrl = animeInfo.attr("href")
                    val episodeNumber = anime.getElementsByClass("episode").first().text()
                    val animeImageInfo = anime.selectFirst("a")
                    val imageUrl = animeImageInfo.select("img").first().absUrl("src")

                    animeMetaModelList.add(
                        AnimeMetaModel(
                            ID = "$title$typeValue".hashCode(),
                            title = title,
                            episodeNumber = episodeNumber,
                            episodeUrl = episodeUrl,
                            categoryUrl = getCategoryUrl(imageUrl),
                            imageUrl = imageUrl,
                            typeValue = typeValue,
                            insertionOrder = index

                        )
                    )
                } catch (ignored: Exception) {
                }

            } ?: throw ParserListFetchException()
            if (animeMetaModelList.isEmpty()) {
                throw ParserListFetchException()
            } else {
                return animeMetaModelList
            }


        }

        fun parsePopular(response: String, typeValue: Int): ArrayList<AnimeMetaModel> {
            val animeMetaModelList: ArrayList<AnimeMetaModel> = ArrayList()
            if (response.isEmpty())
                throw ParserEmptyDataException()
            val document = Jsoup.parse(response)
            val lists =
                document?.getElementsByClass("added_series_body popular")?.first()?.select("ul")
                    ?.first()?.select("li")
            lists?.forEachIndexed { index, anime ->

                try {
                    val animeInfoFirst = anime.select("a").first()
                    val imageDiv =
                        animeInfoFirst.getElementsByClass("thumbnail-popular").first()
                            .attr("style")
                            .toString()
                    val imageUrl =
                        imageDiv.substring(
                            imageDiv.indexOf('\'') + 1,
                            imageDiv.lastIndexOf('\'')
                        )
                    val categoryUrl = animeInfoFirst.attr("href")
                    val animeTitle = animeInfoFirst.attr("title")
                    val animeInfoSecond = anime.select("p").last().select("a")
                    val episodeUrl = animeInfoSecond.attr("href")
                    val episodeNumber = animeInfoSecond.text()
                    val genreHtmlList = anime.getElementsByClass("genres").first().select("a")
                    val genreList = RealmList<GenreModel>()
                    genreList.addAll(getGenreList(genreHtmlList))


                    animeMetaModelList.add(
                        AnimeMetaModel(
                            ID = "$animeTitle$typeValue".hashCode(),
                            title = animeTitle,
                            episodeNumber = episodeNumber,
                            episodeUrl = episodeUrl,
                            categoryUrl = categoryUrl,
                            imageUrl = imageUrl,
                            typeValue = typeValue,
                            genreList = genreList,
                            insertionOrder = index
                        )
                    )

                } catch (ignored: Exception) {
                    //Skip the iteration of that object and move to next
                }


            } ?: throw ParserListFetchException()
            if (animeMetaModelList.isEmpty())
                throw ParserListFetchException()
            else
                return animeMetaModelList

        }

        fun parseMovie(response: String, typeValue: Int): ArrayList<AnimeMetaModel> {
            val animeMetaModelList: ArrayList<AnimeMetaModel> = ArrayList()

            if (response.isEmpty())
                throw ParserEmptyDataException()
            val document = Jsoup.parse(response)
            val lists = document?.getElementsByClass("items")?.first()?.select("li")
            lists?.forEachIndexed { index, animeMovie ->
                try {
                    val movieInfo = animeMovie.select("a").first()
                    val movieUrl = movieInfo.attr("href")
                    val movieName = movieInfo.attr("title")
                    val imageUrl = movieInfo.select("img").first().absUrl("src")
                    val releasedDate =
                        animeMovie.getElementsByClass("released")?.first()?.text()
                    animeMetaModelList.add(
                        AnimeMetaModel(
                            ID = "$movieName$typeValue".hashCode().hashCode(),
                            title = movieName,
                            imageUrl = imageUrl,
                            categoryUrl = movieUrl,
                            episodeUrl = null,
                            episodeNumber = null,
                            typeValue = typeValue,
                            insertionOrder = index,
                            releasedDate = releasedDate
                        )
                    )
                } catch (ignored: Exception) {
                    //Skip Iteration and move to next
                }

            } ?: throw ParserListFetchException()
            if (animeMetaModelList.isEmpty())
                throw ParserListFetchException()
            else
                return animeMetaModelList

        }

        fun parseAnimeInfo(response: String): AnimeInfoModel {
            val document = Jsoup.parse(response)
            val animeInfo = document.getElementsByClass("anime_info_body_bg")
            val animeUrl = animeInfo.select("img").first().absUrl("src")
            val animeTitle = animeInfo.select("h1").first().text()
            val lists = document?.getElementsByClass("type")
            lateinit var type: String
            lateinit var releaseTime: String
            lateinit var status: String
            lateinit var plotSummary: String
            val genre: RealmList<GenreModel> = RealmList()
            lists?.forEachIndexed { index, element ->
                when (index) {
                    0 -> type = element.text()
                    1 -> plotSummary = element.text()
                    2 -> genre.addAll(getGenreList(element.select("a")))
                    3 -> releaseTime = element.text()
                    4 -> status = element.text()
                }
            }
            val episodeInfo = document.getElementById("episode_page")
            val episodeList = episodeInfo.select("a").last()
            val endEpisode = episodeList.attr("ep_end")
            val alias = document.getElementById("alias_anime").attr("value")
            val id = document.getElementById("movie_id").attr("value")
            return AnimeInfoModel(
                id = id,
                animeTitle = animeTitle,
                imageUrl = animeUrl,
                type = formatInfoValues(type),
                releasedTime = formatInfoValues(releaseTime),
                status = formatInfoValues(status),
                genre = genre,
                plotSummary = formatInfoValues(plotSummary).trim(),
                alias = alias,
            )

        }

        fun parseMediaUrl(response: String): EpisodeInfo {
            var mediaUrl: String?
            val document = Jsoup.parse(response)
            val info = document?.getElementsByClass("anime")?.first()?.select("a")
            mediaUrl = info?.attr("data-video").toString()
            val nextEpisodeUrl =
                document.getElementsByClass("anime_video_body_episodes_r")?.select("a")?.first()
                    ?.attr("href")
            val previousEpisodeUrl =
                document.getElementsByClass("anime_video_body_episodes_l")?.select("a")?.first()
                    ?.attr("href")


            return EpisodeInfo(
                nextEpisodeUrl = nextEpisodeUrl,
                previousEpisodeUrl = previousEpisodeUrl,
                vidCdnUrl = mediaUrl
            )
        }

        fun parseEncryptAjaxParameters(response: String,url: String): String {
            return try {
                val id =
                    Regex("id=([^&]+)").find(url)!!.value.removePrefix(
                        "id="
                    )
                val document=Jsoup.parse(response)
                val value2 = document.select("script[data-name=\"episode\"]").attr("data-value")
                val decrypt = decryptAES(value2, C.GogoSecretkey, C.GogoSecretIV).replace("\t","").substringAfter(id)
                val encrypted = encryptAes(id, C.GogoSecretkey, C.GogoSecretIV)
                "id=$encrypted$decrypt&alias=$id"
            }catch (e:Exception){
                e.toString()
            }
        }

        fun parseEncryptedData(response: String): M3U8FromAjaxModel {
            val data = JSONObject(response).getString("data")
            val decryptedData = decryptAES(data, C.GogoSecondKey, C.GogoSecretIV).replace(
                """o"<P{#meme":""",
                """e":[{"file":"""
            )
            return Gson().fromJson(decryptedData, M3U8FromAjaxModel::class.java)
        }


/*
        fun parseEncryptAjaxParameters(response: String): String {

            val document = Jsoup.parse(response)
            //Start with parsing Some Values https://github.com/pystardust/ani-cli/issues/217#issuecomment-1000996059

            // Value6: $('script[data-name=\x27ts\x27]').data('value')
            val value6 =
                document.getElementsByAttributeValue("data-name", "ts").attr("data-value")

            //Value5: $("[name='crypto']").attr('content')
            val value5 = document.getElementsByAttributeValue("name", "crypto").attr("content")

            val value1 = decryptAES(
                encryptedText = document.getElementsByAttributeValue("data-name", "crypto")
                    .attr("data-value"),
                key = decode(value6 + value6),
                iv = decode(value6)
            )
            val value4 = decryptAES(
                encryptedText = value5,
                key = decode(value1),
                iv = decode(value6)
            )
            val value3 = decode(value4)

            //Value2: Random numbers (16 chars)
            val value2 = RandomStringUtils.randomAlphanumeric(16)

            val encrypted = encryptAes(
                text = value4.removeRange(value4.indexOf("&"), value4.length),
                key = decode(value1),
                iv = decode(value2)
            )


            return "id=" + encrypted + "&time=" + "00" + value2 + "00" + value3.substring(
                value3.indexOf("&")
            )

        }


 */


        fun parseM3U8Url(response: String): String? {
            var m3u8Url: String? = ""
            val document = Jsoup.parse(response)
            val info = document?.getElementsByClass("videocontent")
            val pattern = Pattern.compile(C.M3U8_REGEX_PATTERN)
            val matcher = pattern.matcher(info.toString())
            return try {
                while (matcher.find()) {
                    Timber.e(matcher.group((0)))
                    if (matcher.group(0)!!.contains("m3u8") || matcher.group(0)!!
                            .contains("googlevideo")
                    ) {
                        m3u8Url = matcher.group(0)
                        break
                    }
                }
                m3u8Url
            } catch (npe: NullPointerException) {
                m3u8Url
            }

        }

        fun fetchEpisodeList(response: String): ArrayList<EpisodeModel> {
            val episodeList = ArrayList<EpisodeModel>()
            val document = Jsoup.parse(response)
            val lists = document?.select("li")
            lists?.forEach {
                val episodeUrl = it.select("a").first().attr("href").trim()
                val episodeNumber = it.getElementsByClass("name").first().text()
                val episodeType = it.getElementsByClass("cate").first().text()
                episodeList.add(
                    EpisodeModel(
                        episodeNumber = episodeNumber,
                        episodeType = episodeType,
                        episodeurl = episodeUrl
                    )
                )
            }
            return episodeList
        }

        private fun filterGenreName(genreName: String): String {
            return if (genreName.contains(',')) {
                genreName.substring(genreName.indexOf(',') + 1)
            } else {
                genreName
            }
        }

        private fun getGenreList(genreHtmlList: Elements): ArrayList<GenreModel> {
            val genreList = ArrayList<GenreModel>()
            genreHtmlList.forEach {
                val genreUrl = it.attr("href")
                val genreName = it.text()

                genreList.add(
                    GenreModel(
                        genreUrl = genreUrl,
                        genreName = filterGenreName(genreName)
                    )
                )

            }

            return genreList
        }

        fun parseDataBasedOnType(
            response: String,
            type: Int
        ): ArrayList<AnimeMetaModel> {
            return when (type) {
                C.TYPE_RECENT_SUB, C.TYPE_RECENT_DUB -> parseRecentSubOrDub(
                    response,
                    type
                )
                C.TYPE_MOVIE, C.TYPE_NEW_SEASON -> parseMovie(response, type)
                C.TYPE_POPULAR_ANIME -> parsePopular(response, type)
                else -> throw InvalidAnimeTypeException()
            }

        }

        private fun formatInfoValues(infoValue: String): String {
            return infoValue.substring(infoValue.indexOf(':') + 1, infoValue.length)
        }

        private fun getCategoryUrl(url: String): String {
            return try {
                var categoryUrl = url.substring(url.lastIndexOf('/') + 1, url.lastIndexOf('.'))
                categoryUrl = "/category/$categoryUrl"
                categoryUrl
            } catch (exception: StringIndexOutOfBoundsException) {
                Timber.e("Image URL: $url")
                ""

            }

        }

    }


    class ParserListFetchException(message: String = "Unable to parse Anime List") :
        Exception(message)

    class ParserEmptyDataException(message: String = "Unable to fetch from empty data") :
        Exception(message)

}