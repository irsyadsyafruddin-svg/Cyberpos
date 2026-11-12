package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.data.model.*
import com.example.data.repository.CyberPosRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class CyberPosViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = CyberPosRepository(application)

    private val _currentUser = MutableStateFlow(repository.getCurrentUser())
    val currentUser: StateFlow<User> = _currentUser.asStateFlow()

    private val _users = MutableStateFlow(repository.getUsers())
    val users: StateFlow<List<User>> = _users.asStateFlow()

    private val _products = MutableStateFlow(repository.getProducts())
    val products: StateFlow<List<Product>> = _products.asStateFlow()

    private val _transactions = MutableStateFlow(repository.getTransactions())
    val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()

    private val _purchases = MutableStateFlow(repository.getPurchases())
    val purchases: StateFlow<List<SupplierPurchase>> = _purchases.asStateFlow()

    private val _expenses = MutableStateFlow(repository.getExpenses())
    val expenses: StateFlow<List<Expense>> = _expenses.asStateFlow()

    private val _receiptSettings = MutableStateFlow(repository.getReceiptSettings())
    val receiptSettings: StateFlow<ReceiptSettings> = _receiptSettings.asStateFlow()

    private val _currentScreen = MutableStateFlow(Screen.KASIR)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val _cart = MutableStateFlow<List<CartItem>>(emptyList())
    val cart: StateFlow<List<CartItem>> = _cart.asStateFlow()

    private val _activeReceiptTransaction = MutableStateFlow<Transaction?>(null)
    val activeReceiptTransaction: StateFlow<Transaction?> = _activeReceiptTransaction.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun showMessage(msg: String) {
        _userMessage.value = msg
    }

    fun setScreen(screen: Screen): Boolean {
        // Kasir role can only access KASIR, PIUTANG, PENGELUARAN
        if (_currentUser.value.role == Role.KASIR) {
            if (screen != Screen.KASIR && screen != Screen.PIUTANG && screen != Screen.PENGELUARAN) {
                showMessage("Akses dibatasi! Menu ${screen.title} hanya untuk Owner.")
                return false
            }
        }
        _currentScreen.value = screen
        return true
    }

    fun openReceipt(transaction: Transaction) {
        _activeReceiptTransaction.value = transaction
    }

    fun closeReceipt() {
        _activeReceiptTransaction.value = null
    }

    // --- AUTH & ROLES ---
    fun switchUser(user: User, enteredPin: String): Boolean {
        if (user.pin == enteredPin) {
            _currentUser.value = user
            repository.saveCurrentUser(user)
            if (user.role == Role.KASIR && _currentScreen.value != Screen.KASIR && _currentScreen.value != Screen.PIUTANG && _currentScreen.value != Screen.PENGELUARAN) {
                _currentScreen.value = Screen.KASIR
            }
            showMessage("Berhasil beralih pengguna ke: ${user.name} (${user.role.label})")
            return true
        } else {
            showMessage("PIN yang Anda masukkan salah!")
            return false
        }
    }

    fun verifyOwnerPin(pin: String): Boolean {
        val owner = _users.value.firstOrNull { it.role == Role.OWNER }
        return owner?.pin == pin
    }

    fun saveUser(user: User) {
        val currentList = _users.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == user.id }
        if (index >= 0) {
            currentList[index] = user
        } else {
            currentList.add(user)
        }
        _users.value = currentList
        repository.saveUsers(currentList)
        if (_currentUser.value.id == user.id) {
            _currentUser.value = user
            repository.saveCurrentUser(user)
        }
        showMessage("Pengguna ${user.name} berhasil disimpan.")
    }

    fun deleteUser(userId: String) {
        val userToDelete = _users.value.firstOrNull { it.id == userId } ?: return
        if (userToDelete.id == _currentUser.value.id) {
            showMessage("Tidak dapat menghapus pengguna yang sedang aktif!")
            return
        }
        val currentList = _users.value.filter { it.id != userId }
        _users.value = currentList
        repository.saveUsers(currentList)
        showMessage("Pengguna berhasil dihapus.")
    }

    // --- CART (KASIR) ---
    fun addToCart(product: Product, qty: Double = 1.0) {
        val current = _cart.value.toMutableList()
        val existingIndex = current.indexOfFirst { it.product.id == product.id }

        if (existingIndex >= 0) {
            val item = current[existingIndex]
            val newQty = item.quantity + qty
            if (newQty > product.stock) {
                showMessage("Stok ${product.name} tidak mencukupi (Tersisa: ${product.stock} ${product.unit})")
                return
            }
            current[existingIndex] = item.copy(quantity = newQty)
        } else {
            if (qty > product.stock) {
                showMessage("Stok ${product.name} tidak mencukupi (Tersisa: ${product.stock} ${product.unit})")
                return
            }
            current.add(CartItem(product = product, quantity = qty))
        }
        _cart.value = current
    }

    fun updateCartQuantity(productId: String, newQty: Double) {
        val current = _cart.value.toMutableList()
        val index = current.indexOfFirst { it.product.id == productId }
        if (index >= 0) {
            val item = current[index]
            if (newQty <= 0) {
                current.removeAt(index)
            } else {
                if (newQty > item.product.stock) {
                    showMessage("Stok tidak mencukupi (Tersisa: ${item.product.stock} ${item.product.unit})")
                    return
                }
                current[index] = item.copy(quantity = newQty)
            }
            _cart.value = current
        }
    }

    fun removeFromCart(productId: String) {
        _cart.value = _cart.value.filter { it.product.id != productId }
    }

    fun clearCart() {
        _cart.value = emptyList()
    }

    // --- CHECKOUT ---
    fun checkoutTransaction(
        customerName: String,
        customerPhone: String,
        transactionNote: String,
        paymentMethod: PaymentMethod,
        amountPaid: Long,
        dueDate: Long?
    ): Transaction? {
        val items = _cart.value
        if (items.isEmpty()) {
            showMessage("Keranjang belanja masih kosong!")
            return null
        }

        val totalAmount = items.sumOf { it.subtotal }
        val totalCost = items.sumOf { it.totalCost }

        val changeAmount: Long
        val remainingBalance: Long
        val status: PaymentStatus

        when (paymentMethod) {
            PaymentMethod.TUNAI, PaymentMethod.NON_TUNAI -> {
                if (amountPaid >= totalAmount) {
                    changeAmount = amountPaid - totalAmount
                    remainingBalance = 0L
                    status = PaymentStatus.LUNAS
                } else if (amountPaid > 0) {
                    changeAmount = 0L
                    remainingBalance = totalAmount - amountPaid
                    status = PaymentStatus.KURANG_BAYAR
                } else {
                    changeAmount = 0L
                    remainingBalance = totalAmount
                    status = PaymentStatus.BELUM_DIBAYAR
                }
            }
            PaymentMethod.CICILAN -> {
                if (amountPaid >= totalAmount) {
                    changeAmount = amountPaid - totalAmount
                    remainingBalance = 0L
                    status = PaymentStatus.LUNAS
                } else if (amountPaid > 0) {
                    changeAmount = 0L
                    remainingBalance = totalAmount - amountPaid
                    status = PaymentStatus.KURANG_BAYAR
                } else {
                    changeAmount = 0L
                    remainingBalance = totalAmount
                    status = PaymentStatus.BELUM_DIBAYAR
                }
            }
        }

        val seq = (_transactions.value.size + 1).toString().padStart(4, '0')
        val datePrefix = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        val trxId = "TRX-$datePrefix-$seq"

        val initialInstallments = if (amountPaid > 0) {
            listOf(
                InstallmentPayment(
                    id = UUID.randomUUID().toString(),
                    date = System.currentTimeMillis(),
                    amount = amountPaid.coerceAtMost(totalAmount),
                    paymentMethod = paymentMethod,
                    note = "Pembayaran Awal (DP/Checkout)",
                    receivedBy = _currentUser.value.name
                )
            )
        } else emptyList()

        val transaction = Transaction(
            id = trxId,
            timestamp = System.currentTimeMillis(),
            cashierName = _currentUser.value.name,
            customerName = customerName.trim(),
            customerPhone = customerPhone.trim(),
            transactionNote = transactionNote.trim(),
            items = items,
            totalAmount = totalAmount,
            totalCost = totalCost,
            paymentMethod = paymentMethod,
            amountPaid = amountPaid,
            changeAmount = changeAmount,
            remainingBalance = remainingBalance,
            status = status,
            dueDate = dueDate,
            installments = initialInstallments
        )

        // Deduct stock for products
        val updatedProducts = _products.value.toMutableList()
        for (item in items) {
            val pIndex = updatedProducts.indexOfFirst { it.id == item.product.id }
            if (pIndex >= 0) {
                val currentP = updatedProducts[pIndex]
                val newStock = (currentP.stock - item.quantity).coerceAtLeast(0.0)
                updatedProducts[pIndex] = currentP.copy(stock = newStock, updatedAt = System.currentTimeMillis())
            }
        }
        _products.value = updatedProducts
        repository.saveProducts(updatedProducts)

        // Add to transactions
        val updatedTrx = listOf(transaction) + _transactions.value
        _transactions.value = updatedTrx
        repository.saveTransactions(updatedTrx)

        clearCart()
        openReceipt(transaction)
        showMessage("Transaksi $trxId berhasil disimpan!")
        return transaction
    }

    // --- PIUTANG INSTALLMENT ---
    fun payInstallment(
        transactionId: String,
        amount: Long,
        paymentMethod: PaymentMethod,
        note: String
    ): Boolean {
        if (amount <= 0) {
            showMessage("Nominal pembayaran harus lebih dari 0!")
            return false
        }
        val currentTrx = _transactions.value.toMutableList()
        val index = currentTrx.indexOfFirst { it.id == transactionId }
        if (index < 0) return false

        val trx = currentTrx[index]
        val newAmountPaid = trx.amountPaid + amount
        val newRemaining = (trx.totalAmount - newAmountPaid).coerceAtLeast(0L)
        val newStatus = if (newRemaining <= 0L) PaymentStatus.LUNAS else PaymentStatus.KURANG_BAYAR

        val newInstallment = InstallmentPayment(
            id = UUID.randomUUID().toString(),
            date = System.currentTimeMillis(),
            amount = amount,
            paymentMethod = paymentMethod,
            note = note.trim().ifEmpty { "Pembayaran Cicilan" },
            receivedBy = _currentUser.value.name
        )

        val updated = trx.copy(
            amountPaid = newAmountPaid,
            remainingBalance = newRemaining,
            status = newStatus,
            installments = trx.installments + newInstallment
        )

        currentTrx[index] = updated
        _transactions.value = currentTrx
        repository.saveTransactions(currentTrx)

        openReceipt(updated)
        showMessage("Pembayaran cicilan Rp $amount untuk ${trx.id} berhasil dicatat!")
        return true
    }

    // --- KULAKAN (PURCHASES) ---
    fun recordPurchase(
        supplierName: String,
        invoiceNumber: String,
        items: List<PurchaseItem>,
        amountPaid: Long,
        dueDate: Long?
    ): Boolean {
        if (supplierName.isBlank()) {
            showMessage("Nama pemasok/supplier wajib diisi!")
            return false
        }
        if (items.isEmpty()) {
            showMessage("Daftar barang kulakan tidak boleh kosong!")
            return false
        }

        val totalAmount = items.sumOf { it.subtotal }
        val remainingDebt = (totalAmount - amountPaid).coerceAtLeast(0L)
        val status = if (remainingDebt <= 0L) PaymentStatus.LUNAS
        else if (amountPaid > 0) PaymentStatus.KURANG_BAYAR
        else PaymentStatus.BELUM_DIBAYAR

        val seq = (_purchases.value.size + 1).toString().padStart(4, '0')
        val datePrefix = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        val kulId = "KUL-$datePrefix-$seq"

        val initialRepayments = if (amountPaid > 0) {
            listOf(
                DebtRepayment(
                    id = UUID.randomUUID().toString(),
                    date = System.currentTimeMillis(),
                    amount = amountPaid,
                    note = "Uang Muka / Pembayaran Awal"
                )
            )
        } else emptyList()

        val purchase = SupplierPurchase(
            id = kulId,
            timestamp = System.currentTimeMillis(),
            supplierName = supplierName.trim(),
            invoiceNumber = invoiceNumber.trim().ifEmpty { "-" },
            items = items,
            totalAmount = totalAmount,
            amountPaid = amountPaid,
            remainingDebt = remainingDebt,
            status = status,
            dueDate = dueDate,
            debtRepayments = initialRepayments
        )

        // Automatically increase stock of purchased items
        val updatedProducts = _products.value.toMutableList()
        for (item in items) {
            val pIndex = updatedProducts.indexOfFirst { it.id == item.productId }
            if (pIndex >= 0) {
                val currentP = updatedProducts[pIndex]
                val newStock = currentP.stock + item.quantity
                updatedProducts[pIndex] = currentP.copy(
                    stock = newStock,
                    costPrice = item.costPrice, // update modal to latest purchase price
                    updatedAt = System.currentTimeMillis()
                )
            }
        }
        _products.value = updatedProducts
        repository.saveProducts(updatedProducts)

        val updatedPurchases = listOf(purchase) + _purchases.value
        _purchases.value = updatedPurchases
        repository.savePurchases(updatedPurchases)

        showMessage("Kulakan $kulId berhasil dicatat & stok bertambah!")
        return true
    }

    fun paySupplierDebt(purchaseId: String, amount: Long, note: String): Boolean {
        if (amount <= 0) {
            showMessage("Nominal pembayaran harus lebih dari 0!")
            return false
        }
        val current = _purchases.value.toMutableList()
        val index = current.indexOfFirst { it.id == purchaseId }
        if (index < 0) return false

        val pur = current[index]
        val newPaid = pur.amountPaid + amount
        val newRemaining = (pur.totalAmount - newPaid).coerceAtLeast(0L)
        val newStatus = if (newRemaining <= 0L) PaymentStatus.LUNAS else PaymentStatus.KURANG_BAYAR

        val repayment = DebtRepayment(
            id = UUID.randomUUID().toString(),
            date = System.currentTimeMillis(),
            amount = amount,
            note = note.trim().ifEmpty { "Bayar Hutang Pemasok" }
        )

        current[index] = pur.copy(
            amountPaid = newPaid,
            remainingDebt = newRemaining,
            status = newStatus,
            debtRepayments = pur.debtRepayments + repayment
        )
        _purchases.value = current
        repository.savePurchases(current)

        showMessage("Pembayaran hutang ke ${pur.supplierName} berhasil dicatat!")
        return true
    }

    // --- EXPENSES ---
    fun recordExpense(
        category: String,
        amount: Long,
        note: String,
        paymentMethod: PaymentMethod
    ): Boolean {
        if (amount <= 0) {
            showMessage("Nominal pengeluaran harus lebih dari 0!")
            return false
        }
        val expense = Expense(
            id = "EXP-" + System.currentTimeMillis().toString().takeLast(6),
            timestamp = System.currentTimeMillis(),
            category = category,
            amount = amount,
            note = note.trim(),
            paymentMethod = paymentMethod,
            recordedBy = _currentUser.value.name
        )
        val updated = listOf(expense) + _expenses.value
        _expenses.value = updated
        repository.saveExpenses(updated)
        showMessage("Pengeluaran Rp $amount berhasil disimpan!")
        return true
    }

    fun deleteExpense(id: String) {
        val updated = _expenses.value.filter { it.id != id }
        _expenses.value = updated
        repository.saveExpenses(updated)
        showMessage("Pengeluaran berhasil dihapus.")
    }

    // --- PRODUCTS ---
    fun saveProduct(product: Product) {
        val current = _products.value.toMutableList()
        val index = current.indexOfFirst { it.id == product.id }
        if (index >= 0) {
            current[index] = product.copy(updatedAt = System.currentTimeMillis())
        } else {
            current.add(product.copy(updatedAt = System.currentTimeMillis()))
        }
        _products.value = current
        repository.saveProducts(current)
        showMessage("Produk ${product.name} berhasil disimpan.")
    }

    fun deleteProduct(productId: String) {
        val updated = _products.value.filter { it.id != productId }
        _products.value = updated
        repository.saveProducts(updated)
        showMessage("Produk berhasil dihapus.")
    }

    fun quickUpdateStock(productId: String, newStock: Double) {
        val current = _products.value.toMutableList()
        val index = current.indexOfFirst { it.id == productId }
        if (index >= 0) {
            current[index] = current[index].copy(stock = newStock, updatedAt = System.currentTimeMillis())
            _products.value = current
            repository.saveProducts(current)
            showMessage("Stok ${current[index].name} diperbarui menjadi $newStock ${current[index].unit}")
        }
    }

    // --- RECEIPT SETTINGS ---
    fun updateReceiptSettings(settings: ReceiptSettings) {
        _receiptSettings.value = settings
        repository.saveReceiptSettings(settings)
        showMessage("Pengaturan struk berhasil disimpan.")
    }

    // --- BACKUP & RESTORE ---
    fun exportBackupJson(): String {
        return repository.exportAllToJson()
    }

    fun importBackupJson(jsonString: String): Boolean {
        val success = repository.importAllFromJson(jsonString)
        if (success) {
            _users.value = repository.getUsers()
            _currentUser.value = repository.getCurrentUser()
            _products.value = repository.getProducts()
            _transactions.value = repository.getTransactions()
            _purchases.value = repository.getPurchases()
            _expenses.value = repository.getExpenses()
            _receiptSettings.value = repository.getReceiptSettings()
            showMessage("Data berhasil dipulihkan (Restore Sukses)!")
            return true
        } else {
            showMessage("Gagal membaca file/format JSON tidak valid!")
            return false
        }
    }

    fun resetToDefaults() {
        repository.seedInitialData()
        _users.value = repository.getUsers()
        _currentUser.value = repository.getCurrentUser()
        _products.value = repository.getProducts()
        _transactions.value = repository.getTransactions()
        _purchases.value = repository.getPurchases()
        _expenses.value = repository.getExpenses()
        _receiptSettings.value = repository.getReceiptSettings()
        _cart.value = emptyList()
        _currentScreen.value = Screen.KASIR
        showMessage("Data berhasil direset ke data awal default!")
    }
}
