package com.example.synccontrol
import android.widget.TextView
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI

// initialize websocket client
class WebSocketClient(serverUri: URI, private val messageListener: (String ) -> Unit) : WebSocketClient(serverUri) {

    override fun onOpen(handshakedata: ServerHandshake?) {

        println("CONNECTED")
        messageListener.invoke("CONNECTED")
        // When WebSocket connection opened
    }

    override fun onClose(code: Int, reason: String?, remote: Boolean) {
        println("CLOSE")

        messageListener.invoke("DISCONNECTED")

        // When WebSocket connection closed
    }

    override fun onMessage(message: String?) {
        // When Receive a message
        messageListener.invoke(message ?: "")
    }

    override fun onError(ex: Exception?) {
        // When An error occurred
        println("Error")
        messageListener.invoke("DISCONNECTED")

    }

    fun sendMessage(message: String) {
        send(message)
    }
}