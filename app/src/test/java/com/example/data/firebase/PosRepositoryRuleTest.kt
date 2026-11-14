package com.example.data.firebase

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.DiscountType
import com.example.data.model.Expense
import com.example.data.model.ExpenseCategory
import com.example.data.model.PaymentMethod
import com.example.data.model.PosCalculator
import com.example.data.model.PosTransaction
import com.example.data.model.Product
import com.example.data.model.TaxType
import com.example.data.model.TransactionItem
import com.example.data.model.TransactionStatus
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class PosRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun ownerCanSaveProductCompleteTransactionAndCancelOnce(): Unit = runBlocking {
        val uid = signInTestUser("owner_${UUID.randomUUID()}@test.com")
        val repo = PosRepository(firestore)

        withTimeout(5000L) {
            repo.ensureInitialUserSetup(uid, "Rafline", defaultDarkTheme = false, defaultLanguage = "id")
        }

        val prodId = withTimeout(5000L) {
            repo.saveProduct(
                uid,
                Product(
                    userId = uid,
                    name = "Beras Pandan Wangi",
                    sku = "BPW-01",
                    category = "Sembako",
                    unit = "Kg",
                    costPrice = 14000.0,
                    sellPrice = 20000.0,
                    stock = 10.0,
                    minStock = 2.0
                )
            ).getOrThrow()
        }

        val txId = "tx_" + UUID.randomUUID().toString().replace("-", "").take(12)
        val totals = PosCalculator.calculateTotals(
            subtotal = 10000.0,
            discountType = DiscountType.NONE,
            discountValue = 0.0,
            taxType = TaxType.NONE,
            autoTaxPercent = 11.0,
            manualTaxAmount = 0.0,
            amountPaid = 10000.0
        )

        val tx = PosTransaction(
            id = txId,
            userId = uid,
            transactionNumber = "AK-TEST-001",
            customerName = "",
            items = listOf(
                TransactionItem(
                    productId = prodId,
                    productName = "Beras Pandan Wangi",
                    unit = "Kg",
                    unitPrice = 20000.0,
                    costPrice = 14000.0,
                    quantity = 0.5,
                    subtotal = 10000.0
                )
            ),
            subtotal = totals.subtotal,
            discountType = DiscountType.NONE.code,
            discountValue = 0.0,
            discountAmount = totals.discountAmount,
            taxType = TaxType.NONE.code,
            taxPercent = 0.0,
            taxAmount = totals.taxAmount,
            totalAmount = totals.grandTotal,
            paymentMethod = PaymentMethod.CASH.code,
            amountPaid = 10000.0,
            changeAmount = totals.changeAmount,
            transactionMillis = System.currentTimeMillis()
        )

        withTimeout(5000L) {
            repo.completeTransaction(uid, tx).getOrThrow()
            // Idempotent repeat call must not deduct stock a second time
            repo.completeTransaction(uid, tx).getOrThrow()
        }

        val afterCheckout = withTimeout(5000L) {
            repo.observeProducts(uid).first { list -> list.any { it.id == prodId } }
        }
        val prodAfterCheckout = afterCheckout.first { it.id == prodId }
        assertEquals(9.5, prodAfterCheckout.stock, 0.001)

        // Cancel once restores 0.5 Kg -> back to 10.0
        withTimeout(5000L) {
            repo.cancelOrRefundTransaction(uid, txId, TransactionStatus.CANCELLED, "Batal uji").getOrThrow()
        }
        val afterCancel = withTimeout(5000L) {
            repo.observeProducts(uid).first { list ->
                list.firstOrNull { it.id == prodId }?.stock == 10.0
            }
        }
        assertEquals(10.0, afterCancel.first { it.id == prodId }.stock, 0.001)

        // Second cancel/refund must fail (no double stock restore)
        val secondAttempt = withTimeout(5000L) {
            repo.cancelOrRefundTransaction(uid, txId, TransactionStatus.REFUNDED, "Uji ganda")
        }
        assertTrue(secondAttempt.isFailure)
    }

    @Test
    fun crossUserAccessIsRejected(): Unit = runBlocking {
        val aliceUid = signInTestUser("alice_${UUID.randomUUID()}@test.com")
        val repo = PosRepository(firestore)
        withTimeout(5000L) {
            repo.saveProduct(
                aliceUid,
                Product(
                    userId = aliceUid,
                    name = "Kopi Hitam",
                    category = "Minuman",
                    unit = "Pcs",
                    sellPrice = 5000.0,
                    stock = 10.0
                )
            ).getOrThrow()
        }

        signInTestUser("bob_${UUID.randomUUID()}@test.com")
        var caught: Throwable? = null
        try {
            withTimeout(3000L) {
                repo.observeProducts(aliceUid).first()
            }
        } catch (e: Throwable) {
            caught = e
        }
        val fsEx = caught?.unwrapFirestoreException()
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
    }

    @Test
    fun unauthenticatedAccessIsRejected(): Unit = runBlocking {
        auth.signOut()
        val repo = PosRepository(firestore)
        var caught: Throwable? = null
        try {
            withTimeout(3000L) {
                repo.observeProducts("some_user_123").first()
            }
        } catch (e: Throwable) {
            caught = e
        }
        val fsEx = caught?.unwrapFirestoreException()
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
    }

    @Test
    fun ownerCanSaveObserveAndDeleteExpense_crossUserRejected(): Unit = runBlocking {
        val aliceUid = signInTestUser("exp_alice_${UUID.randomUUID()}@test.com")
        val repo = PosRepository(firestore)

        val expId = withTimeout(5000L) {
            repo.saveExpense(
                aliceUid,
                Expense(
                    userId = aliceUid,
                    title = "Beli Kantong Kresek",
                    category = ExpenseCategory.OPERATIONAL.labelId,
                    amount = 35000.0,
                    notes = "2 pack",
                    expenseMillis = System.currentTimeMillis()
                )
            ).getOrThrow()
        }

        val observed = withTimeout(5000L) {
            repo.observeExpenses(aliceUid).first { list -> list.any { it.id == expId } }
        }
        val saved = observed.first { it.id == expId }
        assertEquals("Beli Kantong Kresek", saved.title)
        assertEquals(35000.0, saved.amount, 0.001)

        withTimeout(5000L) {
            repo.deleteExpense(aliceUid, expId).getOrThrow()
        }
        val afterDelete = withTimeout(5000L) {
            repo.observeExpenses(aliceUid).first { list -> list.none { it.id == expId } }
        }
        assertTrue(afterDelete.none { it.id == expId })

        signInTestUser("exp_bob_${UUID.randomUUID()}@test.com")
        var caught: Throwable? = null
        try {
            withTimeout(3000L) {
                repo.observeExpenses(aliceUid).first()
            }
        } catch (e: Throwable) {
            caught = e
        }
        val fsEx = caught?.unwrapFirestoreException()
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
    }

    @Test
    fun receiptCustomizationAndMonitorModePermissionsWorkEndToEnd(): Unit = runBlocking {
        val cashierEmail = "cashier_${UUID.randomUUID()}@test.com"
        val ownerMonitorEmail = "owner_monitor_${UUID.randomUUID()}@test.com"
        val cashierUid = signInTestUser(cashierEmail)
        val repo = PosRepository(firestore)

        withTimeout(5000L) {
            repo.ensureInitialUserSetup(
                userId = cashierUid,
                ownerDisplayName = "Kasir Pagi",
                ownerEmail = cashierEmail,
                defaultDarkTheme = false,
                defaultLanguage = "id"
            )
            repo.saveReceiptCustomization(
                userId = cashierUid,
                showLogo = false,
                showCashierName = true,
                showStorePhone = false,
                showStoreAddress = true,
                showCustomerName = false,
                footerMessage = "Simpan struk ini"
            ).getOrThrow()

            repo.saveMonitorEmails(
                userId = cashierUid,
                ownerEmail = cashierEmail,
                storeName = "Toko Rafline",
                emails = listOf(ownerMonitorEmail)
            ).getOrThrow()
        }

        val updatedSettings = withTimeout(5000L) {
            repo.observeSettings(cashierUid).first { it.receiptFooterMessage == "Simpan struk ini" }
        }
        assertEquals(false, updatedSettings.showReceiptLogo)
        assertEquals(true, updatedSettings.showReceiptCashierName)
        assertEquals(false, updatedSettings.showReceiptStorePhone)
        assertEquals(true, updatedSettings.showReceiptStoreAddress)
        assertEquals(false, updatedSettings.showReceiptCustomerName)
        assertTrue(updatedSettings.monitorEmails.contains(ownerMonitorEmail.lowercase()))

        // Switch auth user to the authorized Owner Monitor account
        signInTestUser(ownerMonitorEmail)

        val linkedStores = withTimeout(5000L) {
            repo.observeAuthorizedStoresForMonitor(ownerMonitorEmail).first { list ->
                list.any { it.userId == cashierUid }
            }
        }
        assertTrue(linkedStores.any { it.userId == cashierUid })

        // Monitor can read cashier's store settings (read-only)
        val monitoredStore = withTimeout(5000L) {
            repo.observeSettings(cashierUid).first { it.userId == cashierUid }
        }
        assertEquals("Simpan struk ini", monitoredStore.receiptFooterMessage)

        // Monitor MUST be blocked by repository client-side guard AND Firestore server rules
        val writeAttempt = withTimeout(5000L) {
            repo.saveProduct(
                cashierUid,
                Product(
                    userId = cashierUid,
                    name = "Unauthorized Item",
                    category = "Makanan",
                    unit = "Pcs",
                    sellPrice = 1000.0,
                    stock = 1.0
                )
            )
        }
        assertTrue(writeAttempt.isFailure)

        var serverCaught: Throwable? = null
        try {
            withTimeout(5000L) {
                firestore.collection("users").document(cashierUid)
                    .collection("products").document("unauth_prod")
                    .set(mapOf("userId" to cashierUid, "name" to "Hack"))
                    .await()
            }
        } catch (e: Throwable) {
            serverCaught = e
        }
        assertEquals(
            FirebaseFirestoreException.Code.PERMISSION_DENIED,
            serverCaught?.unwrapFirestoreException()?.code
        )
    }
}
