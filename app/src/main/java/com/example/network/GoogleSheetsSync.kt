package com.example.network

import com.example.data.InvoiceEntity
import com.example.data.ProductItem
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object GoogleSheetsSync {
    private val client = OkHttpClient()
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

    data class SyncPayload(
        val dateTime: String,
        val invoiceNumber: String,
        val clientName: String,
        val products: List<ProductItem>,
        val totalPieces: Int,
        val totalWeight: Double,
        val remarks: String
    )

    suspend fun syncInvoice(webhookUrl: String, invoice: InvoiceEntity): Boolean = withContext(Dispatchers.IO) {
        if (webhookUrl.isBlank()) return@withContext false

        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val dateStr = sdf.format(Date(invoice.timestamp))

            val payload = SyncPayload(
                dateTime = dateStr,
                invoiceNumber = invoice.invoiceNumber,
                clientName = invoice.clientName,
                products = invoice.products,
                totalPieces = invoice.totalPieces,
                totalWeight = invoice.totalWeight,
                remarks = invoice.remarks
            )

            val adapter = moshi.adapter(SyncPayload::class.java)
            val jsonBody = adapter.toJson(payload)

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = jsonBody.toRequestBody(mediaType)

            val request = Request.Builder()
                .url(webhookUrl)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
