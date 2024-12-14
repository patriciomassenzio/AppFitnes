package com.example.personalfinancemanager

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.*

class TransactionAdapter(
    private val onTransactionClick: (Transaction) -> Unit // Add a click listener parameter
) : RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    private var transactions: List<Transaction> = listOf()

    fun setTransactions(transactions: List<Transaction>) {
        this.transactions = transactions
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        holder.bind(transactions[position], onTransactionClick) // Pass the click listener to bind
    }

    override fun getItemCount() = transactions.size

    class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val amountTextView: TextView = itemView.findViewById(R.id.amountTextView)
        private val descriptionTextView: TextView = itemView.findViewById(R.id.descriptionTextView)
        private val dateTextView: TextView = itemView.findViewById(R.id.dateTextView)
        private val categoryTextView: TextView = itemView.findViewById(R.id.categoryTextView)

        // Modify the bind function to accept the click listener
        fun bind(transaction: Transaction, onTransactionClick: (Transaction) -> Unit) {
            val sign = if (transaction.isExpense) "-" else "+"
            amountTextView.text = "$sign$${String.format("%.2f", transaction.amount)}"
            descriptionTextView.text = transaction.description
            dateTextView.text = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).format(Date(transaction.date))
            categoryTextView.text = transaction.category

            amountTextView.setTextColor(
                ContextCompat.getColor(
                    itemView.context,
                    if (transaction.isExpense) android.R.color.holo_red_dark
                    else android.R.color.holo_green_dark
                )
            )

            // Set the click listener for the entire itemView
            itemView.setOnClickListener {
                onTransactionClick(transaction)
            }
        }
    }
}

