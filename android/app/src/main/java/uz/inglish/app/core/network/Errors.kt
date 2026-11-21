package uz.inglish.app.core.network

import java.io.IOException

/**
 * Turns transport and API failures into short, actionable Uzbek messages.
 * Server messages are English and often too technical for the learner UI.
 */
fun Throwable.userMessage(fallback: String = "Xatolik yuz berdi. Qayta urinib ko'ring."): String =
    when (this) {
        is SessionExpiredException -> "Sessiya muddati tugagan. Qaytadan kiring."
        is ApiException -> when (code) {
            "QUOTA_EXCEEDED" -> "Bu oy/kun uchun limitingiz tugagan. Keyinroq qayta urinib ko'ring."
            "SCORING_IN_PROGRESS" -> "Javob allaqachon baholanmoqda. Bir necha soniyadan keyin tekshiring."
            "SUBMISSION_TOO_SHORT" -> "Javob juda qisqa. Kamida 20 so'z yozing."
            "AI_SCORING_FAILED" -> "AI baholashda xatolik yuz berdi. Qayta urinib ko'ring."
            "PURCHASE_ALREADY_CLAIMED" -> "Bu xarid boshqa akkauntga bog'langan."
            "PURCHASE_INVALID", "PURCHASE_NOT_FOUND" -> "Xaridni tasdiqlab bo'lmadi. Play hisobingizni tekshiring."
            "BILLING_VERIFY_FAILED" -> "To'lovni tekshirishda xatolik. Birozdan keyin qayta urinib ko'ring."
            "BILLING_NOT_CONFIGURED" -> "To'lov tizimi hozircha sozlanmagan."
            "CODE_INVALID" -> "Kod noto'g'ri. Qayta tekshiring."
            "CODE_EXPIRED" -> "Kod muddati tugagan. Yangi kod so'rang."
            "CODE_LOCKED" -> "Juda ko'p urinish. Yangi kod so'rang."
            "UNAUTHORIZED" -> "Sessiya muddati tugagan. Qaytadan kiring."
            else -> message?.takeIf { it.isNotBlank() } ?: fallback
        }
        is IOException -> "Serverga ulanib bo'lmadi. Internetni tekshiring."
        else -> message?.takeIf { it.isNotBlank() } ?: fallback
    }
