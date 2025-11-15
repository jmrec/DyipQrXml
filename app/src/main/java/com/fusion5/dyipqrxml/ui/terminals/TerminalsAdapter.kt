package com.fusion5.dyipqrxml.ui.terminals

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.fusion5.dyipqrxml.R
import com.fusion5.dyipqrxml.databinding.ItemTerminalBinding

class TerminalsAdapter(
    private val onTerminalClick: (TerminalListItem) -> Unit,
    private val onToggleFavorite: (TerminalListItem) -> Unit
) : ListAdapter<TerminalListItem, TerminalsAdapter.TerminalViewHolder>(TerminalDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TerminalViewHolder {
        val binding = ItemTerminalBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TerminalViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TerminalViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TerminalViewHolder(
        private val binding: ItemTerminalBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: TerminalListItem) {
            val terminal = item.terminal
            binding.textTerminalName.text = terminal.name
            binding.textTerminalDescription.text = terminal.description

            if (terminal.latitude != null && terminal.longitude != null) {
                binding.textLocationInfo.text = itemView.context.getString(R.string.location_available)
                binding.textLocationInfo.visibility = View.VISIBLE
            } else {
                binding.textLocationInfo.visibility = View.GONE
            }

            binding.buttonFavorite.setImageResource(
                if (item.isFavorite) R.drawable.ic_bookmark_added else R.drawable.ic_bookmark_add
            )

            binding.buttonFavorite.contentDescription = itemView.context.getString(
                if (item.isFavorite) R.string.favorite_remove_desc else R.string.favorite_add_desc
            )

            binding.buttonFavorite.setOnClickListener { onToggleFavorite(item) }
            binding.root.setOnClickListener { onTerminalClick(item) }
        }
    }

    object TerminalDiffCallback : DiffUtil.ItemCallback<TerminalListItem>() {
        override fun areItemsTheSame(oldItem: TerminalListItem, newItem: TerminalListItem): Boolean =
            oldItem.terminal.id == newItem.terminal.id
        override fun areContentsTheSame(oldItem: TerminalListItem, newItem: TerminalListItem): Boolean =
            oldItem == newItem
    }
}