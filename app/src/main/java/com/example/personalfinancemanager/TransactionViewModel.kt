package com.example.personalfinancemanager

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import kotlinx.coroutines.flow.map

class TransactionViewModel(private val repository: TransactionRepository) : ViewModel() {

    // Observe all transactions from the repository
    val transactions: LiveData<List<Transaction>> = repository.allTransactions.asLiveData()

    // Calculate balance based on transactions
    val balance: LiveData<Double> = repository.allTransactions.map { transactions ->
        transactions.sumOf { if (it.isExpense) -it.amount else it.amount }
    }.asLiveData()

    // LiveData for tracking the remaining budget per category
    val budgetStatus: MutableLiveData<Pair<String, Double>> = MutableLiveData()

    // Map to hold budgets for each category
    val categoryBudgets: MutableMap<String, Double> = mutableMapOf()

    private fun updateBudgetAfterTransactionChange(category: String) {
        viewModelScope.launch {
            val totalExpenses = repository.getExpensesByCategory(category) ?: 0.0
            val budget = categoryBudgets[category] ?: 0.0
            budgetStatus.value = Pair(category, budget - totalExpenses) // Update remaining budget
        }
    }

    fun addTransaction(amount: Double, description: String, isExpense: Boolean, category: String) {
        val transaction = Transaction(amount = amount, description = description, isExpense = isExpense, category = category, date = System.currentTimeMillis())
        viewModelScope.launch {
            repository.insert(transaction)
            // Update remaining budget
            updateBudgetAfterTransactionChange(category)
        }
    }

    fun updateTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.update(transaction)
            // Update remaining budget
            updateBudgetAfterTransactionChange(transaction.category)
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.delete(transaction)
            // Update remaining budget
            updateBudgetAfterTransactionChange(transaction.category)
        }
    }

    // Check the remaining budget for a category
    fun checkBudgetStatus(category: String, budget: Double) {
        viewModelScope.launch {
            val totalExpenses = repository.getExpensesByCategory(category) ?: 0.0
            val remainingBudget = budget - totalExpenses
            budgetStatus.value = Pair(category, remainingBudget)
        }
    }

    // Retrieve transactions by category (if needed)
    fun getTransactionsByCategory(category: String): LiveData<List<Transaction>> {
        val transactionsByCategory = MutableLiveData<List<Transaction>>()
        viewModelScope.launch {
            transactionsByCategory.value = repository.getTransactionsByCategory(category)
        }
        return transactionsByCategory
    }

    // Function to set budget for a category
    fun setBudgetForCategory(category: String, budget: Double) {
        categoryBudgets[category] = budget
    }

    // Function to get the remaining budget for a category
    fun getRemainingBudgetForCategory(category: String): LiveData<Double> {
        val remainingBudgetLiveData = MutableLiveData<Double>()
        viewModelScope.launch {
            val totalExpenses = repository.getExpensesByCategory(category) ?: 0.0
            val budget = categoryBudgets[category] ?: 0.0
            remainingBudgetLiveData.value = budget - totalExpenses
        }
        return remainingBudgetLiveData
    }
}
