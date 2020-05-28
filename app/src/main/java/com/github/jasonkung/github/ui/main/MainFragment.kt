package com.github.jasonkung.github.ui.main

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.getSystemService
import androidx.fragment.app.Fragment
import androidx.lifecycle.observe
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.github.jasonkung.github.R
import com.github.jasonkung.github.model.User
import com.github.jasonkung.github.ui.main.UserAdapter.Companion.ONE_ONE
import com.squareup.picasso.Picasso
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.main_fragment.*
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainFragment : Fragment() {

    companion object {
        fun newInstance() = MainFragment()
    }

    private val viewModel: MainViewModel by viewModel()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.main_fragment, container, false)
    }

    // ref. https://developer.android.com/topic/libraries/architecture/paging
    private val adapter by lazy { UserAdapter() }

    private val disposable = CompositeDisposable()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        search_edit_view.apply {
            setOnEditorActionListener { v, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    viewModel.query.value = search_edit_view.text.toString()
                    viewModel.results
                        .observe(viewLifecycleOwner, adapter::submitList)
                    v.hideKeyboard()
                    true
                } else {
                    false
                }
            }
            setOnKeyListener { v, keyCode, event ->
                if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                    viewModel.query.value = search_edit_view.text.toString()
                    viewModel.results
                        .observe(viewLifecycleOwner, adapter::submitList)
                    v.hideKeyboard()
                    true
                } else {
                    false
                }
            }
        }

        list_view.initial(
            adapter = { this@MainFragment.adapter },
            layoutManager = {
                GridLayoutManager(context, 2).apply {
                    spanSizeLookup { position ->
                        val viewType = this@MainFragment.adapter.getItemViewType(position)
                        if (viewType == ONE_ONE) 1 else 2
                    }
                }
            }
        )
    }

    override fun onStop() {
        super.onStop()
        disposable.clear()
    }

}

class UserAdapter : PagedListAdapter<User, UserViewHolder>(DIFF_CALLBACK) {
    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bindTo(getItem(position))
    }

    companion object {
        private val DIFF_CALLBACK = object :
            DiffUtil.ItemCallback<User>() {
            // User details may have changed if reloaded from the database,
            // but ID is fixed.
            override fun areItemsTheSame(oldUser: User,
                                         newUser: User) = oldUser.id == newUser.id

            override fun areContentsTheSame(oldUser: User,
                                            newUser: User) = oldUser == newUser
        }
        const val ONE_ONE = 0
        const val TWO_ONE = 1
        const val TWO_TWO = 2
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = when (viewType) {
            TWO_TWO -> {
                LayoutInflater.from(parent.context)
                    .inflate( R.layout.user_item2, parent, false)
            }
            TWO_ONE -> {
                LayoutInflater.from(parent.context)
                    .inflate( R.layout.user_item2_1, parent, false)
            }
            else -> {
                LayoutInflater.from(parent.context)
                    .inflate( R.layout.user_item, parent, false)
            }
        }
        return UserViewHolder(view)
    }

    override fun getItemViewType(position: Int): Int {
        return if ((position % 3) == 0) {
            listOf(TWO_ONE, TWO_TWO).random()
        } else {
            ONE_ONE
        }
    }
}

class UserViewHolder(itemView: View) : ViewHolder(itemView) {
    private val avatar = itemView.findViewById<ImageView>(R.id.image1)
    private val name = itemView.findViewById<TextView>(R.id.text1)
    var user : User? = null

    /**
     * Items might be null if they are not paged in yet. PagedListAdapter will re-bind the
     * ViewHolder when Item is loaded.
     */
    fun bindTo(user : User?) {
        this.user = user
        name.text = user?.name ?: user?.login ?: ""
        /// facebook/fresco > google-family/glide > picasso (smallest)
        user?.avatarUrl?.let { Picasso.get().load(it).into(avatar) }
    }
}

// TODO: Move to util.ViewKtx
fun View.hideKeyboard() {
    context.getSystemService<InputMethodManager>()?.hideSoftInputFromWindow(windowToken, 0)
}

// TODO: Move to util.GridLayoutManagerKtx
fun GridLayoutManager.spanSizeLookup(onSpanSize: (Int) -> Int) {
    spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
        override fun getSpanSize(position: Int): Int {
            return onSpanSize(position)
        }
    }
}

fun <VH : RecyclerView.ViewHolder> RecyclerView.initial(layoutManager: () -> RecyclerView.LayoutManager,
                                                        adapter: () -> RecyclerView.Adapter<VH>) {
    this.layoutManager = layoutManager()
    this.adapter = adapter()
}
