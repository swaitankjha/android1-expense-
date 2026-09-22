package com.example.expenceflow.data.auto

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategorizationEngine @Inject constructor() {

    private val merchantToCategory = mapOf(
        "zomato" to "Food",
        "swiggy" to "Food",
        "uber" to "Transport",
        "ola" to "Transport",
        "amazon" to "Shopping",
        "flipkart" to "Shopping",
        "apollo" to "Health",
        "myntra" to "Shopping",
        "irctc" to "Travel",
        "bookmyshow" to "Entertainment",
        "netflix" to "Entertainment",
        "spotify" to "Entertainment",
        "airtel" to "Bills",
        "jio" to "Bills",
        "bescom" to "Bills",
        "lic" to "Bills",
        "starbucks" to "Food",
        "mcdonalds" to "Food"
    )

    fun categorize(merchant: String): String {
        val lowerMerchant = merchant.lowercase()
        for ((key, category) in merchantToCategory) {
            if (lowerMerchant.contains(key)) {
                return category
            }
        }
        return "Other"
    }
}
