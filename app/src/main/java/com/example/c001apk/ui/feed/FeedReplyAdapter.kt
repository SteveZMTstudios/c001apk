package com.example.c001apk.ui.feed

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import androidx.core.view.setPadding
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.absinthe.libraries.utils.extensions.dp
import com.example.c001apk.BR
import com.example.c001apk.R
import com.example.c001apk.adapter.ItemListener
import com.example.c001apk.databinding.ItemFeedContentReplyItemBinding
import com.example.c001apk.logic.model.Like
import com.example.c001apk.logic.model.TotalReplyResponse
import com.example.c001apk.util.BlackListUtil
import com.example.c001apk.util.SpannableStringBuilderUtil
import com.example.c001apk.util.Utils.getColorFromAttr
import com.example.c001apk.view.LinkMovementClickMethod

class FeedReplyAdapter(
    private val listener: ItemListener,
) :
    ListAdapter<TotalReplyResponse.Data, FeedReplyAdapter.ViewHolder>(FeedReplyDiffCallback()) {

    private var haveTop = false
    private var topReplyId: String? = null

    fun setHaveTop(haveTop: Boolean, topReplyId: String?) {
        this.haveTop = haveTop
        this.topReplyId = topReplyId
    }

    class ViewHolder(val binding: ItemFeedContentReplyItemBinding, val listener: ItemListener) :
        RecyclerView.ViewHolder(binding.root) {
        var id: String = ""
        var uid: String = ""
        var username: String = ""
        var message: String = ""
        var likeData: Like = Like()

        init {
            itemView.setOnClickListener {
                listener.onReply(
                    id, uid, username,
                    bindingAdapterPosition, null
                )
            }

            binding.like.setOnClickListener {
                listener.onLikeClick(
                    "feedReply", id, bindingAdapterPosition, likeData
                )
            }

            binding.expand.setOnClickListener {
                listener.onExpand(
                    it, id, uid,
                    message, bindingAdapterPosition, null
                )
            }
        }

        @SuppressLint("SetTextI18n")
        fun bind(reply: TotalReplyResponse.Data, haveTop: Boolean, topReplyId: String?) {

            if (!reply.username.contains("[楼主]") && !reply.username.contains("[置顶]")) {
                val unameTag =
                    when (reply.uid) {
                        reply.feedUid -> " [楼主]"
                        else -> ""
                    }
                val replyTag =
                    when (haveTop && reply.id == topReplyId) {
                        true -> {
                            if (bindingAdapterPosition == 0) " [置顶]"
                            else ""
                        }

                        else -> ""
                    }
                reply.username = "${reply.username}$unameTag$replyTag\u3000"
            }

            id = reply.id
            uid = reply.uid
            username = reply.username
            message = reply.message

            binding.setVariable(BR.data, reply)
            binding.setVariable(BR.listener, listener)
            likeData = Like().also {
                it.likeNum.set(reply.likenum)
                reply.userAction?.like?.let { like ->
                    it.isLike.set(like)
                }
            }
            binding.setVariable(BR.likeData, likeData)

            if (!reply.replyRows.isNullOrEmpty()) {
                val sortedList = ArrayList<TotalReplyResponse.Data>()
                for (element in reply.replyRows) {
                    if (!BlackListUtil.checkUid(element.uid))
                        sortedList.add(element)
                }
                if (sortedList.isNotEmpty()) {
                    binding.replyLayout.visibility = View.VISIBLE
                    if (itemView.layoutParams is StaggeredGridLayoutManager.LayoutParams) {
                        binding.replyLayout.setCardBackgroundColor(
                            itemView.context.getColorFromAttr(
                                android.R.attr.windowBackground
                            )
                        )
                    }
                    binding.linearAdapterLayout.adapter = object : BaseAdapter() {
                        override fun getCount(): Int = sortedList.size
                        override fun getItem(p0: Int): Any = 0
                        override fun getItemId(p0: Int): Long = 0
                        override fun getView(
                            position1: Int,
                            convertView: View?,
                            parent: ViewGroup
                        ): View {
                            val view = LayoutInflater.from(parent.context).inflate(
                                R.layout.item_feed_content_reply_to_reply_item,
                                parent,
                                false
                            )
                            val replyData = sortedList[position1]
                            val textView: TextView = view.findViewById(R.id.reply)
                            textView.highlightColor = ColorUtils.setAlphaComponent(
                                parent.context.getColorFromAttr(
                                    rikka.preference.simplemenu.R.attr.colorPrimaryDark
                                ), 128
                            )

                            val replyTag1 =
                                when (replyData.uid) {
                                    reply.feedUid -> " [楼主] "
                                    reply.uid -> " [层主] "
                                    else -> ""
                                }

                            val rReplyTag =
                                when (replyData.ruid) {
                                    reply.feedUid -> " [楼主] "
                                    reply.uid -> " [层主] "
                                    else -> ""
                                }

                            val rReplyUser =
                                when (replyData.ruid) {
                                    reply.uid -> ""
                                    else -> """<a class="feed-link-uname" href="/u/${replyData.ruid}">${replyData.rusername}${rReplyTag}</a>"""
                                }

                            val replyPic =
                                when (replyData.pic) {
                                    "" -> ""
                                    else -> """ <a class=\"feed-forward-pic\" href=${replyData.pic}>查看图片(${replyData.picArr?.size})</a>"""
                                }

                            val mess =
                                """<a class="feed-link-uname" href="/u/${replyData.uid}">${replyData.username}${replyTag1}</a>回复${rReplyUser}: ${replyData.message}${replyPic}"""

                            textView.movementMethod = LinkMovementClickMethod.instance

                            textView.text = SpannableStringBuilderUtil.setText(
                                parent.context,
                                mess,
                                textView.textSize,
                                replyData.picArr
                            )

                            SpannableStringBuilderUtil.setData(
                                position1 + 1,
                                reply.uid
                            )

                            view.setOnClickListener {
                                listener.onReply(
                                    replyData.id, replyData.uid, replyData.username,
                                    bindingAdapterPosition, position1
                                )
                            }

                            view.setOnLongClickListener {
                                listener.onExpand(
                                    it, replyData.id, replyData.uid,
                                    replyData.message, bindingAdapterPosition, position1
                                )
                                true
                            }

                            return view
                        }
                    }
                } else binding.replyLayout.visibility = View.GONE
            } else binding.replyLayout.visibility = View.GONE

            if (reply.replyRowsMore != 0) {
                binding.totalReply.visibility = View.VISIBLE
                val count = reply.replyRowsMore + reply.replyRows?.size!!
                binding.totalReply.text = "查看更多回复($count)"
                binding.totalReply.setOnClickListener {
                    listener.showTotalReply(
                        reply.id, reply.uid,
                        bindingAdapterPosition,
                        null
                    )
                }
            } else
                binding.totalReply.visibility = View.GONE

            binding.executePendingBindings()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemFeedContentReplyItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        binding.root.also {
            if (it.layoutParams is StaggeredGridLayoutManager.LayoutParams) {
                it.background = parent.context.getDrawable(R.drawable.text_card_bg)
                it.foreground = parent.context.getDrawable(R.drawable.selector_bg_12_trans)
                it.setPadding(10.dp)
            } else {
                it.foreground = parent.context.getDrawable(R.drawable.selector_bg_trans)
                it.setPadding(15.dp, 12.dp, 15.dp, 12.dp)
            }
        }
        return ViewHolder(binding, listener)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(currentList[position], haveTop, topReplyId)
    }

}

class FeedReplyDiffCallback : DiffUtil.ItemCallback<TotalReplyResponse.Data>() {
    override fun areItemsTheSame(
        oldItem: TotalReplyResponse.Data,
        newItem: TotalReplyResponse.Data
    ): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(
        oldItem: TotalReplyResponse.Data,
        newItem: TotalReplyResponse.Data
    ): Boolean {
        return oldItem.id == newItem.id && oldItem.username == newItem.username
    }
}