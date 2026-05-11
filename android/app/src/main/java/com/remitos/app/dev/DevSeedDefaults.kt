package com.remitos.app.dev

import com.remitos.app.BuildConfig

/**
 * Login / device-setup defaults for local development against
 * `backend/db/seed/local_dev_users.sql` (password: LocalSeed123!).
 *
 * Release builds use empty company/password and generic username hints (`admin` / `operador`).
 */
object DevSeedDefaults {
    const val COMPANY_CODE = "SEEDTRIAL"
    const val OWNER_USERNAME = "trial_owner"
    const val OPERATOR_USERNAME = "trial_operator"
    const val PASSWORD = "LocalSeed123!"

    val prefillCompany: String
        get() = if (BuildConfig.DEBUG) COMPANY_CODE else ""

    val prefillOwnerUsername: String
        get() = if (BuildConfig.DEBUG) OWNER_USERNAME else "admin"

    val prefillOperatorUsername: String
        get() = if (BuildConfig.DEBUG) OPERATOR_USERNAME else "operador"

    val prefillPassword: String
        get() = if (BuildConfig.DEBUG) PASSWORD else ""

    fun isSeedOwnerForDemoData(userId: String): Boolean {
        if (!BuildConfig.DEBUG) return false
        val u = userId.lowercase()
        return u == OWNER_USERNAME.lowercase() || u == "admin"
    }
}
