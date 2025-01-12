package org.ktech.ps4jailbreak

import android.content.Context
import android.util.Log
import fi.iki.elonen.NanoHTTPD
import java.io.InputStream
import java.net.Socket

//Initialize NanoHTTPD server on port 8080
//TODO: Allow user to configure custom port to run the server on
class NanoServer(current: Context): NanoHTTPD(8080) {
    //Store context so we can access assets from main activity
    var context: Context = current
    //Store last PS4 ip address
    var lastPS4: String? = null

    //Handle connection from (hopefully) PS4
    //TODO: Check if device is a PS4 and if it is running Firmware 9.0
    override fun serve(session: IHTTPSession?): Response {
        //Retrieve client IP address from the request headers. Seems to be the only way possible with
        // NanoHTTPD

        //Check if client is a PS4 running firmware 9.0 by looking for string "PlayStation 4/9.00"
        //in "user-agent" header.
        if (session?.headers?.get("user-agent")?.contains("PlayStation 4/9.00") == false) {
            onLogMessage("Non-PS4_FW9.0 client connected.")
            return NanoHTTPD.Response(NanoHTTPD.Response.Status.OK, "text/html", """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <title>Client Mismatch</title>
                </head>
                <body>
                    <h1>Run this on a PS4 running firmware 9.00 only!</h1>
                </body>
                </html>
            """)
        }

        val clientIP: String = session?.headers?.get("http-client-ip").toString()
        val requestURL: String = session?.uri.toString()

        if (lastPS4 != clientIP) {
            onLogMessage("PS4 running firmware 9.00 connected with IP Address $clientIP")
            lastPS4 = clientIP
            onLastPS4Changed()
        } else {
            onLogMessage("PS4 browser requesting -> $requestURL")
        }

        //React to request uri path
        when (requestURL) {
            //Return index.html to client/PS4
            "/" -> {
                return NanoHTTPD.Response(NanoHTTPD.Response.Status.OK, "text/html", getResourceAsText("index.html"))
            }
            //When /log/done is received from show the message to user
            "/log/done" -> {
                onLogMessage("____________________________")
                onLogMessage("!!!Exploit Complete!!!")
                onLogMessage("____________________________")
                onLogMessage("Enable binloader in GoldHen to send payloads.")
            }
            else -> {
                //This is a hack to serve all the static files in the assets folder
                val path = requestURL.drop(1)
                when {
                    requestURL.endsWith(".html") -> {
                        return NanoHTTPD.Response(NanoHTTPD.Response.Status.OK, "text/html", getResourceAsText(path))
                        //Else if the request path ends with .js then return the javascript files with the correct mime type
                    }
                    requestURL.endsWith(".js") -> {
                        return NanoHTTPD.Response(NanoHTTPD.Response.Status.OK, "text/javascript ", getResourceAsText(path))
                    }
                    requestURL.endsWith(".bin") -> {
                        return NanoHTTPD.Response(NanoHTTPD.Response.Status.OK, "text/cache-manifest", getResourceAsText(path))
                    }
                    requestURL.endsWith(".cache") -> {
                        return NanoHTTPD.Response(NanoHTTPD.Response.Status.OK, "application/octet-stream", getResourceAsText(path))
                    }
                }
            }
        }
        return super.serve(session)
    }

    //Create event to send log messages
    var onLogMessage: ((String) -> Unit) = {}
    //Create event to notify of new PS4 ip address
    var onLastPS4Changed: (() -> Unit) = {}

    //get the string contents of a resource from the assets folder using its path
    private fun getResourceAsText(path: String): String {
        return context.assets.open(path).reader().readText()
    }

    //get the bytes contents of a resource from the assets folder using its path
    private fun getResourceAsBytes(path: String): ByteArray {
        return context.assets.open(path).readBytes()
    }

}