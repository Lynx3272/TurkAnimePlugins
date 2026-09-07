package com.example

import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.newAnimeSearchResponse
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import com.lagradost.cloudstream3.utils.AppUtils.toJson
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType

data class AniListResponse(
    val data: AniListData
)

data class AniListData(
    @JsonProperty("Page")
    val page: AniListPage
)

data class AniListPage(
    val media: List<AniListMedia>
)

data class AniListMedia(
    val id: Int,
    val title: AniListTitle,
    val coverImage: AniListCover?
)

data class AniListTitle(
    val romaji: String?,
    val english: String?,
    val native: String?
)

data class AniListCover(
    val large: String?
)

class TurkAnimeProvider : MainAPI() {

    override var mainUrl = "https://graphql.anilist.co"
    override var name = "Turk Anime"

    override val supportedTypes = setOf(
        TvType.Anime,
        TvType.AnimeMovie
    )

    override var lang = "tr"
    override val hasMainPage = false

    override suspend fun search(query: String): List<SearchResponse> {

        if (query.isBlank()) {
            return emptyList()
        }

        val graphqlQuery = """
            query (${'$'}search: String) {
                Page(page: 1, perPage: 20) {
                    media(
                        search: ${'$'}search,
                        type: ANIME
                    ) {
                        id
                        title {
                            romaji
                            english
                            native
                        }
                        coverImage {
                            large
                        }
                    }
                }
            }
        """.trimIndent()

        val body = mapOf(
            "query" to graphqlQuery,
            "variables" to mapOf(
                "search" to query
            )
        ).toJson().toRequestBody(
            "application/json".toMediaType()
        )

        val response = app.post(
            mainUrl,
            requestBody = body
        ).text

        val result = parseJson<AniListResponse>(response)

        return result.data.page.media.mapNotNull { anime ->

            val title =
                anime.title.english
                    ?: anime.title.romaji
                    ?: anime.title.native
                    ?: return@mapNotNull null

            newAnimeSearchResponse(
                title,
                "anilist:${anime.id}",
                TvType.Anime
            ) {
                this.posterUrl = anime.coverImage?.large
            }
        }
    }
}
