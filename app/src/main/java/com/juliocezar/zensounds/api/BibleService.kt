import com.google.gson.JsonElement
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.Call

interface BibleService {
    @GET("{translation}/{book}/{chapter}.json")
    fun getChapter(
        @Path("translation") translation: String, // ex.: "BSB"
        @Path("book") book: String,               // ex.: "GEN"
        @Path("chapter") chapter: Int             // ex.: 1
    ): Call<JsonElement>
}
