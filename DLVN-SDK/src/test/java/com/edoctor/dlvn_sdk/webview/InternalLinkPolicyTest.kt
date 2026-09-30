package com.edoctor.dlvn_sdk.webview

import com.edoctor.dlvn_sdk.Constants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InternalLinkPolicyTest {
    @Test
    fun environmentUrlsUseNewHostsAndKeepHealthConsultationPath() {
        assertEquals("https://kh.daiichilife.com.vn/tu-van-suc-khoe", Constants.healthConsultantUrlProd)
        assertEquals("https://khuat.daiichilife.com.vn/tu-van-suc-khoe", Constants.healthConsultantUrlDev)
    }

    @Test
    fun acceptsOnlyDaiichiHostsAndDotSeparatedSubdomains() {
        listOf(
            Constants.healthConsultantUrlProd,
            Constants.healthConsultantUrlDev,
            "https://daiichilife.com.vn",
            "https://campaign.daiichilife.com.vn/path?mode=rating#section",
            "https://nested.kh.daiichilife.com.vn:443/tu-van-suc-khoe",
            "HTTPS://KH.DAIICHILIFE.COM.VN/tu-van-suc-khoe",
            "http://kh.daiichilife.com.vn/tu-van-suc-khoe"
        ).forEach { url -> assertTrue(url, isInternalDaiichiUrl(url)) }
    }

    @Test
    fun rejectsSpoofedHostsAndDomainTextOutsideHostname() {
        listOf(
            "https://notdaiichilife.com.vn/",
            "https://daiichilife.com.vn.evil.example/",
            "https://kh.daiichilife.com.vn.evil.example/",
            "https://daiichilife.com.vn@evil.example/",
            "https://evil.example/daiichilife.com.vn",
            "https://evil.example/tu-van-suc-khoe",
            "https://evil.example/?next=https://kh.daiichilife.com.vn",
            "https://evil.example/#kh.daiichilife.com.vn",
            "https://kh.dai-ichi-life.com.vn/tu-van-suc-khoe",
            "https://khuat.dai-ichi-life.com.vn/tu-van-suc-khoe",
            "https://evil.example\\@kh.daiichilife.com.vn/",
            "https://evil.example%2F@kh.daiichilife.com.vn%2Fevil.example/",
            "https://kh.daiichilife.com.vn%00.evil.example/",
            "javascript://kh.daiichilife.com.vn/",
            "file://kh.daiichilife.com.vn/",
            "//kh.daiichilife.com.vn/tu-van-suc-khoe",
            "/tu-van-suc-khoe",
            "not a URL",
            "",
            null
        ).forEach { url -> assertFalse(url, isInternalDaiichiUrl(url)) }
    }
}
