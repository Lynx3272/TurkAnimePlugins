package com.example

import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.TvType

class TurkAnimeProvider : MainAPI() {

    override var mainUrl = "https://example.com/"
    override var name = "Turk Anime"
    override val supportedTypes = setOf(
        TvType.Anime,
        TvType.AnimeMovie
    )

    override var lang = "tr"
    override val hasMainPage = true

    override suspend fun search(query: String): List<SearchResponse> {
        return emptyList()
    }
}
