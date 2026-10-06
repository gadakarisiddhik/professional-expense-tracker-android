package com.example.utils

import android.content.Context
import android.os.Build
import java.security.MessageDigest

object SecurityUtils {

    private const val SALT = "ai_studio_expense_tracker_salt_2026"

    /**
     * Hashes a 4-6 digit user PIN safely with SHA-256 and constant salt.
     */
    fun hashPin(pin: String): String {
        val input = "$SALT:$pin"
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Verifies if input pin matches stored hash.
     */
    fun verifyPin(inputPin: String, storedHash: String): Boolean {
        if (storedHash.isBlank()) return false
        return hashPin(inputPin) == storedHash
    }

    /**
     * Checks if biometric hardware and enrollment are supported on this device.
     */
    fun isBiometricAvailable(context: Context): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val biometricManager = context.getSystemService(android.hardware.biometrics.BiometricManager::class.java)
                biometricManager != null
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }
}
