package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf

@Immutable
interface AppStrings {
    val appName: String
    val developerBy: String

    // Auth
    val loginTitle: String
    val loginSubtitle: String
    val loginButton: String
    val loginSyncNote: String
    val loginCancelled: String
    val loginSavedSessionNote: String
    val loginErrorFallback: String
    val loginSigningIn: String

    // Main Menus
    val menuTitle: String
    val menuDashboard: String
    val menuCashier: String
    val menuProducts: String
    val menuSalesReport: String
    val menuExpenses: String
    val menuHistory: String
    val menuSettings: String

    // Greetings & Common
    val greetingMorning: String
    val greetingAfternoon: String
    val greetingEvening: String
    val greetingNight: String
    val searchPlaceholder: String
    val cancel: String
    val confirm: String
    val save: String
    val delete: String
    val edit: String
    val close: String
    val all: String
    val loading: String
    val retry: String

    // Dashboard
    val todaySales: String
    val todayTransactions: String
    val salesTrendTitle: String
    val salesTrendSubtitle: String
    val periodToday: String
    val period7Days: String
    val period30Days: String
    val periodThisMonth: String
    val topProductsTitle: String
    val soldLabel: String
    val noSalesDataYet: String
    val recentTransactionsTitle: String
    val viewAllHistory: String
    val noTransactionsYet: String
    val stockAlertsTitle: String
    val allStockHealthy: String
    val lowStockBadge: String
    val outOfStockBadge: String

    // Cashier
    val searchProductHint: String
    val allProducts: String
    val cartTitle: String
    val emptyCartHint: String
    val addToCart: String
    val selectQuantityTitle: String
    val quickAmountLabel: String
    val manualDecimalLabel: String
    val estimatedSubtotal: String
    val subtotalLabel: String
    val customerNameLabel: String
    val customerNameHint: String
    val discountLabel: String
    val discountNone: String
    val discountPercent: String
    val discountAmount: String
    val taxLabel: String
    val taxNone: String
    val taxAuto: String
    val taxManual: String
    val paymentMethodLabel: String
    val methodCash: String
    val methodBankTransfer: String
    val methodQris: String
    val methodOther: String
    val amountReceivedLabel: String
    val exactCashButton: String
    val changeLabel: String
    val grandTotalLabel: String
    val completeTransactionButton: String
    val insufficientStockError: String
    val insufficientPaymentError: String
    val transactionSuccessMsg: String
    val viewCartButton: String

    // Receipt & Print
    val receiptTitle: String
    val receiptNumber: String
    val receiptDate: String
    val receiptCustomer: String
    val receiptCashierNote: String
    val printReceipt: String
    val savePdf: String
    val shareWhatsApp: String
    val printerWidth58: String
    val printerWidth80: String
    val selectPrinterTitle: String
    val noPairedPrinters: String
    val printSuccessMsg: String
    val printFailedMsg: String
    val pdfSavedMsg: String
    val bluetoothPermissionNeeded: String
    val systemPrintFallback: String

    // Products
    val addProduct: String
    val editProduct: String
    val productNameLabel: String
    val productSkuLabel: String
    val productCategoryLabel: String
    val productUnitLabel: String
    val productCostPriceLabel: String
    val productSellPriceLabel: String
    val productStockLabel: String
    val productMinStockLabel: String
    val productDescLabel: String
    val productPhotoLabel: String
    val pickFromGallery: String
    val takePhoto: String
    val manageCategories: String
    val addCategory: String
    val editCategory: String
    val categoryNameLabel: String
    val noProductsFound: String
    val deleteProductConfirm: String
    val stockWarningLeft: String

    // History
    val searchTrxHint: String
    val filterDateLabel: String
    val filterMethodLabel: String
    val trxDetailTitle: String
    val purchasedItemsLabel: String
    val reprintReceipt: String
    val shareReceipt: String
    val cancelTransaction: String
    val refundTransaction: String
    val cancelConfirmTitle: String
    val refundConfirmTitle: String
    val cancelRefundWarning: String
    val reasonOptionalLabel: String
    val statusCompleted: String
    val statusCancelled: String
    val statusRefunded: String

    // Settings
    val storeProfileSection: String
    val storeNameLabel: String
    val ownerNameLabel: String
    val phoneLabel: String
    val addressLabel: String
    val defaultTaxPercentLabel: String
    val saveChanges: String
    val profileSavedMsg: String
    val themeSection: String
    val lightTheme: String
    val darkTheme: String
    val languageSection: String
    val languageIndonesian: String
    val languageEnglish: String
    val backupRestoreSection: String
    val manualBackupButton: String
    val restoreBackupButton: String
    val lastBackupLabel: String
    val neverBackedUp: String
    val backupInProgress: String
    val backupSuccessMsg: String
    val restoreConfirmTitle: String
    val restoreConfirmBody: String
    val restoreSuccessMsg: String
    val restoreInvalidMsg: String
    val aboutSection: String
    val versionLabel: String
    val aboutDescription: String
    val copyrightText: String
    val googleAccountSection: String
    val syncedWithGoogle: String
    val signOutButton: String

