package com.example.data.firebase

import android.content.Context
import com.example.R
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

enum class LicenseErrorCode {
    EMPTY_BUSINESS_NAME,
    INVALID_CODE_FORMAT,
    CODE_NOT_FOUND,
    CODE_ALREADY_USED,
    NETWORK_ERROR,
    UNKNOWN
}

class LicenseActivationException(
    val code: LicenseErrorCode,
    override val message: String
) : Exception(message)

data class LicenseActivationResult(
    val licenseCode: String,
    val businessName: String,
    val deviceId: String,
    val activatedAtMillis: Long
)

class LicenseActivationRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    companion object {
        private val CODE_REGEX = Regex("^[A-Z0-9_\\-]{4,64}$")
        const val ADMIN_SUPPORT_EMAIL = "rendykurniawan0311@gmail.com"
    }

    /**
     * Verifies activation_codes/{code} in Firestore and atomically marks it as "used"
     * bound to this single deviceId & businessName. Requires internet ONCE at initial activation.
     */
    suspend fun activateLicense(
        businessName: String,
        rawCode: String,
        deviceId: String
    ): Result<LicenseActivationResult> {
        val cleanBusiness = businessName.trim().take(100)
        if (cleanBusiness.isBlank()) {
            return Result.failure(
                LicenseActivationException(
                    LicenseErrorCode.EMPTY_BUSINESS_NAME,
                    "Nama usaha wajib diisi."
                )
            )
        }

        val normalizedCode = rawCode.trim().uppercase()
        if (!CODE_REGEX.matches(normalizedCode)) {
            return Result.failure(
                LicenseActivationException(
                    LicenseErrorCode.INVALID_CODE_FORMAT,
                    "Format kode aktivasi tidak valid (minimal 4 karakter huruf kapital, angka, atau tanda hubung)."
                )
            )
        }

        val cleanDeviceId = deviceId.trim().take(128)
        if (cleanDeviceId.length < 4) {
            return Result.failure(
                LicenseActivationException(
                    LicenseErrorCode.UNKNOWN,
                    "ID perangkat tidak dapat dibaca. Coba ulangi."
                )
            )
        }

        val docRef = db.collection("activation_codes").document(normalizedCode)

        return try {
            db.runTransaction { tx ->
                val snapshot = tx.get(docRef)
                if (!snapshot.exists()) {
                    throw LicenseActivationException(
                        LicenseErrorCode.CODE_NOT_FOUND,
                        "Kode aktivasi tidak ditemukan. Periksa kembali kode Anda."
                    )
                }

                val status = snapshot.getString("status") ?: ""
                if (status != "available") {
                    throw LicenseActivationException(
                        LicenseErrorCode.CODE_ALREADY_USED,
                        "Kode aktivasi sudah digunakan di perangkat lain."
                    )
                }

                tx.update(
                    docRef,
                    mapOf(
                        "status" to "used",
                        "usedByBusinessName" to cleanBusiness,
                        "registeredDeviceId" to cleanDeviceId,
                        "usedAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()

            Result.success(
                LicenseActivationResult(
                    licenseCode = normalizedCode,
                    businessName = cleanBusiness,
                    deviceId = cleanDeviceId,
                    activatedAtMillis = System.currentTimeMillis()
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: LicenseActivationException) {
            Result.failure(e)
        } catch (e: FirebaseFirestoreException) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            val mapped = when (e.code) {
                FirebaseFirestoreException.Code.PERMISSION_DENIED -> LicenseActivationException(
                    LicenseErrorCode.CODE_ALREADY_USED,
                    "Kode aktivasi sudah digunakan di perangkat lain atau tidak tersedia."
                )
                FirebaseFirestoreException.Code.UNAVAILABLE,
                FirebaseFirestoreException.Code.DEADLINE_EXCEEDED -> LicenseActivationException(
                    LicenseErrorCode.NETWORK_ERROR,
                    "Tidak dapat terhubung ke server. Pastikan koneksi internet aktif untuk aktivasi pertama kali."
                )
                else -> LicenseActivationException(
                    LicenseErrorCode.UNKNOWN,
                    e.localizedMessage ?: "Gagal memverifikasi kode aktivasi."
                )
            }
            Result.failure(mapped)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(
                LicenseActivationException(
                    LicenseErrorCode.NETWORK_ERROR,
                    e.localizedMessage ?: "Gagal memverifikasi kode aktivasi. Periksa koneksi internet Anda."
                )
            )
        }
    }
}
