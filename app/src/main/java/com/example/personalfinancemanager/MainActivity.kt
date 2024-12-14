package com.example.personalfinancemanager

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.floatingactionbutton.FloatingActionButton
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.util.Log
import android.widget.EditText


class MainActivity : AppCompatActivity() {
    private lateinit var viewModel: TransactionViewModel
    private lateinit var balanceTextView: TextView
    private lateinit var transactionsRecyclerView: RecyclerView
    private lateinit var transactionAdapter: TransactionAdapter

    private val categories = listOf("Food", "Utilities", "Entertainment", "Transaction", "Health", "Other")

    private var budget: Double = 5000.00 // Default budget value
    private var totalExpenses: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupViewModel()
        initializeViews()
        setupRecyclerView()
        observeViewModel()
    }

    private fun setupViewModel() {
        val transactionDao = AppDatabase.getDatabase(applicationContext).transactionDao()
        val repository = TransactionRepository(transactionDao)
        viewModel = ViewModelProvider(this, TransactionViewModelFactory(repository)).get(TransactionViewModel::class.java)
    }

    private fun initializeViews() {
        balanceTextView = findViewById(R.id.balanceTextView)
        transactionsRecyclerView = findViewById(R.id.transactionsRecyclerView)

        findViewById<Button>(R.id.addIncomeButton).setOnClickListener { showAddTransactionDialog(false) }
        findViewById<Button>(R.id.addExpenseButton).setOnClickListener { showAddTransactionDialog(true) }
        findViewById<Button>(R.id.setBudgetButton).setOnClickListener { showSetBudgetDialog() }

        findViewById<FloatingActionButton>(R.id.addTransactionFab).setOnClickListener {
            showAddTransactionDialog(false) // Open the dialog for adding income
        }
    }

    private fun updateBudgetStatus() {
        val remainingBudget = budget - totalExpenses
        val budgetText = if (remainingBudget >= 0) {
            "Budget Status: $%.2f remaining".format(remainingBudget)
        } else {
            "Budget Status: Exceeded by $%.2f".format(Math.abs(remainingBudget))
        }

        findViewById<TextView>(R.id.budgetStatusTextView).text = budgetText
    }

    fun addExpense(amount: Double) {
        totalExpenses += amount
        updateBudgetStatus() // Update budget after adding an expense
    }

    private fun showSetBudgetDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_set_budget, null)

        // Change this line to match the correct ID from the layout file
        val budgetEditText = dialogView.findViewById<EditText>(R.id.editTextBudgetAmount)

        MaterialAlertDialogBuilder(this)
            .setTitle("Set Budget")
            .setView(dialogView)
            .setPositiveButton("Set") { _, _ ->
                val budget = budgetEditText.text.toString().toDoubleOrNull() // Use the updated ID
                if (budget != null) {
                    this.budget = budget // Update the budget value
                    updateBudgetStatus() // Update the budget status display
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }


    private fun setupRecyclerView() {
        transactionAdapter = TransactionAdapter { transaction -> showEditTransactionDialog(transaction) }
        transactionsRecyclerView.layoutManager = LinearLayoutManager(this)
        transactionsRecyclerView.adapter = transactionAdapter
    }

    private fun observeViewModel() {
        viewModel.balance.observe(this) { balance ->
            balanceTextView.text = String.format("Total Balance: $%.2f", balance)
        }

        viewModel.transactions.observe(this) { transactions ->
            Log.d("MainActivity", "Transactions: $transactions")
            transactionAdapter.setTransactions(transactions)
            totalExpenses = transactions.filter { it.isExpense }.sumOf { it.amount } // Calculate total expenses
            updateBudgetStatus() // Update budget after fetching transactions
        }
    }

    private fun showAddTransactionDialog(isExpense: Boolean) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_transaction, null)
        val amountEditText = dialogView.findViewById<TextInputEditText>(R.id.amountEditText)
        val descriptionEditText = dialogView.findViewById<TextInputEditText>(R.id.descriptionEditText)
        val categorySpinner: Spinner = dialogView.findViewById(R.id.categorySpinner)

        // Set up the category spinner
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = adapter

        MaterialAlertDialogBuilder(this)
            .setTitle(if (isExpense) "Add Expense" else "Add Income")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val amount = amountEditText.text.toString().toDoubleOrNull()
                val description = descriptionEditText.text.toString()
                val category = categorySpinner.selectedItem.toString()

                if (amount != null) {
                    viewModel.addTransaction(amount, description, isExpense, category)
                    if (isExpense) addExpense(amount) // Update expenses if it's an expense
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditTransactionDialog(transaction: Transaction) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_transaction, null)
        val amountEditText = dialogView.findViewById<TextInputEditText>(R.id.amountEditText)
        val descriptionEditText = dialogView.findViewById<TextInputEditText>(R.id.descriptionEditText)
        val categorySpinner: Spinner = dialogView.findViewById(R.id.categorySpinner)

        amountEditText.setText(transaction.amount.toString())
        descriptionEditText.setText(transaction.description)

        // Set up the category spinner with existing categories
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = adapter

        // Preselect the category
        val position = categories.indexOf(transaction.category)
        categorySpinner.setSelection(position)

        MaterialAlertDialogBuilder(this)
            .setTitle("Edit Transaction")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val amount = amountEditText.text.toString().toDoubleOrNull()
                val description = descriptionEditText.text.toString()
                val category = categorySpinner.selectedItem.toString()

                if (amount != null) {
                    val updatedTransaction = transaction.copy(amount = amount, description = description, category = category)
                    viewModel.updateTransaction(updatedTransaction)
                    // Update total expenses if the transaction was an expense
                    if (transaction.isExpense) {
                        totalExpenses -= transaction.amount // Remove old amount
                        totalExpenses += amount // Add new amount
                    }
                    updateBudgetStatus() // Update budget after editing
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
