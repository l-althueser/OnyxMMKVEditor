package de.lalthueser.onyxmmkveditor

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class KeyValueAdapter(
    private var items: List<Pair<String, String>>,
    private val onClick: (String, String) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var errorMessage: String? = null

    companion object {
        private const val TYPE_ITEM = 0
        private const val TYPE_ERROR = 1
    }

    class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvKey: TextView = view.findViewById(R.id.tvKey)
    }

    class ErrorViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvError: TextView = view.findViewById(android.R.id.text1)
    }

    override fun getItemViewType(position: Int): Int {
        return if (errorMessage != null) TYPE_ERROR else TYPE_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_ITEM) {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_kv, parent, false)
            ItemViewHolder(v)
        } else {
            val v = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_1, parent, false)
            ErrorViewHolder(v)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ItemViewHolder) {
            val (k, v) = items[position]
            holder.tvKey.text = k
            holder.itemView.setOnClickListener { onClick(k, v) }
        } else if (holder is ErrorViewHolder) {
            holder.tvError.text = errorMessage
        }
    }

    override fun getItemCount(): Int = if (errorMessage != null) 1 else items.size

    fun update(newItems: List<Pair<String, String>>) {
        errorMessage = null
        items = newItems
        notifyDataSetChanged()
    }

    fun showError(message: String) {
        errorMessage = message
        items = emptyList()
        notifyDataSetChanged()
    }
}
