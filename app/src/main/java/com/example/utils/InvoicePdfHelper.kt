package com.example.utils

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.InvoiceEntity
import com.example.data.ProductItem
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object InvoicePdfHelper {

    fun generateInvoicePdf(context: Context, invoice: InvoiceEntity): File? {
        val pdfDocument = PdfDocument()
        
        // A4 specifications (595 x 842 points)
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val paint = Paint()
        val textPaint = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
        }

        // Draw background
        canvas.drawColor(Color.WHITE)

        // Draw top accent line (electric blue)
        paint.color = Color.parseColor("#2D7CFF")
        canvas.drawRect(40f, 40f, 555f, 45f, paint)

        // Title: Seller Aspire Industry Pvt Ltd
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.textSize = 18f
        textPaint.color = Color.parseColor("#0A0E1A")
        canvas.drawText("SELLER ASPIRE INDUSTRY PVT LTD", 40f, 75f, textPaint)

        // Subtitle
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        textPaint.textSize = 9f
        textPaint.color = Color.parseColor("#5A6B82")
        canvas.drawText("Premium Quality Shipping & Logistic Sheet", 40f, 92f, textPaint)

        // Document Heading
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.textSize = 12f
        textPaint.color = Color.parseColor("#2D7CFF")
        canvas.drawText("SHIPPING SHEET / INVOICE", 380f, 75f, textPaint)

        // Divider
        paint.color = Color.parseColor("#E1E6EB")
        canvas.drawLine(40f, 105f, 555f, 105f, paint)

        // Info Block
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        textPaint.textSize = 8.5f
        textPaint.color = Color.parseColor("#1C2434")

        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val dateString = sdf.format(Date(invoice.timestamp))

        canvas.drawText("Invoice No:  ${invoice.invoiceNumber}", 40f, 125f, textPaint)
        canvas.drawText("Date & Time: $dateString", 40f, 140f, textPaint)
        canvas.drawText("Company:     ${invoice.clientName}", 40f, 155f, textPaint)

        canvas.drawText("Origin: New Delhi, India", 380f, 125f, textPaint)
        canvas.drawText("Status: Dispatched / Checked", 380f, 140f, textPaint)

        // Table Header
        val tableTop = 175f
        paint.color = Color.parseColor("#F4F6F9")
        canvas.drawRect(40f, tableTop, 555f, tableTop + 22f, paint)

        paint.color = Color.parseColor("#2D7CFF")
        paint.strokeWidth = 1f
        canvas.drawLine(40f, tableTop, 555f, tableTop, paint)
        canvas.drawLine(40f, tableTop + 22f, 555f, tableTop + 22f, paint)

        // Header Labels
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.textSize = 9.0f
        textPaint.color = Color.parseColor("#0A0E1A")

        canvas.drawText("S.N.", 45f, tableTop + 14f, textPaint)
        canvas.drawText("Product Name", 80f, tableTop + 14f, textPaint)
        canvas.drawText("Qty (Pcs)", 280f, tableTop + 14f, textPaint)
        canvas.drawText("Weight/Pc", 345f, tableTop + 14f, textPaint)
        canvas.drawText("Total Weight", 415f, tableTop + 14f, textPaint)
        canvas.drawText("Color", 495f, tableTop + 14f, textPaint)

        // Table Rows
        var currentY = tableTop + 22f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        textPaint.textSize = 8.5f
        textPaint.color = Color.parseColor("#232E42")

        paint.color = Color.parseColor("#E1E6EB")
        paint.strokeWidth = 0.5f

        for (index in invoice.products.indices) {
            val product = invoice.products[index]
            val rowHeight = 20f

            if (index % 2 == 1) {
                paint.color = Color.parseColor("#FAFCFE")
                canvas.drawRect(40f, currentY, 555f, currentY + rowHeight, paint)
            }

            textPaint.color = Color.parseColor("#1C2434")
            canvas.drawText((index + 1).toString(), 45f, currentY + 13f, textPaint)
            
            val nameToDisplay = if (product.name.length > 25) product.name.substring(0, 23) + ".." else product.name
            canvas.drawText(nameToDisplay, 80f, currentY + 13f, textPaint)
            
            canvas.drawText(product.quantity.toString(), 280f, currentY + 13f, textPaint)
            canvas.drawText(String.format(Locale.US, "%.3f", product.weightPerPiece), 345f, currentY + 13f, textPaint)
            canvas.drawText(String.format(Locale.US, "%.3f", product.totalWeight), 415f, currentY + 13f, textPaint)
            canvas.drawText(product.color, 495f, currentY + 13f, textPaint)

            paint.color = Color.parseColor("#E1E6EB")
            canvas.drawLine(40f, currentY + rowHeight, 555f, currentY + rowHeight, paint)

            currentY += rowHeight
        }

        // Grand Totals block
        val totalBoxTop = currentY + 8f
        paint.color = Color.parseColor("#F4F6F9")
        canvas.drawRect(40f, totalBoxTop, 555f, totalBoxTop + 32f, paint)
        paint.color = Color.parseColor("#2D7CFF")
        paint.strokeWidth = 1f
        canvas.drawLine(40f, totalBoxTop, 555f, totalBoxTop, paint)
        canvas.drawLine(40f, totalBoxTop + 32f, 555f, totalBoxTop + 32f, paint)

        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.textSize = 9.5f
        textPaint.color = Color.parseColor("#0A0E1A")
        canvas.drawText("GRAND TOTALS", 45f, totalBoxTop + 20f, textPaint)

        canvas.drawText("Total Pieces:", 200f, totalBoxTop + 20f, textPaint)
        canvas.drawText(invoice.totalPieces.toString(), 270f, totalBoxTop + 20f, textPaint)

        canvas.drawText("Total Weight:", 360f, totalBoxTop + 20f, textPaint)
        canvas.drawText(String.format(Locale.US, "%.3f PcsWt", invoice.totalWeight), 435f, totalBoxTop + 20f, textPaint)

        // Optional Remarks
        if (invoice.remarks.isNotBlank()) {
            val remarksY = totalBoxTop + 50f
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textPaint.textSize = 8.0f
            textPaint.color = Color.parseColor("#5A6B82")
            canvas.drawText("REMARKS / PACKING NOTES:", 40f, remarksY, textPaint)

            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textPaint.textSize = 8.0f
            textPaint.color = Color.parseColor("#232E42")
            
            val truncatedRemarks = if (invoice.remarks.length > 100) invoice.remarks.substring(0, 97) + "..." else invoice.remarks
            canvas.drawText(truncatedRemarks, 40f, remarksY + 11f, textPaint)
        }

        // Signature Sections
        val signatureY = 745f
        paint.color = Color.parseColor("#CCD4DF")
        paint.strokeWidth = 1f
        canvas.drawLine(60f, signatureY, 190f, signatureY, paint)
        canvas.drawLine(360f, signatureY, 490f, signatureY, paint)

        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        textPaint.textSize = 8.0f
        textPaint.color = Color.parseColor("#5A6B82")
        canvas.drawText("Prepared/Received By", 85f, signatureY + 13f, textPaint)
        canvas.drawText("Authorized Signatory (SAI)", 365f, signatureY + 13f, textPaint)

        // Mini Bottom Tag
        textPaint.textSize = 7.0f
        textPaint.color = Color.parseColor("#9EAEC2")
        canvas.drawText("System Sheet Generated Securely • Seller Aspire Industry Pvt Ltd", 185f, 805f, textPaint)

        pdfDocument.finishPage(page)

        val file = File(context.cacheDir, "Seller_Aspire_${invoice.invoiceNumber}.pdf")
        return try {
            val fos = FileOutputStream(file)
            pdfDocument.writeTo(fos)
            pdfDocument.close()
            fos.close()
            file
        } catch (e: IOException) {
            e.printStackTrace()
            pdfDocument.close()
            null
        }
    }

    fun printInvoice(context: Context, invoiceFile: File) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val jobName = "Seller Aspire Shipping Sheet - ${invoiceFile.nameWithoutExtension}"
        try {
            printManager.print(jobName, object : PrintDocumentAdapter() {
                override fun onLayout(
                    oldAttributes: PrintAttributes?,
                    newAttributes: PrintAttributes?,
                    cancellationSignal: CancellationSignal?,
                    callback: LayoutResultCallback?,
                    extras: Bundle?
                ) {
                    if (cancellationSignal?.isCanceled == true) {
                        callback?.onLayoutCancelled()
                        return
                    }

                    val info = PrintDocumentInfo.Builder(jobName)
                        .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                        .setPageCount(1)
                        .build()
                    callback?.onLayoutFinished(info, true)
                }

                override fun onWrite(
                    pages: Array<out PageRange>?,
                    destination: ParcelFileDescriptor?,
                    cancellationSignal: CancellationSignal?,
                    callback: WriteResultCallback?
                ) {
                    var input: java.io.FileInputStream? = null
                    var output: java.io.FileOutputStream? = null
                    try {
                        input = java.io.FileInputStream(invoiceFile)
                        output = java.io.FileOutputStream(destination?.fileDescriptor)

                        val buf = ByteArray(1024)
                        var bytesRead: Int
                        while (input.read(buf).also { bytesRead = it } > 0) {
                            output.write(buf, 0, bytesRead)
                        }

                        callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                    } catch (e: Exception) {
                        e.printStackTrace()
                        callback?.onWriteFailed(e.message)
                    } finally {
                        try {
                            input?.close()
                            output?.close()
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                }
            }, null)
        } catch (e: Exception) {
            Toast.makeText(context, "Printing failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareInvoicePdf(context: Context, invoiceFile: File) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "com.example.fileprovider",
                invoiceFile
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share Shipping Sheet PDF"))
        } catch (e: Exception) {
            Toast.makeText(context, "Sharing failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
