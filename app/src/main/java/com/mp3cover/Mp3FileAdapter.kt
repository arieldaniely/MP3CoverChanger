package com.mp3cover

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.mp3cover.databinding.ItemMp3FileBinding

class Mp3FileAdapter(
    private val items: MutableList<Mp3FileItem>,
    private val onCoverChangeClick: (Mp3FileItem, Int) -> Unit,
    private val onRemoveClick: (Int) -> Unit
) : RecyclerView.Adapter<Mp3FileAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemMp3FileBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Mp3FileItem, position: Int) {
            // Cover art
            if (item.coverArt != null) {
                binding.imgCover.setImageBitmap(item.coverArt)
                binding.imgCoverPlaceholder.visibility = android.view.View.GONE
                binding.imgCover.visibility = android.view.View.VISIBLE
            } else {
                binding.imgCover.visibility = android.view.View.GONE
                binding.imgCoverPlaceholder.visibility = android.view.View.VISIBLE
            }

            // Text info
            binding.tvTitle.text = item.title?.takeIf { it.isNotBlank() }
                ?: item.fileName.removeSuffix(".mp3")
            binding.tvArtist.text = buildString {
                if (!item.artist.isNullOrBlank()) append(item.artist)
                if (!item.album.isNullOrBlank()) {
                    if (isNotEmpty()) append(" • ")
                    append(item.album)
                }
                if (isEmpty()) append("לא ידוע")
            }
            binding.tvFileSize.text = "${item.fileSizeKb} KB"
            binding.tvFileName.text = item.fileName

            // Cover art badge
            if (item.coverArt != null) {
                binding.tvCoverBadge.text = "✓ יש תמונה"
                binding.tvCoverBadge.setBackgroundResource(R.drawable.badge_has_cover)
            } else {
                binding.tvCoverBadge.text = "אין תמונה"
                binding.tvCoverBadge.setBackgroundResource(R.drawable.badge_no_cover)
            }

            binding.btnChangeCover.setOnClickListener {
                onCoverChangeClick(item, position)
            }

            binding.btnRemove.setOnClickListener {
                onRemoveClick(position)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMp3FileBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position], position)
    }

    override fun getItemCount() = items.size
}
