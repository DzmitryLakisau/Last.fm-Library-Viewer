package by.d1makrat.library_fm.repository


import android.icu.text.SymbolTable
import android.util.Log
import by.d1makrat.library_fm.AppContext
import by.d1makrat.library_fm.Constants.MAX_FOR_SCROBBLES_BY_ARTIST
import by.d1makrat.library_fm.database.DatabaseHelper
import by.d1makrat.library_fm.model.*
import by.d1makrat.library_fm.retrofit.LastFmRestApiService
import by.d1makrat.library_fm.utils.ConnectionChecker
import io.reactivex.Completable
import io.reactivex.Single
import java.net.ConnectException

class Repository(private val restApiWorker: LastFmRestApiService, private val databaseHelper: DatabaseHelper) {

    fun clearDatabase(): Completable {
        return Completable.create{completableEmitter ->
            try {
                databaseHelper.deleteScrobbles()
                databaseHelper.deleteTopAlbums()
                databaseHelper.deleteTopArtists()
                databaseHelper.deleteTopTracks()

                completableEmitter.onComplete()
            }
            catch (e: Exception) {
                completableEmitter.onError(e)
            }
        }
    }

    fun getScrobbles(page: Int, from: Long?, to: Long?): Single<List<Scrobble>> {
        return Single.create { singleEmitter ->
            try {
                lateinit var scrobbles: List<Scrobble>

                if (ConnectionChecker.isNetworkAvailable()) {
                    val response = restApiWorker.getRecentScrobbles(AppContext.getInstance().user.username, page, AppContext.getInstance().limit, from, to)
                            .execute()

                    if (response.isSuccessful) {
                        scrobbles = response.body()!!.getAll()
                        databaseHelper.insertScrobbles(scrobbles)
                    }
                    else throw ConnectException("Server responded with ${response.raw().code} code")
                }
                else {
                    scrobbles = databaseHelper.getScrobbles(page, from, to)
                }

                singleEmitter.onSuccess(scrobbles)
            }
            catch (e: Exception){
                if (!singleEmitter.isDisposed) {
                    singleEmitter.onError(e)
                }
            }
        }
    }

    fun getScrobblesOfArtist(artist: String, page: Int, from: Long?, to: Long?): Single<List<Scrobble>> {
        return Single.create { singleEmitter ->
            try {
                val limit = AppContext.getInstance().limit

                if (ConnectionChecker.isNetworkAvailable()) {

                    val scrobbles = ArrayList<Scrobble>()

                    val scrobblesCount = getScrobblesCountOfArtist(artist)

                    val tracksOfArtist = getTracksOfArtist(artist)

                    for (track in tracksOfArtist) {
                        val response = restApiWorker.getScrobblesOfTrack(AppContext.getInstance().user.username, track.artist, track.title, page, limit, from, to)
                                .execute()

                        if (response.isSuccessful) {
                            val scrobblesOfTrack = response.body()!!.getAll()
                            databaseHelper.insertScrobbles(scrobblesOfTrack)

                            scrobbles.addAll(scrobblesOfTrack)
                            if (scrobbles.size == scrobblesCount) break
                        } else throw ConnectException("Server responded with ${response.raw().code} code")
                    }
                }
                singleEmitter.onSuccess(databaseHelper.getScrobblesOfArtist(artist, from, to))
            } catch (e: Exception) {
                if (!singleEmitter.isDisposed) {
                    singleEmitter.onError(e)
                }
            }
        }
    }

    fun getScrobblesOfAlbum(artist: String, album: String, from: Long?, to: Long?): Single<List<Scrobble>> {
        return Single.create { singleEmitter ->
            try {
                if (ConnectionChecker.isNetworkAvailable()) {
                    var page = 1
//                    do {
//                        val response = restApiWorker.getScrobblesOfArtist(AppContext.getInstance().user.username, artist, page,
//                                if (AppContext.getInstance().limit>MAX_FOR_SCROBBLES_BY_ARTIST) MAX_FOR_SCROBBLES_BY_ARTIST else AppContext.getInstance().limit,
//                                from, to)
//                                .execute()
//
//                        lateinit var artistScrobbles: List<Scrobble>
//                        if (response.isSuccessful) {
//                            artistScrobbles = response.body()!!.getAll()
//                            databaseHelper.insertScrobbles(artistScrobbles)
//                        }
//                        else throw ConnectException("Server responded with ${response.raw().code} code")
//
//                        page++
//                    }
//                    while (artistScrobbles.isNotEmpty())
                }

                singleEmitter.onSuccess(databaseHelper.getScrobblesOfAlbum(artist, album, from, to))
            }
            catch (e: Exception){
                if (!singleEmitter.isDisposed) {
                    singleEmitter.onError(e)
                }
            }
        }
    }

