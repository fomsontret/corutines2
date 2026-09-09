package coroutines

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import coroutines.dto.Author
import coroutines.dto.Post
import coroutines.dto.PostWithAuthor
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

private val gson = Gson()

private const val BASE_URL = "http://127.0.0.1:9999"

private val client = OkHttpClient.Builder()
    .addInterceptor(
        HttpLoggingInterceptor(::println).apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    )
    .connectTimeout(30, TimeUnit.SECONDS)
    .build()

fun main() {
    with(CoroutineScope(EmptyCoroutineContext)) {
        launch {
            try {

                val posts = getPosts(client)


                val postsWithAuthors = posts.map { post ->

                    val author = Author(
                        id = post.id,
                        name = post.author,
                        avatar = post.authorAvatar
                    )

                    PostWithAuthor(
                        post = post,
                        author = author
                    )
                }

                println(postsWithAuthors)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Thread.sleep(30_000L)
}

suspend fun OkHttpClient.apiCall(url: String): Response {
    return suspendCoroutine { continuation ->

        Request.Builder()
            .url(url)
            .build()
            .let(::newCall)
            .enqueue(
                object : Callback {

                    override fun onResponse(
                        call: Call,
                        response: Response
                    ) {
                        continuation.resume(response)
                    }

                    override fun onFailure(
                        call: Call,
                        e: IOException
                    ) {
                        continuation.resumeWithException(e)
                    }
                }
            )
    }
}

suspend fun <T> makeRequest(
    url: String,
    client: OkHttpClient,
    typeToken: TypeToken<T>
): T = withContext(Dispatchers.IO) {

    client.apiCall(url).use { response ->

        if (!response.isSuccessful) {
            throw RuntimeException(
                "HTTP ${response.code}: ${response.message}"
            )
        }

        val body = response.body
            ?: throw RuntimeException("Response body is null")

        gson.fromJson(body.string(), typeToken.type)
    }
}

suspend fun getPosts(client: OkHttpClient): List<Post> =
    makeRequest(
        "$BASE_URL/api/slow/posts",
        client,
        object : TypeToken<List<Post>>() {}
    )



