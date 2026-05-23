package com.example.ui

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.InvoiceDatabase
import com.example.data.InvoiceEntity
import com.example.data.ProductItem
import com.example.network.GoogleSheetsSync
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class InvoiceViewModel(application: Application) : AndroidViewModel(application) {

    private val db = InvoiceDatabase.getDatabase(application)
    private val dao = db.invoiceDao()

    private val sharedPrefs = application.getSharedPreferences("seller_aspire_prefs", Context.MODE_PRIVATE)

    // Current Webhook URL for Google Sheets Sync
    var googleSheetsUrl by mutableStateOf(sharedPrefs.getString("webhook_url", "") ?: "")
        private set

    // Auto-save setting
    var autoSaveToSheets by mutableStateOf(sharedPrefs.getBoolean("auto_save_sheets", false))
        private set

    // Saved Billing Name
    var savedBillingName by mutableStateOf(sharedPrefs.getString("billing_name", "Seller Aspire Industry Pvt Ltd") ?: "Seller Aspire Industry Pvt Ltd")
        private set

    // Active List of products being drafted
    val draftProducts = mutableStateListOf<ProductItem>()

    // Draft product input fields
    var inputProductName by mutableStateOf("")
    var inputQuantity by mutableStateOf("")
    var inputWeight by mutableStateOf("")
    var inputColor by mutableStateOf("")

    // Form warnings
    var formError by mutableStateOf("")

    // Active Invoice Remarks/Notes
    var inputRemarks by mutableStateOf("")

    // Active Invoice under Preview or Draft
    private val _currentActiveInvoice = MutableStateFlow<InvoiceEntity?>(null)
    val currentActiveInvoice: StateFlow<InvoiceEntity?> = _currentActiveInvoice.asStateFlow()

    // Room Database History
    private val _invoiceHistory = MutableStateFlow<List<InvoiceEntity>>(emptyList())
    val invoiceHistory: StateFlow<List<InvoiceEntity>> = _invoiceHistory.asStateFlow()

    // Operational/Loading States
    var isSaving by mutableStateOf(false)
    var syncStatusMessage by mutableStateOf("")
    var isSyncing by mutableStateOf(false)

    // Editing mode state (index of product item being edited, -1 if composing new)
    var editingProductIndex by mutableStateOf(-1)

    init {
        loadHistory()
    }

    // Load saved sheets from database
    fun loadHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            dao.getAllInvoices().collect { list ->
                _invoiceHistory.value = list
            }
        }
    }

    // Persistent config savers
    fun updateGoogleSheetsUrl(url: String) {
        googleSheetsUrl = url
        sharedPrefs.edit().putString("webhook_url", url).apply()
    }

    fun updateAutoSaveToSheets(enabled: Boolean) {
        autoSaveToSheets = enabled
        sharedPrefs.edit().putBoolean("auto_save_sheets", enabled).apply()
    }

    fun updateBillingName(name: String) {
        val finalName = name.ifBlank { "Seller Aspire Industry Pvt Ltd" }
        savedBillingName = finalName
        sharedPrefs.edit().putString("billing_name", finalName).apply()
    }

    // Real-Time Draft calculations
    val currentItemTotalWeight: Double
        get() {
            val q = inputQuantity.toIntOrNull() ?: 0
            val w = inputWeight.toDoubleOrNull() ?: 0.0
            return q * w
        }

    val draftTotalPieces: Int
        get() = draftProducts.sumOf { it.quantity }

    val draftTotalWeight: Double
        get() = draftProducts.sumOf { it.totalWeight }

    // Draft Operations
    fun addDraftItem() {
        if (inputProductName.isBlank()) {
            formError = "Product Name is required"
            return
        }
        val qty = inputQuantity.toIntOrNull()
        if (qty == null || qty <= 0) {
            formError = "Quantity must be a valid number greater than 0"
            return
        }
        val wt = inputWeight.toDoubleOrNull()
        if (wt == null || wt <= 0.0) {
            formError = "Weight per Piece must be a valid number greater than 0"
            return
        }
        if (inputColor.isBlank()) {
            formError = "Product Color or Code is required"
            return
        }

        val newItem = ProductItem(
            name = inputProductName.trim(),
            quantity = qty,
            weightPerPiece = wt,
            color = inputColor.trim()
        )

        if (editingProductIndex >= 0 && editingProductIndex < draftProducts.size) {
            // Update existing row
            draftProducts[editingProductIndex] = newItem
            editingProductIndex = -1
        } else {
            // Append new row
            draftProducts.add(newItem)
        }

        // Reset inputs easily but keep Color/Name as templates to speed up consecutive entries!
        inputProductName = ""
        inputQuantity = ""
        inputWeight = ""
        formError = ""
    }

    fun deleteDraftItem(index: Int) {
        if (index >= 0 && index < draftProducts.size) {
            draftProducts.removeAt(index)
            if (editingProductIndex == index) {
                editingProductIndex = -1
            }
        }
    }

    fun startEditingDraftItem(index: Int) {
        if (index >= 0 && index < draftProducts.size) {
            val item = draftProducts[index]
            inputProductName = item.name
            inputQuantity = item.quantity.toString()
            inputWeight = item.weightPerPiece.toString()
            inputColor = item.color
            editingProductIndex = index
            formError = ""
        }
    }

    fun cancelEditing() {
        editingProductIndex = -1
        inputProductName = ""
        inputQuantity = ""
        inputWeight = ""
        inputColor = ""
        formError = ""
    }

    fun clearDraftSheet() {
        draftProducts.clear()
        inputRemarks = ""
        _currentActiveInvoice.value = null
        editingProductIndex = -1
        formError = ""
    }

    // Saves current draft to Room Database as an Invoice
    fun compileAndSaveInvoice(callback: (InvoiceEntity) -> Unit = {}) {
        if (draftProducts.isEmpty()) {
            formError = "Cannot compile empty sheet. Add at least one item."
            return
        }

        isSaving = true
        formError = ""

        viewModelScope.launch(Dispatchers.IO) {
            val ts = System.currentTimeMillis()
            val formatId = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault()).format(Date(ts))
            val invoiceNum = "SAI-$formatId"

            val invoice = InvoiceEntity(
                timestamp = ts,
                invoiceNumber = invoiceNum,
                clientName = savedBillingName,
                products = draftProducts.toList(),
                totalPieces = draftTotalPieces,
                totalWeight = draftTotalWeight,
                remarks = inputRemarks.trim()
            )

            val newId = dao.insertInvoice(invoice)
            val savedInvoice = invoice.copy(id = newId)

            _currentActiveInvoice.value = savedInvoice
            loadHistory()
            isSaving = false

            // Auto Sync Trigger
            if (autoSaveToSheets && googleSheetsUrl.isNotBlank()) {
                syncToGoogleSheets(savedInvoice)
            }

            launch(Dispatchers.Main) {
                callback(savedInvoice)
            }
        }
    }

    // Selects an existing invoice from history
    fun selectActiveInvoice(invoice: InvoiceEntity) {
        _currentActiveInvoice.value = invoice
    }

    // Trigger explicit sync to Google Sheets for any invoice
    fun syncToGoogleSheets(invoice: InvoiceEntity) {
        if (googleSheetsUrl.isBlank()) {
            syncStatusMessage = "Sync failed: Go to settings to set your Google Sheets Webhook URL."
            return
        }

        isSyncing = true
        syncStatusMessage = "Syncing with Google Sheets..."

        viewModelScope.launch {
            val success = GoogleSheetsSync.syncInvoice(googleSheetsUrl, invoice)
            isSyncing = false
            syncStatusMessage = if (success) {
                "Google Sheets synced successfully! Rows appended."
            } else {
                "Sync connection failed. Check your Webhook URL or network."
            }
        }
    }

    fun deleteInvoiceFromHistory(invoice: InvoiceEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteInvoice(invoice)
            if (_currentActiveInvoice.value?.id == invoice.id) {
                _currentActiveInvoice.value = null
            }
            loadHistory()
        }
    }

    fun clearFeedbackMessage() {
        syncStatusMessage = ""
    }
}
