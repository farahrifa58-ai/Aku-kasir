package com.example.util

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import androidx.core.content.FileProvider
import com.example.R
import com.example.data.model.DiscountType
import com.example.data.model.PosTransaction
import com.example.data.model.StoreSettings
import com.example.data.model.TaxType
import com.example.ui.theme.AppStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID

enum class PaperWidth(val mm: Int, val charsPerLine: Int) {
    MM_58(58, 32),
    MM_80(80, 48)
}

data class PairedPrinter(
    val name: String,
    val address: String,
    val isConnected: Boolean = false
)

object ReceiptHelper {
    private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    fun buildPlainTextReceipt(
        transaction: PosTransaction,
        store: StoreSettings,
        strings: AppStrings,
        language: String,
        charsPerLine: Int = 32
    ): String {
        val line = "-".repeat(charsPerLine)
        val doubleLine = "=".repeat(charsPerLine)
        val sb = StringBuilder()

        fun center(text: String): String {
            val clean = text.take(charsPerLine)
            val pad = ((charsPerLine - clean.length) / 2).coerceAtLeast(0)
            return " ".repeat(pad) + clean
        }

        fun leftRight(left: String, right: String): String {
            val maxLeft = (charsPerLine - right.length - 1).coerceAtLeast(4)
            val l = left.take(maxLeft)
            val spaces = (charsPerLine - l.length - right.length).coerceAtLeast(1)
            return l + " ".repeat(spaces) + right
        }

        sb.appendLine(center(store.storeName.ifBlank { "ADA KASIR" }))
        if (store.showReceiptStoreAddress && store.address.isNotBlank()) {
            sb.appendLine(center(store.address))
        }
        if (store.showReceiptStorePhone && store.phone.isNotBlank()) {
            sb.appendLine(center("Telp: ${store.phone}"))
        }
        sb.appendLine(doubleLine)
        sb.appendLine(leftRight(strings.receiptNumber, transaction.transactionNumber))
        sb.appendLine(leftRight(strings.receiptDate, PosFormatters.formatDateTime(transaction.transactionMillis, language)))
        if (store.showReceiptCashierName) {
            val cashier = transaction.cashierName.ifBlank { store.ownerName }.ifBlank { "Kasir" }
            sb.appendLine(leftRight(strings.receiptCashierLabel, cashier))
        }
        if (store.showReceiptCustomerName && transaction.customerName.isNotBlank()) {
            sb.appendLine(leftRight(strings.receiptCustomer, transaction.customerName))
        }
        sb.appendLine(leftRight(strings.paymentMethodLabel, PosFormatters.paymentMethodLabel(transaction.paymentMethod, strings)))
        sb.appendLine(line)

        transaction.items.forEach { item ->
            sb.appendLine(item.productName.take(charsPerLine))
            val qtyPrice = "${PosFormatters.formatQuantity(item.quantity, item.unit)} x ${PosFormatters.formatRupiah(item.unitPrice)}"
            val sub = PosFormatters.formatRupiah(item.subtotal)
            sb.appendLine(leftRight("  $qtyPrice", sub))
        }

        sb.appendLine(line)
        sb.appendLine(leftRight(strings.subtotalLabel, PosFormatters.formatRupiah(transaction.subtotal)))
        if (transaction.discountAmount > 0.0) {
            val discTag = if (transaction.discountType == DiscountType.PERCENT.code) {
                "${strings.discountLabel} (${PosFormatters.formatQuantity(transaction.discountValue)}%)"
            } else {
                strings.discountLabel
            }
            sb.appendLine(leftRight(discTag, "-${PosFormatters.formatRupiah(transaction.discountAmount)}"))
        }
        if (transaction.taxAmount > 0.0) {
            val taxTag = if (transaction.taxType == TaxType.AUTO.code) {
                "${strings.taxLabel} (${PosFormatters.formatQuantity(transaction.taxPercent)}%)"
            } else {
                strings.taxLabel
            }
            sb.appendLine(leftRight(taxTag, PosFormatters.formatRupiah(transaction.taxAmount)))
        }
        sb.appendLine(doubleLine)
        sb.appendLine(leftRight(strings.grandTotalLabel.uppercase(), PosFormatters.formatRupiah(transaction.totalAmount)))
        sb.appendLine(leftRight(strings.amountReceivedLabel, PosFormatters.formatRupiah(transaction.amountPaid)))
        sb.appendLine(leftRight(strings.changeLabel, PosFormatters.formatRupiah(transaction.changeAmount)))
        sb.appendLine(line)
        val footerText = store.receiptFooterMessage.trim().ifBlank { strings.receiptCashierNote }
        footerText.lines().filter { it.isNotBlank() }.forEach { footerLine ->
            sb.appendLine(center(footerLine.trim()))
        }
        sb.appendLine(center("ADA KASIR - Rafline Dev"))
        return sb.toString()
    }

