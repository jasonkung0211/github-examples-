package com.github.jasonkung.github.repo

import androidx.lifecycle.MutableLiveData
import androidx.paging.DataSource
import androidx.paging.PageKeyedDataSource
import com.github.jasonkung.github.api.GitHubService
import com.github.jasonkung.github.api.PageLinks
import com.github.jasonkung.github.api.enqueue
import com.github.jasonkung.github.model.User
import com.github.jasonkung.github.model.Users
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import retrofit2.Response

// ref. https://developer.android.com/topic/libraries/architecture/paging
// ref. https://developer.android.com/topic/libraries/architecture/paging/data
class UserDataSource(
    private val query: String,
    private val githubService: GitHubService,
    private val compositeDisposable: CompositeDisposable
)
    : PageKeyedDataSource<String, User>() {

    override fun loadInitial(
        params: LoadInitialParams<String>,
        callback: LoadInitialCallback<String, User>
    ) {
        compositeDisposable.add(
            Single.create<Response<Users>> {
                val call = githubService.getUsersCall(query)
                call.enqueue(
                    onResponse = { _, response -> it.onSuccess(response) },
                    onFailure = { _, e -> it.onError(e) }
                )
            }
                .subscribe({ response ->
                    val links = response.headers().get("Link")
                        ?.let { PageLinks(it) }
                    val users = response.body()?.items ?: emptyList()
                    callback.onResult(users, links?.prev, links?.next)
                }, { e -> })
        )
    }

    override fun loadAfter(params: LoadParams<String>, callback: LoadCallback<String, User>) {
        compositeDisposable.add(
            Single.create<Response<Users>> {
                val call = githubService.getUsersCallByUrl(params.key)
                call.enqueue(
                    onResponse = { _, response -> it.onSuccess(response) },
                    onFailure = { _, e -> it.onError(e) }
                )
            }
                .subscribe({ response ->
                    val links = response.headers().get("Link")
                        ?.let { PageLinks(it) }
                    val users = response.body()?.items ?: emptyList()
                    callback.onResult(users, links?.next)
                }, { e -> })
        )
    }

    override fun loadBefore(params: LoadParams<String>, callback: LoadCallback<String, User>) {
        compositeDisposable.add(
            Single.create<Response<Users>> {
                val call = githubService.getUsersCallByUrl(params.key)
                call.enqueue(
                    onResponse = { _, response -> it.onSuccess(response) },
                    onFailure = { _, e -> it.onError(e) }
                )
            }
                .subscribe({ response ->
                    val links = response.headers().get("Link")
                        ?.let { PageLinks(it) }
                    val users = response.body()?.items ?: emptyList()
                    callback.onResult(users, links?.prev)
                }, { e -> })
        )
    }
}

// ref. https://developer.android.com/topic/libraries/architecture/paging
// ref. https://developer.android.com/topic/libraries/architecture/paging/data
class UserDataSourceFactory(
    private val query: String,
    private val compositeDisposable: CompositeDisposable,
    private val githubService: GitHubService
)
    : DataSource.Factory<String, User>() {

    private val usersDataSourceLiveData = MutableLiveData<UserDataSource>()

    override fun create(): DataSource<String, User> {
        val usersDataSource = UserDataSource(query, githubService, compositeDisposable)
        usersDataSourceLiveData.postValue(usersDataSource)
        return usersDataSource
    }
}

