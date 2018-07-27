package by.d1makrat.library_fm.json

import by.d1makrat.library_fm.Constants.*
import by.d1makrat.library_fm.Constants.JsonConstants.*
import by.d1makrat.library_fm.json.model.ScrobblesJsonModel
import by.d1makrat.library_fm.model.Scrobble
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import org.json.JSONException

class ScrobblesAdapter : TypeAdapter<ScrobblesJsonModel>() {

    private val RECENT_TRACKS_KEY = "recenttracks"
    private val ARTIST_TRACKS_KEY = "artisttracks"
    private val MAX_IMAGE_RESOLUTION_INDEX = 3

    private val mGson = Gson()

    @Throws(JSONException::class)
    override fun write(jsonWriter: JsonWriter, scrobblesJsonModel: ScrobblesJsonModel) {
        mGson.toJson(scrobblesJsonModel, ScrobblesJsonModel::class.java, jsonWriter)
    }

    @Throws(JSONException::class)
    override fun read(jsonReader: JsonReader): ScrobblesJsonModel {
        val scrobbles = ScrobblesJsonModel()
        val rootElement = mGson.getAdapter(JsonElement::class.java).read(jsonReader).asJsonObject

        val scrobblesJsonArray = if (rootElement.asJsonObject.has(RECENT_TRACKS_KEY))
            rootElement.asJsonObject.get(RECENT_TRACKS_KEY).asJsonObject.get(TRACK_KEY).asJsonArray
        else rootElement.asJsonObject.get(ARTIST_TRACKS_KEY).asJsonObject.get(TRACK_KEY).asJsonArray

        for (i in 0 until scrobblesJsonArray.size()) {
            val scrobbleJsonObject = scrobblesJsonArray.get(i).asJsonObject

            if (!scrobbleJsonObject.has(ATTRIBUTE_KEY)) {//TODO parse and show scrobble that "now playing"
                val scrobble = Scrobble()

                scrobble.TrackTitle = scrobbleJsonObject.get(NAME_KEY).asString
                scrobble.Artist = scrobbleJsonObject.get(ARTIST_KEY).asJsonObject.get(TEXT_KEY).asString
                scrobble.Album = scrobbleJsonObject.get(ALBUM_KEY).asJsonObject.get(TEXT_KEY).asString
                scrobble.ImageUrl = scrobbleJsonObject.get(IMAGE_KEY).asJsonArray.get(MAX_IMAGE_RESOLUTION_INDEX).asJsonObject.get(TEXT_KEY).asString
                scrobble.setDate(scrobbleJsonObject.get(DATE_KEY).asJsonObject.get("uts").asLong)

                scrobbles.add(scrobble)
            }
        }

        return scrobbles
    }
}