    fun generateReceiptPdf(
        context: Context,
        transaction: PosTransaction,
        store: StoreSettings,
        strings: AppStrings,
        language: String,
        paperWidth: PaperWidth = PaperWidth.MM_58
    ): File {
        val pageWidth = if (paperWidth == PaperWidth.MM_58) 220 else 300
        val receiptText = buildPlainTextReceipt(transaction, store, strings, language, paperWidth.charsPerLine)
        val lines = receiptText.lines()
        val lineHeight = 14

        val logoBitmap: Bitmap? = if (store.showReceiptLogo) {
            ImageUtils.decodeDataUriToBitmap(store.photoData)
                ?: runCatching { BitmapFactory.decodeResource(context.resources, R.drawable.ada_kasir_logo) }.getOrNull()
        } else {
            null
        }
        val logoHeight = if (logoBitmap != null) 46f else 0f
        val pageHeight = (lines.size * lineHeight + logoHeight.toInt() + 64).coerceAtLeast(300)

        val pdf = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = pdf.startPage(pageInfo)
        val canvas: Canvas = page.canvas
        canvas.drawColor(Color.WHITE)

        val paint = Paint().apply {
            color = Color.BLACK
            textSize = if (paperWidth == PaperWidth.MM_58) 9.5f else 9.0f
            typeface = Typeface.MONOSPACE
            isAntiAlias = true
        }
        val measuredLineWidth = paint.measureText("=".repeat(paperWidth.charsPerLine))
        val textStartX = ((pageWidth - measuredLineWidth) / 2f).coerceAtLeast(8f)

        var y = 20f
        if (logoBitmap != null) {
            val logoSize = 38f
            val aspect = if (logoBitmap.height > 0) logoBitmap.width.toFloat() / logoBitmap.height.toFloat() else 1f
            val drawW = if (aspect >= 1f) logoSize else logoSize * aspect
            val drawH = if (aspect >= 1f) logoSize / aspect else logoSize
            val left = (pageWidth - drawW) / 2f
            canvas.drawBitmap(logoBitmap, null, RectF(left, y, left + drawW, y + drawH), null)
            y += drawH + 14f
        } else {
            y = 26f
        }

        lines.forEach { textLine ->
            canvas.drawText(textLine, textStartX, y, paint)
            y += lineHeight
        }

        pdf.finishPage(page)

        val dir = File(context.cacheDir, "receipts").apply { mkdirs() }
        val file = File(dir, "Struk_${transaction.transactionNumber}.pdf")
        FileOutputStream(file).use { out ->
            pdf.writeTo(out)
        }
        pdf.close()
        return file
    }

