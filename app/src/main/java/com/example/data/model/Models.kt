package com.example.data.model

enum class Role(val label: String) {
    OWNER("Owner"),
    KASIR("Kasir")
}

data class User(
    val id: String,
    val name: String,
    val role: Role,
    val pin: String
)

data class Product(
    val id: String,
    val name: String,
    val barcode: String,
    val category: String,
    val sellingPrice: Long,
    val costPrice: Long, // Harga Modal
    val stock: Double,
    val minStock: Double = 5.0,
    val unit: String = "lembar",
    val updatedAt: Long = System.currentTimeMillis()
)

data class CartItem(
    val product: Product,
    val quantity: Double,
    val itemNote: String = ""
) {
    val subtotal: Long
        get() = (product.sellingPrice * quantity).toLong()

    val totalCost: Long
        get() = (product.costPrice * quantity).toLong()
}

enum class PaymentMethod(val label: String) {
    TUNAI("Tunai"),
    NON_TUNAI("Non-Tunai (QRIS/Transfer)"),
    CICILAN("Cicilan / Tempo")
}

enum class PaymentStatus(val label: String) {
    LUNAS("Lunas"),
    KURANG_BAYAR("Kurang Bayar"),
    BELUM_DIBAYAR("Belum Dibayar")
}

data class InstallmentPayment(
    val id: String,
    val date: Long,
    val amount: Long,
    val paymentMethod: PaymentMethod,
    val note: String,
    val receivedBy: String
)

data class Transaction(
    val id: String,
    val timestamp: Long,
    val cashierName: String,
    val customerName: String = "",
    val customerPhone: String = "",
    val transactionNote: String = "",
    val items: List<CartItem>,
    val totalAmount: Long,
    val totalCost: Long,
    val paymentMethod: PaymentMethod,
    val amountPaid: Long,
    val changeAmount: Long, // Kembalian
    val remainingBalance: Long, // Sisa Piutang
    val status: PaymentStatus,
    val dueDate: Long? = null,
    val installments: List<InstallmentPayment> = emptyList()
)

data class PurchaseItem(
    val productId: String,
    val productName: String,
    val quantity: Double,
    val costPrice: Long,
    val unit: String
) {
    val subtotal: Long
        get() = (costPrice * quantity).toLong()
}

data class DebtRepayment(
    val id: String,
    val date: Long,
    val amount: Long,
    val note: String
)

data class SupplierPurchase(
    val id: String,
    val timestamp: Long,
    val supplierName: String,
    val invoiceNumber: String,
    val items: List<PurchaseItem>,
    val totalAmount: Long,
    val amountPaid: Long,
    val remainingDebt: Long,
    val status: PaymentStatus,
    val dueDate: Long? = null,
    val debtRepayments: List<DebtRepayment> = emptyList()
)

data class Expense(
    val id: String,
    val timestamp: Long,
    val category: String,
    val amount: Long,
    val note: String,
    val paymentMethod: PaymentMethod,
    val recordedBy: String
)

data class ReceiptSettings(
    val storeName: String = "CyberPos",
    val address: String = "Jl. Cyber POS No. 88, Indonesia",
    val phoneNumber: String = "081234567890",
    val openingMessage: String = "Selamat Datang & Terima Kasih atas Kunjungan Anda",
    val closingMessage: String = "Barang yang sudah dibeli tidak dapat ditukar atau dikembalikan.",
    val showAddress: Boolean = true,
    val showPhone: Boolean = true,
    val showCashier: Boolean = true,
    val showNote: Boolean = true,
    val showRemainingBalance: Boolean = true,
    val paperWidthMm: Int = 58
)

enum class Screen(val title: String) {
    KASIR("Kasir (POS)"),
    KATALOG("Katalog & Stok"),
    KULAKAN("Kulakan (Beli)"),
    PIUTANG("Piutang Pelanggan"),
    HUTANG("Hutang Pemasok"),
    PENGELUARAN("Pengeluaran & Kas"),
    LAPORAN("Laporan & Statistik"),
    PENGATURAN("Pengaturan")
}
