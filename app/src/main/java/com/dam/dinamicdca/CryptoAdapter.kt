package com.dam.dinamicdca

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import android.widget.Filter
import android.widget.Filterable

class CryptoAdapter(
    private var cryptos: List<CryptoCurrency>,
    private val onCryptoSelected: (CryptoCurrency) -> Unit
) : RecyclerView.Adapter<CryptoAdapter.CryptoViewHolder>(), Filterable {

    private var filteredCryptos = cryptos

    class CryptoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(android.R.id.text1)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CryptoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)
        return CryptoViewHolder(view)
    }

    override fun onBindViewHolder(holder: CryptoViewHolder, position: Int) {
        val crypto = filteredCryptos[position]
        holder.tvName.text = "${crypto.name} (${crypto.ticker})"
        holder.itemView.setOnClickListener { onCryptoSelected(crypto) }
    }

    override fun getItemCount() = filteredCryptos.size

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val query = constraint?.toString()?.lowercase() ?: ""
                val filteredList = if (query.isEmpty()) {
                    cryptos
                } else {
                    cryptos.filter { crypto ->
                        crypto.name.lowercase().contains(query) ||
                        crypto.ticker.lowercase().contains(query)
                    }
                }
                return FilterResults().apply { values = filteredList }
            }

            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                filteredCryptos = results?.values as? List<CryptoCurrency> ?: cryptos
                notifyDataSetChanged()
            }
        }
    }
} 