package com.example.data

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class FinanceRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun addTransaction_and_observeTransactions_authenticatedOwner_succeeds() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = FinanceRepository(firestore)
        val uniqueId = "tx_${UUID.randomUUID().toString().replace("-", "").take(16)}"

        val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.addTransaction(
                FinancialTransaction(
                    id = uniqueId,
                    userId = uid,
                    title = "فطور عمل",
                    amount = 180.0,
                    type = "expense",
                    category = "مطاعم وكافيهات",
                    priority = "luxury",
                    recurrence = "variable",
                    wallet = "cash",
                    txDateEpochMs = System.currentTimeMillis()
                )
            )
        }
        assertTrue(createResult.isSuccess)

        val emitted = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeTransactions(uid).first { list -> list.any { it.id == uniqueId } }
        }
        assertTrue(emitted.any { it.id == uniqueId && it.title == "فطور عمل" })
    }

    @Test
    fun getTransactionById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = FinanceRepository(firestore)
        val uniqueId = "tx_${UUID.randomUUID().toString().replace("-", "").take(16)}"

        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.addTransaction(
                FinancialTransaction(
                    id = uniqueId,
                    userId = aliceUid,
                    title = "سوبر ماركت",
                    amount = 420.0,
                    type = "expense",
                    category = "طعام ومشتريات",
                    priority = "essential",
                    recurrence = "variable",
                    wallet = "bank",
                    txDateEpochMs = System.currentTimeMillis()
                )
            ).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = FinanceRepository(firestore)
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                bobRepo.getTransactionById(aliceUid, uniqueId)
            }
            fail("Expected FirebaseFirestoreException PERMISSION_DENIED for cross-user access")
        } catch (e: Exception) {
            val fsEx = e.unwrapFirestoreException()
            assertNotNull("Expected nested FirebaseFirestoreException, got $e", fsEx)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
        }
    }

    @Test
    fun observeTransactions_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = FinanceRepository(firestore)
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeTransactions("unauth_target_uid").first()
            }
            fail("Expected PERMISSION_DENIED when unauthenticated")
        } catch (e: Exception) {
            val fsEx = e.unwrapFirestoreException()
            assertNotNull("Expected nested FirebaseFirestoreException, got $e", fsEx)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
        }
    }

    private companion object {
        const val ALICE_EMAIL = "alice@mizan.test"
        const val BOB_EMAIL = "bob@mizan.test"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