    // Sales Report
    val reportFilterDate: String
    val reportFilterMonth: String
    val reportFilterYear: String
    val reportTotalSales: String
    val reportOmset: String
    val reportHpp: String
    val reportGrossProfit: String
    val reportExpensesSummary: String
    val reportNetProfit: String
    val reportPaymentBreakdown: String
    val reportSelectDate: String

    // Expenses
    val addExpense: String
    val editExpense: String
    val expenseTitleLabel: String
    val expenseCategoryLabel: String
    val expenseAmountLabel: String
    val expenseNotesLabel: String
    val expenseDateLabel: String
    val todayExpensesLabel: String
    val monthExpensesLabel: String
    val noExpensesYet: String
    val deleteExpenseConfirm: String
    val expenseSavedMsg: String

    // Settings Grouped Sections & Printer Management
    val settingsConfigSection: String
    val settingsHardwareSection: String
    val settingsDataSection: String
    val settingsAccountSection: String
    val storeProfileSubtitle: String
    val appearanceLanguageTitle: String
    val appearanceLanguageSubtitle: String
    val printerSettingsTitle: String
    val printerSettingsSubtitle: String
    val backupRestoreSubtitle: String
    val connectedPrintersTitle: String
    val selectedPrinterLabel: String
    val noPrinterSelected: String
    val defaultPaperWidthLabel: String
    val refreshPrintersButton: String
    val testPrintButton: String
    val selectPrinterButton: String
    val disconnectPrinterButton: String
    val printerConnectedBadge: String
    val printerPairedBadge: String
    val printerSelectedBadge: String
    val openBluetoothSettingsButton: String

    // Splash
    val splashByDeveloper: String

    // Receipt Customization
    val receiptCashierLabel: String
    val receiptCustomTitle: String
    val receiptCustomSubtitle: String
    val receiptPreviewLabel: String
    val receiptShowLogoLabel: String
    val receiptShowCashierLabel: String
    val receiptShowPhoneLabel: String
    val receiptShowAddressLabel: String
    val receiptShowCustomerLabel: String
    val receiptFooterMessageLabel: String
    val receiptFooterMessageHint: String
    val receiptCustomSavedMsg: String

    // User Management / Monitor Mode
    val userManageTitle: String
    val userManageSubtitle: String
    val userRoleInfoTitle: String
    val userRoleInfoBody: String
    val userModeCashierLabel: String
    val userModeMonitorLabel: String
    val userModeCashierDesc: String
    val userModeMonitorDesc: String
    val monitorEmailListLabel: String
    val monitorEmailInputHint: String
    val addMonitorEmailButton: String
    val noMonitorEmailsYet: String
    val monitorStoresAvailableLabel: String
    val monitorNoStoresLinked: String
    val connectStoreButton: String
    val connectedBadge: String
    val monitorBannerTitle: String
    val monitorReadOnlyError: String
}

