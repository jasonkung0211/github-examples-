package com.github.jasonkung.github.ui.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.switchMap
import androidx.paging.PagedList
import androidx.paging.toLiveData
import com.github.jasonkung.github.api.GitHubService
import com.github.jasonkung.github.model.User
import com.github.jasonkung.github.repo.UserDataSourceFactory
import io.reactivex.disposables.CompositeDisposable
import timber.log.Timber

/**
 * TODO: Repository with LiveData and cache
 */
class MainViewModel(val github: GitHubService) : ViewModel() {

    private val compositeDisposable = CompositeDisposable()

    val query = MutableLiveData<String>()

    val results: LiveData<PagedList<User>> = query.switchMap { q ->
        Timber.d("query $q")
        if (q.isBlank()) {
            MutableLiveData<PagedList<User>>()
        } else {
            val userSource = UserDataSourceFactory(q, compositeDisposable, github)
            userSource.toLiveData(pageSize = 15)
        }
    }

    override fun onCleared() {
        super.onCleared()
        compositeDisposable.dispose()
    }
}
