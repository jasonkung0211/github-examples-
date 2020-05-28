package com.github.jasonkung.github.api

import com.github.jasonkung.github.model.User
import com.github.jasonkung.github.model.Users
import io.reactivex.Observable
import io.reactivex.Single
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url


interface GitHubService {

    @GET("search/users")
    fun getUsersCall(
        @Query("q") query: String,
        @Query("sort") sort: String? = null,
        @Query("order") order: String? = null
    ): Call<Users>

    @GET
    fun getUsersCallByUrl(
        @Url url: String
    ): Call<Users>

}

/**
 * TODO: Move to utils/CallKtx
 */
fun <T> Call<T>.enqueue(onResponse: (call: Call<T>, response: Response<T>) -> Unit = { _, _ -> },
                        onFailure: (call: Call<T>, e: Throwable) -> Unit = { _, _ -> }) {
    enqueue(object : Callback<T> {
        override fun onResponse(call: Call<T>, response: Response<T>) {
            onResponse(call, response)
        }

        override fun onFailure(call: Call<T>, e: Throwable) {
            onFailure(call, e)
        }
    });
}

/**
 * TODO: Move util/
 * ref. https://github.com/eclipse/egit-github/blob/master/org.eclipse.egit.github.core/src/org/eclipse/egit/github/core/client/PageLinks.java
 */
class PageLinks(link: String) {

    /**
     * @return first
     */
    var first: String? = null
    /**
     * @return last
     */
    var last: String? = null
    /**
     * @return next
     */
    var next: String? = null
    /**
     * @return prev
     */
    var prev: String? = null

    companion object {
        private const val DELIM_LINKS = "," //$NON-NLS-1$
        private const val DELIM_LINK_PARAM = ";" //$NON-NLS-1$
        private const val META_REL = "rel" //$NON-NLS-1$
        private const val META_LAST = "last" //$NON-NLS-1$
        private const val META_NEXT = "next" //$NON-NLS-1$
        private const val META_FIRST = "first" //$NON-NLS-1$
        private const val META_PREV = "prev" //$NON-NLS-1$
    }

    /**
     * Parse links from executed method
     */
    init {
        val links = link.split(DELIM_LINKS).toTypedArray()
        for (_link in links) {
            val segments = _link.split(DELIM_LINK_PARAM).toTypedArray()
            if (segments.size < 2) continue
            var linkPart = segments[0].trim { it <= ' ' }
            if (!linkPart.startsWith("<") || !linkPart.endsWith(">")) //$NON-NLS-1$ //$NON-NLS-2$
                continue
            linkPart = linkPart.substring(1, linkPart.length - 1)
            for (i in 1 until segments.size) {
                val rel =
                    segments[i].trim { it <= ' ' }.split("=").toTypedArray() //$NON-NLS-1$
                if (rel.size < 2 || META_REL != rel[0]) continue
                var relValue = rel[1]
                if (relValue.startsWith("\"") && relValue.endsWith("\"")) //$NON-NLS-1$ //$NON-NLS-2$
                    relValue = relValue.substring(1, relValue.length - 1)
                when {
                    META_FIRST == relValue -> first = linkPart
                    META_LAST == relValue -> last = linkPart
                    META_NEXT == relValue -> next = linkPart
                    META_PREV == relValue -> prev = linkPart
                }
            }
        }
    }
}