object IndonesianStrings : AppStrings {
    override val appName = "ADA KASIR"
    override val developerBy = "Rafline Dev"
    override val loginTitle = "ADA KASIR"
    override val loginSubtitle = "Aplikasi Point of Sale modern dengan sinkronisasi cloud otomatis di seluruh perangkat Anda."
    override val loginButton = "Masuk dengan Google"
    override val loginSyncNote = "Cukup login dengan satu akun Google. Semua data toko, produk, stok, dan transaksi tersinkronisasi otomatis."
    override val loginCancelled = "Login Google dibatalkan. Silakan pilih akun untuk melanjutkan."
    override val loginSavedSessionNote = "Sesi akun tersimpan (%s). Tekan Masuk dengan Google untuk membuka Dashboard."
    override val loginErrorFallback = "Gagal masuk dengan Google. Periksa koneksi atau akun Google Anda, lalu coba lagi."
    override val loginSigningIn = "Memproses masuk..."
    override val menuTitle = "Menu Utama"
    override val menuDashboard = "Dashboard"
    override val menuCashier = "Kasir"
    override val menuProducts = "Produk"
    override val menuSalesReport = "Laporan Penjualan"
    override val menuExpenses = "Pengeluaran"
    override val menuHistory = "Riwayat"
    override val menuSettings = "Pengaturan"
    override val greetingMorning = "Selamat Pagi"
    override val greetingAfternoon = "Selamat Siang"
    override val greetingEvening = "Selamat Sore"
    override val greetingNight = "Selamat Malam"
    override val searchPlaceholder = "Cari..."
    override val cancel = "Batal"
    override val confirm = "Konfirmasi"
    override val save = "Simpan"
    override val delete = "Hapus"
    override val edit = "Ubah"
    override val close = "Tutup"
    override val all = "Semua"
    override val loading = "Memuat..."
    override val retry = "Coba Lagi"
    override val todaySales = "Total Penjualan Hari Ini"
    override val todayTransactions = "Jumlah Transaksi Hari Ini"
    override val salesTrendTitle = "Grafik Tren Penjualan"
    override val salesTrendSubtitle = "Aktivitas Kasir & Jam Sibuk"
    override val periodToday = "Hari ini"
    override val period7Days = "7 hari"
    override val period30Days = "30 hari"
    override val periodThisMonth = "Bulan ini"
    override val topProductsTitle = "Produk Terlaris"
    override val soldLabel = "terjual"
    override val noSalesDataYet = "Belum ada transaksi selesai pada periode ini."
    override val recentTransactionsTitle = "Transaksi Terbaru"
    override val viewAllHistory = "Lihat Semua Riwayat"
    override val noTransactionsYet = "Belum ada transaksi tercatat."
    override val stockAlertsTitle = "Notifikasi Stok"
    override val allStockHealthy = "Semua stok produk dalam kondisi aman."
    override val lowStockBadge = "Stok Menipis"
    override val outOfStockBadge = "Habis"
    override val searchProductHint = "Cari nama atau kode SKU produk..."
    override val allProducts = "Semua produk"
    override val cartTitle = "Keranjang"
    override val emptyCartHint = "Ketuk produk untuk menambahkan ke keranjang"
    override val addToCart = "Tambah ke Keranjang"
    override val selectQuantityTitle = "Jumlah Pembelian"
    override val quickAmountLabel = "Pilihan Cepat"
    override val manualDecimalLabel = "Input Manual (misal: 0.75, 1.25)"
    override val estimatedSubtotal = "Subtotal Barang"
    override val subtotalLabel = "Subtotal"
    override val customerNameLabel = "Nama Pelanggan (Opsional)"
    override val customerNameHint = "Boleh dikosongkan"
    override val discountLabel = "Diskon"
    override val discountNone = "Tanpa Diskon"
    override val discountPercent = "Persentase (%)"
    override val discountAmount = "Nominal (Rp)"
    override val taxLabel = "PPN"
    override val taxNone = "Tanpa PPN"
    override val taxAuto = "PPN Otomatis"
    override val taxManual = "PPN Manual (Rp)"
    override val paymentMethodLabel = "Metode Pembayaran"
    override val methodCash = "Tunai"
    override val methodBankTransfer = "Transfer Bank"
    override val methodQris = "QRIS"
    override val methodOther = "Metode Lainnya"
    override val amountReceivedLabel = "Uang Diterima"
    override val exactCashButton = "Uang Pas"
    override val changeLabel = "Kembalian"
    override val grandTotalLabel = "Total Bayar"
    override val completeTransactionButton = "Selesaikan Transaksi"
    override val insufficientStockError = "Stok produk tidak mencukupi."
    override val insufficientPaymentError = "Uang diterima kurang dari total pembayaran."
    override val transactionSuccessMsg = "Transaksi berhasil disimpan!"
    override val viewCartButton = "Lihat Keranjang"
    override val receiptTitle = "Struk Transaksi"
    override val receiptNumber = "No. Transaksi"
    override val receiptDate = "Waktu"
    override val receiptCustomer = "Pelanggan"
    override val receiptCashierNote = "Terima kasih telah berbelanja!"
    override val printReceipt = "Cetak Struk"
    override val savePdf = "Simpan PDF"
    override val shareWhatsApp = "Bagikan WA"
    override val printerWidth58 = "Thermal 58 mm"
    override val printerWidth80 = "Thermal 80 mm"
    override val selectPrinterTitle = "Pilih Printer Bluetooth"
    override val noPairedPrinters = "Tidak ada printer Bluetooth terpasang. Gunakan cetak sistem PDF atau pasangkan printer di pengaturan Bluetooth HP."
    override val printSuccessMsg = "Struk berhasil dikirim ke printer."
    override val printFailedMsg = "Gagal mencetak ke printer Bluetooth."
    override val pdfSavedMsg = "Struk PDF berhasil disimpan."
    override val bluetoothPermissionNeeded = "Izin Bluetooth diperlukan untuk mencetak ke printer thermal."
    override val systemPrintFallback = "Cetak via Sistem / PDF"
    override val addProduct = "Tambah Produk"
    override val editProduct = "Edit Produk"
    override val productNameLabel = "Nama Produk"
    override val productSkuLabel = "Kode Produk / SKU"
    override val productCategoryLabel = "Kategori"
    override val productUnitLabel = "Satuan"
    override val productCostPriceLabel = "Harga Modal (Rp)"
    override val productSellPriceLabel = "Harga Jual (Rp)"
    override val productStockLabel = "Jumlah Stok"
    override val productMinStockLabel = "Stok Minimum (Opsional)"
    override val productDescLabel = "Deskripsi Produk (Opsional)"
    override val productPhotoLabel = "Foto Produk"
    override val pickFromGallery = "Pilih dari Galeri"
    override val takePhoto = "Ambil Foto Kamera"
    override val manageCategories = "Kelola Kategori"
    override val addCategory = "Tambah Kategori"
    override val editCategory = "Ubah Kategori"
    override val categoryNameLabel = "Nama Kategori"
    override val noProductsFound = "Belum ada produk yang sesuai."
    override val deleteProductConfirm = "Hapus produk ini dari daftar?"
    override val stockWarningLeft = "Sisa stok"
    override val searchTrxHint = "Cari nomor transaksi atau pelanggan..."
    override val filterDateLabel = "Periode Tanggal"
    override val filterMethodLabel = "Metode Bayar"
    override val trxDetailTitle = "Detail Transaksi"
    override val purchasedItemsLabel = "Daftar Produk Dibeli"
    override val reprintReceipt = "Cetak Ulang Struk"
    override val shareReceipt = "Bagikan Struk"
    override val cancelTransaction = "Batalkan Transaksi"
    override val refundTransaction = "Pengembalian (Refund)"
    override val cancelConfirmTitle = "Batalkan Transaksi?"
    override val refundConfirmTitle = "Proses Pengembalian Barang?"
    override val cancelRefundWarning = "Status transaksi akan diperbarui dan stok produk akan dikembalikan secara otomatis."
    override val reasonOptionalLabel = "Alasan (Opsional)"
    override val statusCompleted = "Selesai"
    override val statusCancelled = "Dibatalkan"
    override val statusRefunded = "Dikembalikan"
    override val storeProfileSection = "Profil Toko"
    override val storeNameLabel = "Nama Toko"
    override val ownerNameLabel = "Nama Pemilik"
    override val phoneLabel = "Nomor HP"
    override val addressLabel = "Alamat Toko"
    override val defaultTaxPercentLabel = "Tarif PPN Otomatis (%)"
    override val saveChanges = "Simpan Perubahan"
    override val profileSavedMsg = "Profil toko berhasil disimpan dan disinkronkan."
    override val themeSection = "Tema Aplikasi"
    override val lightTheme = "Tema Terang"
    override val darkTheme = "Tema Gelap"
    override val languageSection = "Bahasa"
    override val languageIndonesian = "Bahasa Indonesia"
    override val languageEnglish = "English"
    override val backupRestoreSection = "Pencadangan dan Pemulihan Data"
    override val manualBackupButton = "Backup Manual"
    override val restoreBackupButton = "Restore Data"
    override val lastBackupLabel = "Backup Terakhir"
    override val neverBackedUp = "Belum pernah dilakukan backup"
    override val backupInProgress = "Memproses cadangan data..."
    override val backupSuccessMsg = "Cadangan data berhasil dibuat dan disimpan."
    override val restoreConfirmTitle = "Konfirmasi Pemulihan Data"
    override val restoreConfirmBody = "Pemulihan akan memperbarui data produk, kategori, transaksi, dan profil toko Anda. Lanjutkan?"
    override val restoreSuccessMsg = "Data berhasil dipulihkan dan disinkronkan."
    override val restoreInvalidMsg = "File cadangan tidak valid atau rusak."
    override val aboutSection = "Tentang Aplikasi"
    override val versionLabel = "Versi Aplikasi"
    override val aboutDescription = "Aplikasi Point of Sale (POS) modern untuk mengelola transaksi kasir, produk, stok, dan riwayat toko dengan sinkronisasi real-time."
    override val copyrightText = "© 2026 Rafline Dev. Hak cipta dilindungi."
    override val googleAccountSection = "Akun Google & Sinkronisasi"
    override val syncedWithGoogle = "Tersinkronisasi otomatis dengan akun Google"
    override val signOutButton = "Keluar Akun"
    override val reportFilterDate = "Tanggal"
    override val reportFilterMonth = "Bulan"
    override val reportFilterYear = "Tahun"
    override val reportTotalSales = "Total Penjualan"
    override val reportOmset = "Omset"
    override val reportHpp = "HPP / Modal"
    override val reportGrossProfit = "Laba Kotor"
    override val reportExpensesSummary = "Pengeluaran Periode Ini"
    override val reportNetProfit = "Laba Bersih Estimasi"
    override val reportPaymentBreakdown = "Rincian Metode Pembayaran"
    override val reportSelectDate = "Pilih Tanggal"
    override val addExpense = "Tambah Pengeluaran"
    override val editExpense = "Edit Pengeluaran"
    override val expenseTitleLabel = "Nama / Keterangan Pengeluaran"
    override val expenseCategoryLabel = "Kategori Pengeluaran"
    override val expenseAmountLabel = "Nominal (Rp)"
    override val expenseNotesLabel = "Catatan (Opsional)"
    override val expenseDateLabel = "Tanggal Pengeluaran"
    override val todayExpensesLabel = "Pengeluaran Hari Ini"
    override val monthExpensesLabel = "Pengeluaran Bulan Ini"
    override val noExpensesYet = "Belum ada catatan pengeluaran."
    override val deleteExpenseConfirm = "Hapus catatan pengeluaran ini?"
    override val expenseSavedMsg = "Pengeluaran berhasil disimpan."
    override val settingsConfigSection = "Konfigurasi Toko & POS"
    override val settingsHardwareSection = "Hardware & Struk"
    override val settingsDataSection = "Data & Cadangan"
    override val settingsAccountSection = "Akun & Aplikasi"
    override val storeProfileSubtitle = "Nama toko, alamat, nomor HP, foto, & PPN"
    override val appearanceLanguageTitle = "Tampilan & Bahasa"
    override val appearanceLanguageSubtitle = "Tema aplikasi dan bahasa tampilan"
    override val printerSettingsTitle = "Printer Kasir"
    override val printerSettingsSubtitle = "Kelola printer Bluetooth & daftar tersambung"
    override val backupRestoreSubtitle = "Backup JSON manual & pulihkan data toko"
    override val connectedPrintersTitle = "Daftar Printer Tersambung"
    override val selectedPrinterLabel = "Printer Kasir Aktif"
    override val noPrinterSelected = "Belum ada printer default terpilih"
    override val defaultPaperWidthLabel = "Ukuran Kertas Struk Default"
    override val refreshPrintersButton = "Refresh Daftar"
    override val testPrintButton = "Tes Cetak"
    override val selectPrinterButton = "Pilih"
    override val disconnectPrinterButton = "Putuskan"
    override val printerConnectedBadge = "Tersambung"
    override val printerPairedBadge = "Terpasang"
    override val printerSelectedBadge = "Aktif"
    override val openBluetoothSettingsButton = "Buka Pengaturan Bluetooth HP"
    override val splashByDeveloper = "by Rafline Dev"
    override val receiptCashierLabel = "Kasir"
    override val receiptCustomTitle = "Kustomisasi Struk"
    override val receiptCustomSubtitle = "Logo toko, nama kasir, HP, alamat & footer"
    override val receiptPreviewLabel = "Pratinjau Gambar Struk"
    override val receiptShowLogoLabel = "Tampilkan Logo Toko"
    override val receiptShowCashierLabel = "Tampilkan Nama Kasir"
    override val receiptShowPhoneLabel = "Tampilkan Nomor HP Toko"
    override val receiptShowAddressLabel = "Tampilkan Alamat Toko"
    override val receiptShowCustomerLabel = "Tampilkan Nama Pelanggan"
    override val receiptFooterMessageLabel = "Pesan Footer Struk"
    override val receiptFooterMessageHint = "Contoh: Terima kasih, selamat belanja kembali!"
    override val receiptCustomSavedMsg = "Kustomisasi struk berhasil disimpan."
    override val userManageTitle = "Kelola Pengguna"
    override val userManageSubtitle = "Mode Kasir & Mode Pemantau multi-email"
    override val userRoleInfoTitle = "Informasi Mode Kasir & Pemantau"
    override val userRoleInfoBody = "• Mode Kasir: Akses penuh untuk mencatat transaksi, mengelola produk, stok, pengeluaran, laporan, dan pengaturan toko.\n\n• Mode Pemantau (Owner): Digunakan pemilik toko untuk memantau omset, laba, grafik penjualan, stok, dan riwayat secara real-time (hanya-baca) menggunakan email Google yang berbeda.\n\n• Cara menautkan: Pada akun Kasir, masukkan email Google Owner ke daftar Email Pemantau. Lalu login di HP Owner dengan email tersebut dan pilih Mode Pemantau."
    override val userModeCashierLabel = "Mode Kasir"
    override val userModeMonitorLabel = "Mode Pemantau"
    override val userModeCashierDesc = "Akses kasir penuh (kelola produk & transaksi)"
    override val userModeMonitorDesc = "Pantau penjualan toko lain secara real-time (hanya-baca)"
    override val monitorEmailListLabel = "Email Pemantau Terdaftar (Untuk Owner)"
    override val monitorEmailInputHint = "Contoh: owner@gmail.com"
    override val addMonitorEmailButton = "Tambah Email"
    override val noMonitorEmailsYet = "Belum ada email pemantau yang ditautkan."
    override val monitorStoresAvailableLabel = "Toko yang Dapat Dipantau"
    override val monitorNoStoresLinked = "Belum ada toko Kasir yang menautkan email Google ini (%s)."
    override val connectStoreButton = "Pantau Toko"
    override val connectedBadge = "Terhubung"
    override val monitorBannerTitle = "Mode Pemantau (Hanya Baca)"
    override val monitorReadOnlyError = "Mode Pemantau bersifat hanya-baca. Ubah ke Mode Kasir untuk mengedit data."
}

