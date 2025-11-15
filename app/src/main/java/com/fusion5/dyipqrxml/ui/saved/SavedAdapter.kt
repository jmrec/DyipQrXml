package com.fusion5.dyipqrxml.ui.saved

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.fusion5.dyipqrxml.data.model.Terminal
import com.fusion5.dyipqrxml.databinding.ItemSavedTerminalBinding

class SavedAdapter(
    private val onTerminalClick: (Terminal) -> Unit,
    private val onRemoveFavorite: (Terminal) -> Unit
) : ListAdapter<Terminal, SavedAdapter.SavedTerminalViewHolder>(SavedTerminalDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SavedTerminalViewHolder {
        val binding = ItemSavedTerminalBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SavedTerminalViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SavedTerminalViewHolder, position: Int) {
        val terminal = getItem(position)
        holder.bind(terminal)
    }

    inner class SavedTerminalViewHolder(
        private val binding: ItemSavedTerminalBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(terminal: Terminal) {
            binding.textTerminalName.text = terminal.name
            binding.textTerminalDescription.text = terminal.description
            
            // Show location info if available
            if (terminal.latitude != null && terminal.longitude != null) {
                binding.textLocationInfo.text = "Location available"
                binding.textLocationInfo.visibility = View.VISIBLE
            } else {
                binding.textLocationInfo.visibility = View.GONE
            }

            binding.root.setOnClickListener {
                onTerminalClick(terminal)
            }

            binding.buttonRemove.setOnClickListener {
                onRemoveFavorite(terminal)
            }
        }
    }

    object SavedTerminalDiffCallback : DiffUtil.ItemCallback<Terminal>() {
        override fun areItemsTheSame(oldItem: Terminal, newItem: Terminal): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Terminal, newItem: Terminal): Boolean {
            return oldItem == newItem
        }
    }
}