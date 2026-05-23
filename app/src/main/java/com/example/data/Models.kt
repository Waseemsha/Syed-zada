package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

@JsonClass(generateAdapter = true)
data class ProductItem(
    val name: String,
    val quantity: Int,
    val weightPerPiece: Double,
    val color: String
) {
    val totalWeight: Double
        get() = quantity * weightPerPiece
}

class InvoiceTypeConverters {
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val type = Types.newParameterizedType(List::class.java, ProductItem::class.java)
    private val adapter = moshi.adapter<List<ProductItem>>(type)

    @TypeConverter
    fun stringToProductItems(value: String): List<ProductItem> {
        return try {
            adapter.fromJson(value) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun productItemsToString(list: List<ProductItem>): String {
        return adapter.toJson(list)
    }
}

@Entity(tableName = "invoices")
@TypeConverters(InvoiceTypeConverters::class)
data class InvoiceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val invoiceNumber: String,
    val clientName: String,
    val products: List<ProductItem>,
    val totalPieces: Int,
    val totalWeight: Double,
    val remarks: String = ""
)