object EnglishStrings : AppStrings {
    override val appName = "ADA KASIR"
    override val developerBy = "Rafline Dev"
    override val loginTitle = "ADA KASIR"
    override val loginSubtitle = "Modern Point of Sale application with automatic cloud sync across all your devices."
    override val loginButton = "Sign in with Google"
    override val loginSyncNote = "Just sign in with one Google account. Your store profile, products, stock, and transactions sync automatically."
    override val loginCancelled = "Google Sign-In cancelled. Please choose an account to continue."
    override val loginSavedSessionNote = "Saved session (%s). Tap Sign in with Google to open Dashboard."
    override val loginErrorFallback = "Google Sign-In failed. Please check your connection or Google account and try again."
    override val loginSigningIn = "Signing in..."
    override val menuTitle = "Main Menu"
    override val menuDashboard = "Dashboard"
    override val menuCashier = "Cashier"
    override val menuProducts = "Products"
    override val menuSalesReport = "Sales Report"
    override val menuExpenses = "Expenses"
    override val menuHistory = "History"
    override val menuSettings = "Settings"
    override val greetingMorning = "Good Morning"
    override val greetingAfternoon = "Good Afternoon"
    override val greetingEvening = "Good Evening"
    override val greetingNight = "Good Night"
    override val searchPlaceholder = "Search..."
    override val cancel = "Cancel"
    override val confirm = "Confirm"
    override val save = "Save"
    override val delete = "Delete"
    override val edit = "Edit"
    override val close = "Close"
    override val all = "All"
    override val loading = "Loading..."
    override val retry = "Retry"
    override val todaySales = "Today's Total Sales"
    override val todayTransactions = "Today's Transactions"
    override val salesTrendTitle = "Sales Trend Chart"
    override val salesTrendSubtitle = "Cashier Activity & Peak Hours"
    override val periodToday = "Today"
    override val period7Days = "7 days"
    override val period30Days = "30 days"
    override val periodThisMonth = "This month"
    override val topProductsTitle = "Best-Selling Products"
    override val soldLabel = "sold"
    override val noSalesDataYet = "No completed transactions in this period yet."
    override val recentTransactionsTitle = "Recent Transactions"
    override val viewAllHistory = "View All History"
    override val noTransactionsYet = "No transactions recorded yet."
    override val stockAlertsTitle = "Stock Notifications"
    override val allStockHealthy = "All product stock levels are healthy."
    override val lowStockBadge = "Low Stock"
    override val outOfStockBadge = "Out of Stock"
    override val searchProductHint = "Search product name or SKU..."
    override val allProducts = "All products"
    override val cartTitle = "Cart"
    override val emptyCartHint = "Tap a product to add it to the cart"
    override val addToCart = "Add to Cart"
    override val selectQuantityTitle = "Purchase Quantity"
    override val quickAmountLabel = "Quick Select"
    override val manualDecimalLabel = "Manual Input (e.g. 0.75, 1.25)"
    override val estimatedSubtotal = "Item Subtotal"
    override val subtotalLabel = "Subtotal"
    override val customerNameLabel = "Customer Name (Optional)"
    override val customerNameHint = "May be left blank"
    override val discountLabel = "Discount"
    override val discountNone = "No Discount"
    override val discountPercent = "Percentage (%)"
    override val discountAmount = "Amount (Rp)"
    override val taxLabel = "Tax (PPN)"
    override val taxNone = "No Tax"
    override val taxAuto = "Auto Tax"
    override val taxManual = "Manual Tax (Rp)"
    override val paymentMethodLabel = "Payment Method"
    override val methodCash = "Cash"
    override val methodBankTransfer = "Bank Transfer"
    override val methodQris = "QRIS"
    override val methodOther = "Other"
    override val amountReceivedLabel = "Amount Received"
    override val exactCashButton = "Exact Cash"
    override val changeLabel = "Change"
    override val grandTotalLabel = "Grand Total"
    override val completeTransactionButton = "Complete Transaction"
    override val insufficientStockError = "Insufficient product stock."
    override val insufficientPaymentError = "Amount received is less than grand total."
    override val transactionSuccessMsg = "Transaction saved successfully!"
    override val viewCartButton = "View Cart"
    override val receiptTitle = "Transaction Receipt"
    override val receiptNumber = "Transaction No."
    override val receiptDate = "Time"
    override val receiptCustomer = "Customer"
    override val receiptCashierNote = "Thank you for shopping with us!"
    override val printReceipt = "Print Receipt"
    override val savePdf = "Save PDF"
    override val shareWhatsApp = "Share WA"
    override val printerWidth58 = "Thermal 58 mm"
    override val printerWidth80 = "Thermal 80 mm"
    override val selectPrinterTitle = "Select Bluetooth Printer"
    override val noPairedPrinters = "No paired Bluetooth thermal printer found. Use System Print / PDF or pair your printer in Android Bluetooth settings."
    override val printSuccessMsg = "Receipt sent to printer."
    override val printFailedMsg = "Failed to print to Bluetooth printer."
    override val pdfSavedMsg = "Receipt PDF saved successfully."
    override val bluetoothPermissionNeeded = "Bluetooth permission is required to print to thermal printers."
    override val systemPrintFallback = "System Print / PDF"
    override val addProduct = "Add Product"
    override val editProduct = "Edit Product"
    override val productNameLabel = "Product Name"
    override val productSkuLabel = "Product Code / SKU"
    override val productCategoryLabel = "Category"
    override val productUnitLabel = "Unit"
    override val productCostPriceLabel = "Cost Price (Rp)"
    override val productSellPriceLabel = "Sell Price (Rp)"
    override val productStockLabel = "Stock Quantity"
    override val productMinStockLabel = "Minimum Stock (Optional)"
    override val productDescLabel = "Product Description (Optional)"
    override val productPhotoLabel = "Product Photo"
    override val pickFromGallery = "Choose from Gallery"
    override val takePhoto = "Take Photo"
    override val manageCategories = "Manage Categories"
    override val addCategory = "Add Category"
    override val editCategory = "Edit Category"
    override val categoryNameLabel = "Category Name"
    override val noProductsFound = "No matching products found."
    override val deleteProductConfirm = "Delete this product from inventory?"
    override val stockWarningLeft = "Remaining stock"
    override val searchTrxHint = "Search transaction number or customer..."
    override val filterDateLabel = "Date Period"
    override val filterMethodLabel = "Payment Method"
    override val trxDetailTitle = "Transaction Details"
    override val purchasedItemsLabel = "Purchased Items"
    override val reprintReceipt = "Reprint Receipt"
    override val shareReceipt = "Share Receipt"
    override val cancelTransaction = "Cancel Transaction"
    override val refundTransaction = "Refund Transaction"
    override val cancelConfirmTitle = "Cancel Transaction?"
    override val refundConfirmTitle = "Process Refund?"
    override val cancelRefundWarning = "Transaction status will be updated and product stock will be restored automatically."
    override val reasonOptionalLabel = "Reason (Optional)"
    override val statusCompleted = "Completed"
    override val statusCancelled = "Cancelled"
    override val statusRefunded = "Refunded"
    override val storeProfileSection = "Store Profile"
    override val storeNameLabel = "Store Name"
    override val ownerNameLabel = "Owner Name"
    override val phoneLabel = "Phone Number"
    override val addressLabel = "Store Address"
    override val defaultTaxPercentLabel = "Auto Tax Rate (%)"
    override val saveChanges = "Save Changes"
    override val profileSavedMsg = "Store profile saved and synced."
    override val themeSection = "App Theme"
    override val lightTheme = "Light Theme"
    override val darkTheme = "Dark Theme"
    override val languageSection = "Language"
    override val languageIndonesian = "Bahasa Indonesia"
    override val languageEnglish = "English"
    override val backupRestoreSection = "Data Backup & Restore"
    override val manualBackupButton = "Manual Backup"
    override val restoreBackupButton = "Restore Data"
    override val lastBackupLabel = "Last Backup"
    override val neverBackedUp = "Never backed up yet"
    override val backupInProgress = "Processing data backup..."
    override val backupSuccessMsg = "Backup created and saved successfully."
    override val restoreConfirmTitle = "Confirm Data Restore"
    override val restoreConfirmBody = "Restoring will update your products, categories, transactions, and store profile. Continue?"
    override val restoreSuccessMsg = "Data restored and synced successfully."
    override val restoreInvalidMsg = "Invalid or corrupted backup file."
    override val aboutSection = "About App"
    override val versionLabel = "App Version"
    override val aboutDescription = "Modern Point of Sale (POS) application to manage cashier transactions, products, stock, and store history with real-time sync."
    override val copyrightText = "© 2026 Rafline Dev. All rights reserved."
    override val googleAccountSection = "Google Account & Sync"
    override val syncedWithGoogle = "Automatically synced with Google account"
    override val signOutButton = "Sign Out"
    override val reportFilterDate = "Date"
    override val reportFilterMonth = "Month"
    override val reportFilterYear = "Year"
    override val reportTotalSales = "Total Sales"
    override val reportOmset = "Revenue (Omset)"
    override val reportHpp = "COGS / Capital"
    override val reportGrossProfit = "Gross Profit"
    override val reportExpensesSummary = "Period Expenses"
    override val reportNetProfit = "Estimated Net Profit"
    override val reportPaymentBreakdown = "Payment Method Breakdown"
    override val reportSelectDate = "Select Date"
    override val addExpense = "Add Expense"
    override val editExpense = "Edit Expense"
    override val expenseTitleLabel = "Expense Title"
    override val expenseCategoryLabel = "Expense Category"
    override val expenseAmountLabel = "Amount (Rp)"
    override val expenseNotesLabel = "Notes (Optional)"
    override val expenseDateLabel = "Expense Date"
    override val todayExpensesLabel = "Today's Expenses"
    override val monthExpensesLabel = "This Month's Expenses"
    override val noExpensesYet = "No expenses recorded yet."
    override val deleteExpenseConfirm = "Delete this expense record?"
    override val expenseSavedMsg = "Expense saved successfully."
    override val settingsConfigSection = "Store & POS Configuration"
    override val settingsHardwareSection = "Hardware & Receipt"
    override val settingsDataSection = "Data & Backup"
    override val settingsAccountSection = "Account & App"
    override val storeProfileSubtitle = "Store name, address, phone, photo, & tax"
    override val appearanceLanguageTitle = "Appearance & Language"
    override val appearanceLanguageSubtitle = "App theme and display language"
    override val printerSettingsTitle = "Cashier Printer"
    override val printerSettingsSubtitle = "Manage Bluetooth printers & connected list"
    override val backupRestoreSubtitle = "Manual JSON backup & restore store data"
    override val connectedPrintersTitle = "Connected Printers List"
    override val selectedPrinterLabel = "Active Cashier Printer"
    override val noPrinterSelected = "No default printer selected"
    override val defaultPaperWidthLabel = "Default Receipt Paper Width"
    override val refreshPrintersButton = "Refresh List"
    override val testPrintButton = "Test Print"
    override val selectPrinterButton = "Select"
    override val disconnectPrinterButton = "Disconnect"
    override val printerConnectedBadge = "Connected"
    override val printerPairedBadge = "Paired"
    override val printerSelectedBadge = "Active"
    override val openBluetoothSettingsButton = "Open Android Bluetooth Settings"
    override val splashByDeveloper = "by Rafline Dev"
    override val receiptCashierLabel = "Cashier"
    override val receiptCustomTitle = "Receipt Customization"
    override val receiptCustomSubtitle = "Store logo, cashier, phone, address & footer"
    override val receiptPreviewLabel = "Receipt Image Preview"
    override val receiptShowLogoLabel = "Show Store Logo"
    override val receiptShowCashierLabel = "Show Cashier Name"
    override val receiptShowPhoneLabel = "Show Store Phone"
    override val receiptShowAddressLabel = "Show Store Address"
    override val receiptShowCustomerLabel = "Show Customer Name"
    override val receiptFooterMessageLabel = "Receipt Footer Message"
    override val receiptFooterMessageHint = "e.g. Thank you, please come again!"
    override val receiptCustomSavedMsg = "Receipt customization saved."
    override val userManageTitle = "Manage Users"
    override val userManageSubtitle = "Cashier Mode & Multi-Email Monitor Mode"
    override val userRoleInfoTitle = "Cashier & Monitor Mode Guide"
    override val userRoleInfoBody = "• Cashier Mode: Full access to process sales, manage products, stock, expenses, reports, and store settings.\n\n• Monitor Mode (Owner): Allows the store owner to monitor sales, profit, charts, inventory, and history in real-time (read-only) using a different Google email.\n\n• How to link: On the Cashier account, add the Owner's Google email to Authorized Monitor Emails. Then sign in on the Owner's device with that email and switch to Monitor Mode."
    override val userModeCashierLabel = "Cashier Mode"
    override val userModeMonitorLabel = "Monitor Mode"
    override val userModeCashierDesc = "Full POS access (manage products & sales)"
    override val userModeMonitorDesc = "Monitor linked store sales in real-time (read-only)"
    override val monitorEmailListLabel = "Authorized Monitor Emails (For Owner)"
    override val monitorEmailInputHint = "e.g. owner@gmail.com"
    override val addMonitorEmailButton = "Add Email"
    override val noMonitorEmailsYet = "No monitor emails linked yet."
    override val monitorStoresAvailableLabel = "Linked Cashier Stores"
    override val monitorNoStoresLinked = "No cashier store has authorized this email (%s) yet."
    override val connectStoreButton = "Monitor Store"
    override val connectedBadge = "Connected"
    override val monitorBannerTitle = "Monitor Mode (Read-Only)"
    override val monitorReadOnlyError = "Monitor Mode is read-only. Switch to Cashier Mode to edit data."
}

val LocalAppStrings = staticCompositionLocalOf<AppStrings> { IndonesianStrings }
