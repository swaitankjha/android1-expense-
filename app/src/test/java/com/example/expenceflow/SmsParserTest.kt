package com.example.expenceflow

import com.example.expenceflow.data.auto.SmsParser
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SmsParserTest {

    private lateinit var smsParser: SmsParser

    @Before
    fun setUp() {
        smsParser = SmsParser()
    }

    @Test
    fun testSbiDebitSms() {
        val sms = "Dear SBI User, A/c X1234 debited by Rs.250.00 on 15Jan25 trf to ZOMATO. Ref: 123456."
        val candidate = smsParser.parse(sms, "VM-SBI")
        assertNotNull(candidate)
        assertEquals(250.0, candidate!!.amount, 0.01)
        assertEquals("Expense", candidate.type)
        assertEquals("Zomato", candidate.merchant)
        assertEquals("Bank (SBI)", candidate.account)
    }

    @Test
    fun testHdfcSpentSms() {
        val sms = "Rs 1,500.00 debited from HDFC Bank A/C XX5678 on 15-01-25 to VPA swiggy@icici."
        val candidate = smsParser.parse(sms, "AD-HDFCBK")
        assertNotNull(candidate)
        assertEquals(1500.0, candidate!!.amount, 0.01)
        assertEquals("Expense", candidate.type)
        assertEquals("Swiggy", candidate.merchant)
        assertEquals("Bank (HDFC)", candidate.account)
    }

    @Test
    fun testIciciCreditSms() {
        val sms = "Dear Customer, Rs 2,500.00 credited to your A/c XX4321 on 15-Jan-25 by transfer from ALICE."
        val candidate = smsParser.parse(sms, "JM-ICICIB")
        assertNotNull(candidate)
        assertEquals(2500.0, candidate!!.amount, 0.01)
        assertEquals("Income", candidate.type)
        assertEquals("Alice", candidate.merchant)
        assertEquals("Bank (ICICI)", candidate.account)
    }

    @Test
    fun testUpiPaidSms() {
        val sms = "Paid ₹450 to Chai Point using UPI."
        val candidate = smsParser.parse(sms, "AX-UPI")
        assertNotNull(candidate)
        assertEquals(450.0, candidate!!.amount, 0.01)
        assertEquals("Expense", candidate.type)
        assertEquals("Chai Point", candidate.merchant)
    }

    @Test
    fun testOtpSmsIgnored() {
        val sms = "Your OTP for net banking login is 482910. Do not share with anyone."
        val candidate = smsParser.parse(sms, "VM-SBI")
        assertNull(candidate)
    }
}
