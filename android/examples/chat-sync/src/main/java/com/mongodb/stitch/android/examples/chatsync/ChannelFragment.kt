package com.mongodb.stitch.android.examples.chatsync

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import com.mongodb.stitch.android.examples.chatsync.model.ChannelMessage
import com.mongodb.stitch.android.examples.chatsync.service.ChannelServiceAction
import com.mongodb.stitch.android.examples.chatsync.viewModel.ChannelViewModel
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class ChannelFragment : Fragment() {
    private lateinit var channelViewModel: ChannelViewModel

    private val channelMessagesRecyclerView by lazy {
        view!!.findViewById<RecyclerView>(R.id.channel_messages_recycler_view)
    }

    private val adapter by lazy { MessageAdapter(this.activity!!) }
    private var isInitialized = false

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.content_main, container, false)
    }

    private fun sendMessage(v: View) {
        val messageEditText = v.rootView.findViewById<EditText>(R.id.message_edit_text)
        channelViewModel.channel.observe(
            this, object : Observer<ChannelServiceAction> {

            override fun onChanged(t: ChannelServiceAction?) {
                channelViewModel.channel.removeObserver(this)
                channelViewModel.channel.sendMessage(messageEditText.text.toString())
            }
        })
    }

    private fun channelObserver() = Observer<ChannelServiceAction> { data ->
        Log.d("ChannelObserver", "LiveData changed: $data")
        when (data) {
            is ChannelServiceAction.SubscribeToChannelReply -> {
                if (!isInitialized) {
                    GlobalScope.launch(Main) {
                        val (channel) = data
                        GlobalScope.launch(IO) {

                            adapter.setCursor(
                                SparseRemoteMongoCursor(
                                    ChannelMessage.getMessages(channel.id),
                                    ChannelMessage.getMessagesCount(channel.id).toInt()))
                            channelMessagesRecyclerView.smoothScrollToPosition(0)
                        }.join()
                        view?.findViewById<Button>(R.id.send_button)?.isEnabled = true
                        isInitialized = true
                    }
                }
            }
            is ChannelServiceAction.SendMessageReply -> {
                adapter.put(data.channelMessage)
            }
            is ChannelServiceAction.NewMessageReply -> {
                adapter.put(data.channelMessage)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val layoutManager = LinearLayoutManager(this.context)

        layoutManager.stackFromEnd = true
        layoutManager.reverseLayout = true
        layoutManager.orientation = LinearLayoutManager.VERTICAL
        channelMessagesRecyclerView.layoutManager = layoutManager
        channelMessagesRecyclerView.adapter = adapter
        channelMessagesRecyclerView.addOnLayoutChangeListener { _, _, _, _, bottom, _, _, _, oldBottom ->
            if (bottom < oldBottom) {
                channelMessagesRecyclerView.smoothScrollToPosition(0)
            }
        }

        val sendButton = view.findViewById<Button>(R.id.send_button)
        sendButton.setOnClickListener(::sendMessage)
        sendButton.isEnabled = false

        channelViewModel = ViewModelProviders.of(activity!!).get(ChannelViewModel::class.java)
        channelViewModel.selectChannel(view.context, user.channelsSubscribedTo.first())
            .observe(this, channelObserver())
    }
}
