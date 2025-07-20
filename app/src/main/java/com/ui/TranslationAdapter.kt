package com.bwc.translator.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bwc.translator.databinding.ItemTranslationBinding

class TranslationAdapter : ListAdapter<TranslationItem, TranslationAdapter.ViewHolder>(TranslationDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTranslationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(private val binding: ItemTranslationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: TranslationItem) {
            with(binding) {
                translationText.text = item.text
                speakerLabel.text = if (item.isUser) "You said:" else "Translation:"

                val gravity = if (item.isUser) {
                    android.view.Gravity.END
                } else {
                    android.view.Gravity.START
                }

                messageContainer.gravity = gravity
                translationText.gravity = gravity
                speakerLabel.gravity = gravity
            }
        }
    }

    class TranslationDiffCallback : DiffUtil.ItemCallback<TranslationItem>() {
        override fun areItemsTheSame(oldItem: TranslationItem, newItem: TranslationItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: TranslationItem, newItem: TranslationItem): Boolean {
            return oldItem == newItem
        }
    }
}

data class TranslationItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean
)