    fun shareReceiptWhatsApp(
        context: Context,
        transaction: PosTransaction,
        store: StoreSettings,
        strings: AppStrings,
        language: String
    ) {
        val receiptText = buildPlainTextReceipt(transaction, store, strings, language, 32)
        val waIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "```$receiptText```")
            setPackage("com.whatsapp")
        }
        try {
            context.startActivity(waIntent)
        } catch (_: Exception) {
            val fallback = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, receiptText)
            }
            context.startActivity(Intent.createChooser(fallback, strings.shareReceipt))
        }
    }

    fun sharePdfFile(context: Context, pdfFile: File, title: String) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, title))
    }

    fun printViaAndroidSystem(context: Context, pdfFile: File, jobName: String) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val adapter = object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback,
                extras: Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback.onLayoutCancelled()
                    return
                }
                val info = PrintDocumentInfo.Builder(pdfFile.name)
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(1)
                    .build()
                callback.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pages: Array<out PageRange>?,
                destination: ParcelFileDescriptor,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback
            ) {
                try {
                    FileInputStream(pdfFile).use { input ->
                        FileOutputStream(destination.fileDescriptor).use { output ->
                            input.copyTo(output)
                        }
                    }
                    callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback.onWriteFailed(e.message)
                }
            }
        }
        printManager.print(jobName, adapter, PrintAttributes.Builder().build())
    }

    @SuppressLint("MissingPermission")
    fun getPairedBluetoothPrinters(context: Context): List<PairedPrinter> {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter: BluetoothAdapter = manager?.adapter ?: return emptyList()
        if (!adapter.isEnabled) return emptyList()
        return try {
            adapter.bondedDevices.orEmpty().map { dev ->
                val connected = try {
                    val m = dev.javaClass.getMethod("isConnected")
                    (m.invoke(dev) as? Boolean) ?: false
                } catch (_: Exception) {
                    false
                }
                PairedPrinter(
                    name = dev.name ?: "Bluetooth Printer",
                    address = dev.address,
                    isConnected = connected
                )
            }.sortedWith(compareByDescending<PairedPrinter> { it.isConnected }.thenBy { it.name.lowercase() })
        } catch (_: SecurityException) {
            emptyList()
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun printTestReceipt(
        context: Context,
        printerAddress: String,
        store: StoreSettings,
        paperWidth: PaperWidth
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val adapter = manager?.adapter ?: throw IllegalStateException("Bluetooth unavailable")
            if (!adapter.isEnabled) throw IllegalStateException("Bluetooth is turned off")
            val device: BluetoothDevice = adapter.getRemoteDevice(printerAddress)
            val socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_SCAN) == android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                try { adapter.cancelDiscovery() } catch (_: Exception) {}
            }
            val line = "-".repeat(paperWidth.charsPerLine)
            val sampleText = buildString {
                appendLine(store.storeName.ifBlank { "ADA KASIR" }.take(paperWidth.charsPerLine))
                appendLine(line)
                appendLine("TES CETAK PRINTER ${paperWidth.mm}MM")
                appendLine("Status: Terhubung & Siap")
                appendLine("ADA KASIR - Rafline Dev")
                appendLine(line)
                appendLine("\n\n")
            }
            socket.use { s ->
                s.connect()
                val out = s.outputStream
                out.write(byteArrayOf(0x1B, 0x40))
                out.write(sampleText.toByteArray(Charsets.UTF_8))
                out.write(byteArrayOf(0x1D, 0x56, 0x41, 0x10))
                out.flush()
            }
            Unit
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun printBluetoothEscPos(
        context: Context,
        printerAddress: String,
        transaction: PosTransaction,
        store: StoreSettings,
        strings: AppStrings,
        language: String,
        paperWidth: PaperWidth
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val adapter = manager?.adapter ?: throw IllegalStateException("Bluetooth unavailable")
            if (!adapter.isEnabled) throw IllegalStateException("Bluetooth is turned off")
            val device: BluetoothDevice = adapter.getRemoteDevice(printerAddress)
            val socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_SCAN) == android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                try { adapter.cancelDiscovery() } catch (_: Exception) {}
            }
            socket.use { s ->
                s.connect()
                val out = s.outputStream
                // ESC/POS init + text + feed + partial cut
                val init = byteArrayOf(0x1B, 0x40)
                val text = buildPlainTextReceipt(
                    transaction = transaction,
                    store = store,
                    strings = strings,
                    language = language,
                    charsPerLine = paperWidth.charsPerLine
                ) + "\n\n\n"
                val cut = byteArrayOf(0x1D, 0x56, 0x41, 0x10)
                out.write(init)
                out.write(text.toByteArray(Charsets.UTF_8))
                out.write(cut)
                out.flush()
            }
            Unit
        }
    }
}
