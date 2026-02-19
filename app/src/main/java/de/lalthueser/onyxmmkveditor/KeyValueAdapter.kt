package de.lalthueser.onyxmmkveditor

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class KeyValueAdapter(
    private var items: List<Pair<String, String>>,
    private val onClick: (String, String) -> Unit
) : RecyclerView.Adapter<KeyValueAdapter.VH>() {

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvKey: TextView = view.findViewById(R.id.tvKey)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_kv, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val (k, v) = items[position]
        holder.tvKey.text = k
        holder.itemView.setOnClickListener { onClick(k, v) }
    }

    override fun getItemCount(): Int = items.size

    fun update(newItems: List<Pair<String, String>>) {
        items = newItems
        notifyDataSetChanged()
    }
}
