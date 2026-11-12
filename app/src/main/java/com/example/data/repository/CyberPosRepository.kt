package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class CyberPosRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("cyberpos_storage", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_INITIALIZED = "cyberpos_initialized"
        private const val KEY_USERS = "cyberpos_users"
        private const val KEY_CURRENT_USER = "cyberpos_current_user"
        private const val KEY_PRODUCTS = "cyberpos_products"
        private const val KEY_TRANSACTIONS = "cyberpos_transactions"
        private const val KEY_PURCHASES = "cyberpos_purchases"
        private const val KEY_EXPENSES = "cyberpos_expenses"
        private const val KEY_RECEIPT_SETTINGS = "cyberpos_receipt_settings"
    }

    init {
        if (!prefs.getBoolean(KEY_INITIALIZED, false)) {
            seedInitialData()
        } else {
            // Clean up and migrate legacy data if present (remove raw materials, migrate default unit to lembar)
            val currentProducts = getProducts()
            val hasLegacy = currentProducts.any { it.id == "mat_thermal" || it.id == "mat_plastic" || it.unit == "pcs" }
            if (hasLegacy) {
                val cleaned = currentProducts
                    .filter { it.id != "mat_thermal" && it.id != "mat_plastic" }
                    .map { p ->
                        if (p.unit == "pcs") p.copy(unit = "lembar") else p
                    }
                saveProducts(cleaned)
            }
        }
    }

    fun seedInitialData() {
        val initialUsers = listOf(
            User(id = "user_owner", name = "Owner", role = Role.OWNER, pin = "1234"),
            User(id = "user_kasir", name = "Kasir", role = Role.KASIR, pin = "0000")
        )

        val initialProducts = listOf(
            Product(
                id = "prod_v2k",
                name = "V 2K",
                barcode = "899100100201",
                category = "Voucher",
                sellingPrice = 2000L,
                costPrice = 1800L,
                stock = 100.0,
                minStock = 10.0,
                unit = "lembar"
            ),
            Product(
                id = "prod_v5k",
                name = "V 5K",
                barcode = "899100100202",
                category = "Voucher",
                sellingPrice = 5000L,
                costPrice = 4500L,
                stock = 100.0,
                minStock = 10.0,
                unit = "lembar"
            ),
            Product(
                id = "prod_mem50k",
                name = "Member 50K",
                barcode = "899100100203",
                category = "Member",
                sellingPrice = 55000L,
                costPrice = 50000L,
                stock = 100.0,
                minStock = 10.0,
                unit = "lembar"
            )
        )

        val defaultReceipt = ReceiptSettings()

        saveUsers(initialUsers)
        saveCurrentUser(initialUsers[0])
        saveProducts(initialProducts)
        saveTransactions(emptyList())
        savePurchases(emptyList())
        saveExpenses(emptyList())
        saveReceiptSettings(defaultReceipt)

        prefs.edit().putBoolean(KEY_INITIALIZED, true).apply()
    }

    // --- USERS ---
    fun getUsers(): List<User> {
        val jsonStr = prefs.getString(KEY_USERS, null) ?: return emptyList()
        val list = mutableListOf<User>()
        val arr = JSONArray(jsonStr)
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            list.add(
                User(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    role = Role.valueOf(obj.getString("role")),
                    pin = obj.getString("pin")
                )
            )
        }
        return list
    }

    fun saveUsers(users: List<User>) {
        val arr = JSONArray()
        for (u in users) {
            val obj = JSONObject()
            obj.put("id", u.id)
            obj.put("name", u.name)
            obj.put("role", u.role.name)
            obj.put("pin", u.pin)
            arr.put(obj)
        }
        prefs.edit().putString(KEY_USERS, arr.toString()).apply()
    }

    fun getCurrentUser(): User {
        val jsonStr = prefs.getString(KEY_CURRENT_USER, null)
        if (jsonStr != null) {
            try {
                val obj = JSONObject(jsonStr)
                return User(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    role = Role.valueOf(obj.getString("role")),
                    pin = obj.getString("pin")
                )
            } catch (_: Exception) {}
        }
        return getUsers().firstOrNull() ?: User("user_owner", "Owner", Role.OWNER, "1234")
    }

    fun saveCurrentUser(user: User) {
        val obj = JSONObject()
        obj.put("id", user.id)
        obj.put("name", user.name)
        obj.put("role", user.role.name)
        obj.put("pin", user.pin)
        prefs.edit().putString(KEY_CURRENT_USER, obj.toString()).apply()
    }

    // --- PRODUCTS ---
    fun getProducts(): List<Product> {
        val jsonStr = prefs.getString(KEY_PRODUCTS, null) ?: return emptyList()
        val list = mutableListOf<Product>()
        val arr = JSONArray(jsonStr)
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val rawUnit = obj.optString("unit", "lembar")
            val unit = if (rawUnit == "pcs") "lembar" else rawUnit
            list.add(
                Product(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    barcode = obj.getString("barcode"),
                    category = obj.getString("category"),
                    sellingPrice = obj.getLong("sellingPrice"),
                    costPrice = obj.getLong("costPrice"),
                    stock = obj.getDouble("stock"),
                    minStock = obj.optDouble("minStock", 5.0),
                    unit = unit,
                    updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                )
            )
        }
        return list
    }

    fun saveProducts(products: List<Product>) {
        val arr = JSONArray()
        for (p in products) {
            val obj = JSONObject()
            obj.put("id", p.id)
            obj.put("name", p.name)
            obj.put("barcode", p.barcode)
            obj.put("category", p.category)
            obj.put("sellingPrice", p.sellingPrice)
            obj.put("costPrice", p.costPrice)
            obj.put("stock", p.stock)
            obj.put("minStock", p.minStock)
            obj.put("unit", p.unit)
            obj.put("updatedAt", p.updatedAt)
            arr.put(obj)
        }
        prefs.edit().putString(KEY_PRODUCTS, arr.toString()).apply()
    }

    // --- TRANSACTIONS ---
    fun getTransactions(): List<Transaction> {
        val jsonStr = prefs.getString(KEY_TRANSACTIONS, null) ?: return emptyList()
        val list = mutableListOf<Transaction>()
        val arr = JSONArray(jsonStr)
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val itemsArr = obj.getJSONArray("items")
            val items = mutableListOf<CartItem>()
            for (j in 0 until itemsArr.length()) {
                val itmObj = itemsArr.getJSONObject(j)
                val prodObj = itmObj.getJSONObject("product")
                val rawUnit = prodObj.optString("unit", "lembar")
                val unit = if (rawUnit == "pcs") "lembar" else rawUnit
                val prod = Product(
                    id = prodObj.getString("id"),
                    name = prodObj.getString("name"),
                    barcode = prodObj.getString("barcode"),
                    category = prodObj.getString("category"),
                    sellingPrice = prodObj.getLong("sellingPrice"),
                    costPrice = prodObj.getLong("costPrice"),
                    stock = prodObj.getDouble("stock"),
                    minStock = prodObj.optDouble("minStock", 5.0),
                    unit = unit
                )
                items.add(
                    CartItem(
                        product = prod,
                        quantity = itmObj.getDouble("quantity"),
                        itemNote = itmObj.optString("itemNote", "")
                    )
                )
            }

            val instArr = obj.optJSONArray("installments")
            val installments = mutableListOf<InstallmentPayment>()
            if (instArr != null) {
                for (k in 0 until instArr.length()) {
                    val insObj = instArr.getJSONObject(k)
                    installments.add(
                        InstallmentPayment(
                            id = insObj.getString("id"),
                            date = insObj.getLong("date"),
                            amount = insObj.getLong("amount"),
                            paymentMethod = PaymentMethod.valueOf(insObj.getString("paymentMethod")),
                            note = insObj.optString("note", ""),
                            receivedBy = insObj.optString("receivedBy", "")
                        )
                    )
                }
            }

            list.add(
                Transaction(
                    id = obj.getString("id"),
                    timestamp = obj.getLong("timestamp"),
                    cashierName = obj.getString("cashierName"),
                    customerName = obj.optString("customerName", ""),
                    customerPhone = obj.optString("customerPhone", ""),
                    transactionNote = obj.optString("transactionNote", ""),
                    items = items,
                    totalAmount = obj.getLong("totalAmount"),
                    totalCost = obj.getLong("totalCost"),
                    paymentMethod = PaymentMethod.valueOf(obj.getString("paymentMethod")),
                    amountPaid = obj.getLong("amountPaid"),
                    changeAmount = obj.getLong("changeAmount"),
                    remainingBalance = obj.getLong("remainingBalance"),
                    status = PaymentStatus.valueOf(obj.getString("status")),
                    dueDate = if (obj.has("dueDate") && !obj.isNull("dueDate")) obj.getLong("dueDate") else null,
                    installments = installments
                )
            )
        }
        return list
    }

    fun saveTransactions(transactions: List<Transaction>) {
        val arr = JSONArray()
        for (t in transactions) {
            val obj = JSONObject()
            obj.put("id", t.id)
            obj.put("timestamp", t.timestamp)
            obj.put("cashierName", t.cashierName)
            obj.put("customerName", t.customerName)
            obj.put("customerPhone", t.customerPhone)
            obj.put("transactionNote", t.transactionNote)
            obj.put("totalAmount", t.totalAmount)
            obj.put("totalCost", t.totalCost)
            obj.put("paymentMethod", t.paymentMethod.name)
            obj.put("amountPaid", t.amountPaid)
            obj.put("changeAmount", t.changeAmount)
            obj.put("remainingBalance", t.remainingBalance)
            obj.put("status", t.status.name)
            if (t.dueDate != null) obj.put("dueDate", t.dueDate)

            val itemsArr = JSONArray()
            for (item in t.items) {
                val itmObj = JSONObject()
                itmObj.put("quantity", item.quantity)
                itmObj.put("itemNote", item.itemNote)

                val prodObj = JSONObject()
                prodObj.put("id", item.product.id)
                prodObj.put("name", item.product.name)
                prodObj.put("barcode", item.product.barcode)
                prodObj.put("category", item.product.category)
                prodObj.put("sellingPrice", item.product.sellingPrice)
                prodObj.put("costPrice", item.product.costPrice)
                prodObj.put("stock", item.product.stock)
                prodObj.put("minStock", item.product.minStock)
                prodObj.put("unit", item.product.unit)
                itmObj.put("product", prodObj)

                itemsArr.put(itmObj)
            }
            obj.put("items", itemsArr)

            val instArr = JSONArray()
            for (ins in t.installments) {
                val insObj = JSONObject()
                insObj.put("id", ins.id)
                insObj.put("date", ins.date)
                insObj.put("amount", ins.amount)
                insObj.put("paymentMethod", ins.paymentMethod.name)
                insObj.put("note", ins.note)
                insObj.put("receivedBy", ins.receivedBy)
                instArr.put(insObj)
            }
            obj.put("installments", instArr)

            arr.put(obj)
        }
        prefs.edit().putString(KEY_TRANSACTIONS, arr.toString()).apply()
    }

    // --- PURCHASES (KULAKAN) ---
    fun getPurchases(): List<SupplierPurchase> {
        val jsonStr = prefs.getString(KEY_PURCHASES, null) ?: return emptyList()
        val list = mutableListOf<SupplierPurchase>()
        val arr = JSONArray(jsonStr)
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val itemsArr = obj.getJSONArray("items")
            val items = mutableListOf<PurchaseItem>()
            for (j in 0 until itemsArr.length()) {
                val itm = itemsArr.getJSONObject(j)
                items.add(
                    PurchaseItem(
                        productId = itm.getString("productId"),
                        productName = itm.getString("productName"),
                        quantity = itm.getDouble("quantity"),
                        costPrice = itm.getLong("costPrice"),
                        unit = itm.getString("unit")
                    )
                )
            }

            val repayArr = obj.optJSONArray("debtRepayments")
            val debtRepayments = mutableListOf<DebtRepayment>()
            if (repayArr != null) {
                for (k in 0 until repayArr.length()) {
                    val repObj = repayArr.getJSONObject(k)
                    debtRepayments.add(
                        DebtRepayment(
                            id = repObj.getString("id"),
                            date = repObj.getLong("date"),
                            amount = repObj.getLong("amount"),
                            note = repObj.optString("note", "")
                        )
                    )
                }
            }

            list.add(
                SupplierPurchase(
                    id = obj.getString("id"),
                    timestamp = obj.getLong("timestamp"),
                    supplierName = obj.getString("supplierName"),
                    invoiceNumber = obj.getString("invoiceNumber"),
                    items = items,
                    totalAmount = obj.getLong("totalAmount"),
                    amountPaid = obj.getLong("amountPaid"),
                    remainingDebt = obj.getLong("remainingDebt"),
                    status = PaymentStatus.valueOf(obj.getString("status")),
                    dueDate = if (obj.has("dueDate") && !obj.isNull("dueDate")) obj.getLong("dueDate") else null,
                    debtRepayments = debtRepayments
                )
            )
        }
        return list
    }

    fun savePurchases(purchases: List<SupplierPurchase>) {
        val arr = JSONArray()
        for (p in purchases) {
            val obj = JSONObject()
            obj.put("id", p.id)
            obj.put("timestamp", p.timestamp)
            obj.put("supplierName", p.supplierName)
            obj.put("invoiceNumber", p.invoiceNumber)
            obj.put("totalAmount", p.totalAmount)
            obj.put("amountPaid", p.amountPaid)
            obj.put("remainingDebt", p.remainingDebt)
            obj.put("status", p.status.name)
            if (p.dueDate != null) obj.put("dueDate", p.dueDate)

            val itemsArr = JSONArray()
            for (itm in p.items) {
                val itmObj = JSONObject()
                itmObj.put("productId", itm.productId)
                itmObj.put("productName", itm.productName)
                itmObj.put("quantity", itm.quantity)
                itmObj.put("costPrice", itm.costPrice)
                itmObj.put("unit", itm.unit)
                itemsArr.put(itmObj)
            }
            obj.put("items", itemsArr)

            val repArr = JSONArray()
            for (rep in p.debtRepayments) {
                val repObj = JSONObject()
                repObj.put("id", rep.id)
                repObj.put("date", rep.date)
                repObj.put("amount", rep.amount)
                repObj.put("note", rep.note)
                repArr.put(repObj)
            }
            obj.put("debtRepayments", repArr)

            arr.put(obj)
        }
        prefs.edit().putString(KEY_PURCHASES, arr.toString()).apply()
    }

    // --- EXPENSES ---
    fun getExpenses(): List<Expense> {
        val jsonStr = prefs.getString(KEY_EXPENSES, null) ?: return emptyList()
        val list = mutableListOf<Expense>()
        val arr = JSONArray(jsonStr)
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            list.add(
                Expense(
                    id = obj.getString("id"),
                    timestamp = obj.getLong("timestamp"),
                    category = obj.getString("category"),
                    amount = obj.getLong("amount"),
                    note = obj.optString("note", ""),
                    paymentMethod = PaymentMethod.valueOf(obj.getString("paymentMethod")),
                    recordedBy = obj.optString("recordedBy", "")
                )
            )
        }
        return list
    }

    fun saveExpenses(expenses: List<Expense>) {
        val arr = JSONArray()
        for (e in expenses) {
            val obj = JSONObject()
            obj.put("id", e.id)
            obj.put("timestamp", e.timestamp)
            obj.put("category", e.category)
            obj.put("amount", e.amount)
            obj.put("note", e.note)
            obj.put("paymentMethod", e.paymentMethod.name)
            obj.put("recordedBy", e.recordedBy)
            arr.put(obj)
        }
        prefs.edit().putString(KEY_EXPENSES, arr.toString()).apply()
    }

    // --- RECEIPT SETTINGS ---
    fun getReceiptSettings(): ReceiptSettings {
        val jsonStr = prefs.getString(KEY_RECEIPT_SETTINGS, null) ?: return ReceiptSettings()
        return try {
            val obj = JSONObject(jsonStr)
            ReceiptSettings(
                storeName = obj.optString("storeName", "CyberPos"),
                address = obj.optString("address", "Jl. Cyber POS No. 88, Indonesia"),
                phoneNumber = obj.optString("phoneNumber", "081234567890"),
                openingMessage = obj.optString("openingMessage", "Selamat Datang & Terima Kasih atas Kunjungan Anda"),
                closingMessage = obj.optString("closingMessage", "Barang yang sudah dibeli tidak dapat ditukar atau dikembalikan."),
                showAddress = obj.optBoolean("showAddress", true),
                showPhone = obj.optBoolean("showPhone", true),
                showCashier = obj.optBoolean("showCashier", true),
                showNote = obj.optBoolean("showNote", true),
                showRemainingBalance = obj.optBoolean("showRemainingBalance", true),
                paperWidthMm = obj.optInt("paperWidthMm", 58)
            )
        } catch (_: Exception) {
            ReceiptSettings()
        }
    }

    fun saveReceiptSettings(settings: ReceiptSettings) {
        val obj = JSONObject()
        obj.put("storeName", settings.storeName)
        obj.put("address", settings.address)
        obj.put("phoneNumber", settings.phoneNumber)
        obj.put("openingMessage", settings.openingMessage)
        obj.put("closingMessage", settings.closingMessage)
        obj.put("showAddress", settings.showAddress)
        obj.put("showPhone", settings.showPhone)
        obj.put("showCashier", settings.showCashier)
        obj.put("showNote", settings.showNote)
        obj.put("showRemainingBalance", settings.showRemainingBalance)
        obj.put("paperWidthMm", settings.paperWidthMm)
        prefs.edit().putString(KEY_RECEIPT_SETTINGS, obj.toString()).apply()
    }

    // --- BACKUP & RESTORE ---
    fun exportAllToJson(): String {
        val root = JSONObject()
        root.put("version", 1)
        root.put("appName", "CyberPos")
        root.put("exportTime", System.currentTimeMillis())

        root.put("users", JSONArray(prefs.getString(KEY_USERS, "[]")))
        root.put("products", JSONArray(prefs.getString(KEY_PRODUCTS, "[]")))
        root.put("transactions", JSONArray(prefs.getString(KEY_TRANSACTIONS, "[]")))
        root.put("purchases", JSONArray(prefs.getString(KEY_PURCHASES, "[]")))
        root.put("expenses", JSONArray(prefs.getString(KEY_EXPENSES, "[]")))
        root.put("receiptSettings", JSONObject(prefs.getString(KEY_RECEIPT_SETTINGS, "{}")))

        return root.toString(2)
    }

    fun importAllFromJson(jsonString: String): Boolean {
        return try {
            val root = JSONObject(jsonString)
            if (root.has("users")) {
                prefs.edit().putString(KEY_USERS, root.getJSONArray("users").toString()).apply()
            }
            if (root.has("products")) {
                prefs.edit().putString(KEY_PRODUCTS, root.getJSONArray("products").toString()).apply()
            }
            if (root.has("transactions")) {
                prefs.edit().putString(KEY_TRANSACTIONS, root.getJSONArray("transactions").toString()).apply()
            }
            if (root.has("purchases")) {
                prefs.edit().putString(KEY_PURCHASES, root.getJSONArray("purchases").toString()).apply()
            }
            if (root.has("expenses")) {
                prefs.edit().putString(KEY_EXPENSES, root.getJSONArray("expenses").toString()).apply()
            }
            if (root.has("receiptSettings")) {
                prefs.edit().putString(KEY_RECEIPT_SETTINGS, root.getJSONObject("receiptSettings").toString()).apply()
            }
            true
        } catch (_: Exception) {
            false
        }
    }
}
