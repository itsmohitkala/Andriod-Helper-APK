package com.example.poshelper

import fi.iki.elonen.NanoHTTPD
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class LocalBridgeServer(port: Int) : NanoHTTPD("127.0.0.1", port) {

    override fun serve(session: IHTTPSession): Response {
        return try {
            when {
                session.method == Method.OPTIONS -> {
                    val response = newFixedLengthResponse(Response.Status.OK, "text/plain", "")
                    addCorsHeaders(response)
                    response
                }

                session.uri == "/health" -> {
                    val response = newFixedLengthResponse(
                        Response.Status.OK,
                        "application/json",
                        """{"ok":true,"service":"pos-helper"}"""
                    )
                    addCorsHeaders(response)
                    response
                }

                session.uri == "/printer-status" -> {
                    proxyGet("http://127.0.0.1:5618/peri/print/state")
                }

                session.uri == "/cfg" && session.method == Method.GET -> {
                    proxyGet("http://127.0.0.1:5618/cfg")
                }

                else -> {
                    val response = newFixedLengthResponse(
                        Response.Status.NOT_FOUND,
                        "text/plain",
                        "Not Found"
                    )
                    addCorsHeaders(response)
                    response
                }
            }
        } catch (e: Exception) {
            val response = newFixedLengthResponse(
                Response.Status.INTERNAL_ERROR,
                "text/plain",
                "Helper error: ${e.message}"
            )
            addCorsHeaders(response)
            response
        }
    }

    private fun proxyGet(targetUrl: String): Response {
        val url = URL(targetUrl)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 5000
        connection.readTimeout = 5000

        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val reader = BufferedReader(InputStreamReader(stream))
        val responseText = reader.readText()
        reader.close()

        val response = newFixedLengthResponse(
            if (code in 200..299) Response.Status.OK else Response.Status.BAD_REQUEST,
            connection.contentType ?: "text/plain",
            responseText
        )
        addCorsHeaders(response)
        return response
    }

    private fun addCorsHeaders(response: Response) {
        response.addHeader("Access-Control-Allow-Origin", "*")
        response.addHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
        response.addHeader("Access-Control-Allow-Headers", "Content-Type, Authorization")
    }
}