    fun getScrobblesOfTrack(artist: String, track: String, page: Int, startOfPeriod: Long?, endOfPeriod: Long?): Single<List<Scrobble>> {
        return Single.create { singleEmitter ->
            try {
                lateinit var scrobbles: List<Scrobble>

                if (ConnectionChecker.isNetworkAvailable()) {
                    val response = restApiWorker.getScrobblesOfTrack(AppContext.getInstance().user.username, artist, track, page,
                            if (AppContext.getInstance().limit>MAX_FOR_SCROBBLES_BY_ARTIST) MAX_FOR_SCROBBLES_BY_ARTIST else AppContext.getInstance().limit,
                            startOfPeriod, endOfPeriod)
                            .execute()

                    if (response.isSuccessful) {
                            scrobbles = response.body()!!.getAll()
                            databaseHelper.insertScrobbles(scrobbles)
                    }
                    else throw ConnectException("Server responded with ${response.raw().code} code")
                }
                else {
                    scrobbles = databaseHelper.getScrobblesOfTrack(artist, track, page, startOfPeriod, endOfPeriod)
                }

                singleEmitter.onSuccess(scrobbles)
            }
            catch (e: Exception){
                if (!singleEmitter.isDisposed) {
                    singleEmitter.onError(e)
                }
            }
        }
    }

    fun getTopAlbums(period: String, page: Int): Single<TopAlbums> {
        return Single.create { singleEmitter ->
            try {
                lateinit var topAlbums: TopAlbums

                if (ConnectionChecker.isNetworkAvailable()) {

                    val response = restApiWorker.getTopAlbums(AppContext.getInstance().user.username, period, page, AppContext.getInstance().limit).execute()

                    if (response.isSuccessful) {
                        topAlbums = response.body()!!
                        if (page == 1) {
                            databaseHelper.deleteTopAlbums(period)
                        }
                        databaseHelper.insertTopAlbums(topAlbums.items, period)
                    }
                    else throw ConnectException("Server responded with ${response.raw().code} code")
                } else {
                    topAlbums = databaseHelper.getTopAlbums(period, page)
                }

                singleEmitter.onSuccess(topAlbums)
            }
            catch (e: Exception){
                if (!singleEmitter.isDisposed) {
                    singleEmitter.onError(e)
                }
            }
        }
    }

    fun getTopArtists(period: String, page: Int): Single<TopArtists> {
        return Single.create { singleEmitter ->
            try {
                lateinit var topArtists: TopArtists

                if (ConnectionChecker.isNetworkAvailable()) {

                    val response = restApiWorker.getTopArtists(AppContext.getInstance().user.username, period, page, AppContext.getInstance().limit).execute()

                    if (response.isSuccessful) {
                        topArtists = response.body()!!
                        if (page == 1) {
                            databaseHelper.deleteTopArtists(period)
                        }
                        databaseHelper.insertTopArtists(topArtists.items, period)
                    }
                    else throw ConnectException("Server responded with ${response.raw().code} code")
                } else {
                    topArtists = databaseHelper.getTopArtists(period, page)
                }

                singleEmitter.onSuccess(topArtists)
            }
            catch (e: Exception){
                if (!singleEmitter.isDisposed) {
                    singleEmitter.onError(e)
                }
            }
        }
    }

    fun getTopTracks(period: String, page: Int): Single<TopTracks> {
        return Single.create { singleEmitter ->
            try {
                lateinit var topTracks: TopTracks

                if (ConnectionChecker.isNetworkAvailable()) {

                    val response = restApiWorker.getTopTracks(AppContext.getInstance().user.username, period, page, AppContext.getInstance().limit).execute()

                    if (response.isSuccessful) {
                        topTracks = response.body()!!
                        if (page == 1) {
                            databaseHelper.deleteTopTracks(period)
                        }
                        databaseHelper.insertTopTracks(topTracks.items, period)
                    }
                    else throw ConnectException("Server responded with ${response.raw().code} code")
                } else {
                    topTracks = databaseHelper.getTopTracks(period, page)
                }

                singleEmitter.onSuccess(topTracks)
            }
            catch (e: Exception){
                if (!singleEmitter.isDisposed) {
                    singleEmitter.onError(e)
                }
            }
        }
    }

    private fun getTracksOfArtist(artist: String): List<Track> {

        val limit = 1000

        val tracks = ArrayList<Track>()

        var page = 1
        do {
            val response = restApiWorker.getTracksOfArtist(artist, page, limit).execute()

            lateinit var pageOfTracks: List<Track>
            if (response.isSuccessful) {
                pageOfTracks = response.body()!!
            } else throw ConnectException("Server responded with ${response.raw().code} code")

            tracks.addAll(pageOfTracks)
            page++
        } while (pageOfTracks.size == limit)

        return tracks.distinctBy { it.title }.sortedByDescending { it.playcount }
    }

    private fun getScrobblesCountOfArtist(artist: String): Int {
        val response = restApiWorker.getArtistInfo(AppContext.getInstance().user.username, artist).execute()
        if (response.isSuccessful) {
            return response.body()!!.artist.stats?.scrobblesCount!!
        } else throw ConnectException("Server responded with ${response.raw().code} code")
    }
}
