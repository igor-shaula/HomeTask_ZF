package com.igor_shaula.hometask_zf

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class CarListAdapter(private val onClickFunction: (CarItemRecord, Int) -> Unit) :
    RecyclerView.Adapter<CarItemViewHolder>() {

    private var items: MutableList<CarItemRecord> = mutableListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CarItemViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_car, parent, false)
        return CarItemViewHolder(itemView, onClickFunction)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: CarItemViewHolder, position: Int) {
        holder.bind(items[position], position)
    }

    fun setItems(items: MutableList<CarItemRecord>) {
        this.items = items
        notifyDataSetChanged() // todo remove this warning by providing the position somehow
    }
}