package com.kodlama.feridunmp3

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.media3.common.MediaItem
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale

class TrackAdapter(
    private val onClick: (Int) -> Unit
) : RecyclerView.Adapter<TrackAdapter.VH>() {

    private var items: List<MediaItem> = emptyList()
    private var currentId: String? = null

    fun submit(newItems: List<MediaItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    fun setCurrent(mediaId: String?) {
        if (mediaId == currentId) return
        val old = items.indexOfFirst { it.mediaId == currentId }
        val new = items.indexOfFirst { it.mediaId == mediaId }
        currentId = mediaId
        if (old >= 0) notifyItemChanged(old)
        if (new >= 0) notifyItemChanged(new)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_track, parent, false)
        return VH(v)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(h: VH, position: Int) {
        val item = items[position]
        val md = item.mediaMetadata
        h.title.text = md.title
        h.artist.text = md.artist
        h.duration.text = formatTime(md.extras?.getLong(TrackRepository.EXTRA_DURATION) ?: 0L)
        h.itemView.isActivated = item.mediaId == currentId
        h.title.setTextColor(
            if (item.mediaId == currentId) 0xFF1F6FEB.toInt() else h.defaultColor
        )
        h.itemView.setOnClickListener {
            val p = h.bindingAdapterPosition
            if (p != RecyclerView.NO_POSITION) onClick(p)
        }
    }

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val title: TextView = v.findViewById(R.id.title)
        val artist: TextView = v.findViewById(R.id.artist)
        val duration: TextView = v.findViewById(R.id.duration)
        val defaultColor: Int = title.currentTextColor
    }

    companion object {
        fun formatTime(ms: Long): String {
            val total = ms / 1000
            val h = total / 3600
            val m = (total % 3600) / 60
            val s = total % 60
            return if (h > 0) String.format(Locale.getDefault(), "%d:%02d:%02d", h, m, s)
            else String.format(Locale.getDefault(), "%d:%02d", m, s)
        }
    }
}
