package jetbrains.buildServer.clouds.amazon.sns.trigger.utils

import java.io.BufferedReader
import java.security.Principal
import java.util.*
import javax.servlet.RequestDispatcher
import javax.servlet.ServletInputStream
import javax.servlet.http.Cookie
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpSession

object HttpServletRequestStub : HttpServletRequest {
    private var myHeaders: Map<String, String> = mapOf()

    fun setHeaders(headers: Map<String, String>) {
        myHeaders = headers
    }

    override fun getHeader(name: String?): String? {
        return name?.let { myHeaders[it] }
    }

    override fun getAttribute(name: String?): Any {
        error("Not supported by this stub")
    }

    override fun getAttributeNames(): Enumeration<*> {
        error("Not supported by this stub")
    }

    override fun getCharacterEncoding(): String {
        error("Not supported by this stub")
    }

    override fun getContentLength(): Int {
        error("Not supported by this stub")
    }

    override fun getContentType(): String {
        error("Not supported by this stub")
    }

    override fun getInputStream(): ServletInputStream {
        error("Not supported by this stub")
    }

    override fun getLocale(): Locale {
        error("Not supported by this stub")
    }

    override fun getLocales(): Enumeration<*> {
        error("Not supported by this stub")
    }

    override fun getParameter(name: String?): String {
        error("Not supported by this stub")
    }

    override fun getParameterNames(): Enumeration<*> {
        error("Not supported by this stub")
    }

    override fun getParameterValues(name: String?): Array<String> {
        error("Not supported by this stub")
    }

    override fun getProtocol(): String {
        error("Not supported by this stub")
    }

    override fun getReader(): BufferedReader {
        error("Not supported by this stub")
    }

    @Deprecated("Deprecated in Java")
    override fun getRealPath(path: String?): String {
        error("Not supported by this stub")
    }

    override fun getRemoteAddr(): String {
        error("Not supported by this stub")
    }

    override fun getRemoteHost(): String {
        error("Not supported by this stub")
    }

    override fun getRequestDispatcher(path: String?): RequestDispatcher {
        error("Not supported by this stub")
    }

    override fun getScheme(): String {
        error("Not supported by this stub")
    }

    override fun getServerName(): String {
        error("Not supported by this stub")
    }

    override fun getServerPort(): Int {
        error("Not supported by this stub")
    }

    override fun isSecure(): Boolean {
        error("Not supported by this stub")
    }

    override fun removeAttribute(name: String?) {
        error("Not supported by this stub")
    }

    override fun setAttribute(name: String?, o: Any?) {
        error("Not supported by this stub")
    }

    override fun getAuthType(): String {
        error("Not supported by this stub")
    }

    override fun getContextPath(): String {
        error("Not supported by this stub")
    }

    override fun getCookies(): Array<Cookie> {
        error("Not supported by this stub")
    }

    override fun getDateHeader(name: String?): Long {
        error("Not supported by this stub")
    }

    override fun getHeaderNames(): Enumeration<*> {
        error("Not supported by this stub")
    }

    override fun getHeaders(name: String?): Enumeration<*> {
        error("Not supported by this stub")
    }

    override fun getIntHeader(name: String?): Int {
        error("Not supported by this stub")
    }

    override fun getMethod(): String {
        error("Not supported by this stub")
    }

    override fun getPathInfo(): String {
        error("Not supported by this stub")
    }

    override fun getPathTranslated(): String {
        error("Not supported by this stub")
    }

    override fun getQueryString(): String {
        error("Not supported by this stub")
    }

    override fun getRemoteUser(): String {
        error("Not supported by this stub")
    }

    override fun getRequestURI(): String {
        error("Not supported by this stub")
    }

    override fun getRequestedSessionId(): String {
        error("Not supported by this stub")
    }

    override fun getServletPath(): String {
        error("Not supported by this stub")
    }

    override fun getSession(): HttpSession {
        error("Not supported by this stub")
    }

    override fun getSession(create: Boolean): HttpSession {
        error("Not supported by this stub")
    }

    override fun getUserPrincipal(): Principal {
        error("Not supported by this stub")
    }

    override fun isRequestedSessionIdFromCookie(): Boolean {
        error("Not supported by this stub")
    }

    override fun isRequestedSessionIdFromURL(): Boolean {
        error("Not supported by this stub")
    }

    @Deprecated("Deprecated in Java")
    override fun isRequestedSessionIdFromUrl(): Boolean {
        error("Not supported by this stub")
    }

    override fun isRequestedSessionIdValid(): Boolean {
        error("Not supported by this stub")
    }

    override fun isUserInRole(role: String?): Boolean {
        error("Not supported by this stub")
    }
}
