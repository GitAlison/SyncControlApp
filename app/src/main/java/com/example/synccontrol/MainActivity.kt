package com.example.synccontrol

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.InputDevice
import android.view.KeyEvent
import android.view.View
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import java.net.URI


class MainActivity : AppCompatActivity() {
    private var controllerInputEventListener: ControllerHelper.ControllerInputEventListener? = null

    private val buttonsPressed: MutableList<Int> = mutableListOf()
    private val keyBindings = HashMap<String, String>()
    private lateinit var vtvLogButton: TextView
    private lateinit var deviceId: EditText
    private lateinit var textViewConnectionStatus: TextView
    private lateinit var webSocketClient: WebSocketClient


    val controllerHelper = ControllerHelper()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        vtvLogButton = findViewById<TextView>(R.id.tv_log_button);
        deviceId = findViewById<EditText>(R.id.editTextDeviceCode)
        textViewConnectionStatus = findViewById<TextView>(R.id.textViewConnectionStatus)


        getData()
        connectToServer()
    }

    fun connectToServer(){
        val serverUri = URI(deviceId.text.toString())

        webSocketClient = WebSocketClient(serverUri ) { message ->
            if (message == "CONNECTED") {
                textViewConnectionStatus.text = "Connected to server"
                textViewConnectionStatus.setTextColor(Color.GREEN);
            }else if(message == "DISCONNECTED"){
                textViewConnectionStatus.text = "Disconnected"
                textViewConnectionStatus.setTextColor(Color.RED);
            }else {
                //println("mensagem $message")
            }
        }

        webSocketClient.connect()
    }
    fun getData(){
        var sharedPref = getSharedPreferences("settingsApp", Context.MODE_PRIVATE)
        val serverIp: String? = sharedPref.getString("serverIp", "ws://192.168.1.2:9999");
        deviceId.setText(serverIp);
    }
    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        try {
            buttonsPressed.remove(keyCode)
            sendKeyUp(keyCode)
            vtvLogButton.text = buttonsPressed.toString()

        }catch(e: Exception){
            println("exception: $e")
        }

        return super.onKeyUp(keyCode, event)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {

        if (keyCode == KeyEvent.KEYCODE_BACK  ||
            keyCode == KeyEvent.KEYCODE_SPACE ||
            keyCode == KeyEvent.KEYCODE_DEL ||
            keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
            return true;
        }

        if (event.source and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD || event.source and InputDevice.SOURCE_JOYSTICK
                    == InputDevice.SOURCE_JOYSTICK) {

            var tvLogButton:TextView = findViewById<TextView>(R.id.tv_log_button);

            var keyEventName:String = KeyEvent.keyCodeToString(keyCode);

            if (!buttonsPressed.contains(event.keyCode)){
                buttonsPressed.add(event.keyCode)
                try {
                    sendKeyDown()
                }catch(e: Exception){
                    webSocketClient.reconnect()
                    println("exception: $e")
                }

            }


            tvLogButton.text =  buttonsPressed.toString() //event.keyCode.toString()◊

        }



        return super.onKeyDown(keyCode, event)


    }
    fun saveData(v: View){

        if (deviceId.isEnabled()){
            deviceId.isEnabled = false;
            var btnSave: Button = findViewById<Button>(R.id.btn_save);
            btnSave.isEnabled = false
        }
        deviceId.clearFocus()

        var sharedPref = getSharedPreferences("settingsApp", Context.MODE_PRIVATE)
        var editor = sharedPref.edit()
        //"ws://192.168.1.2:9999"
        editor.putString("serverIp", deviceId.text.toString() )
        editor.apply()

        Toast.makeText(this,"To change data close/open app",Toast.LENGTH_LONG).show();


    }
    fun sendKeyUp(keyCode: Int){

        val buttonsUp: MutableList<Int> = mutableListOf()
        buttonsUp.add(keyCode)
        val dataButton= JSONObject()
        dataButton.put("keyType","keyup")
        dataButton.put("keys", buttonsUp)

        val dataObject= JSONObject()
        dataObject.put("type","keys")
        dataObject.put("device", deviceId.text)
        dataObject.put("keys", dataButton)

        webSocketClient.sendMessage(dataObject.toString())
    }
    fun sendKeyDown(){
        val dataButton= JSONObject()
        dataButton.put("keyType","keydown")
        dataButton.put("keys",buttonsPressed)

        val dataObject= JSONObject()
        dataObject.put("type","keys")
        dataObject.put("device", deviceId.text)
        dataObject.put("keys", dataButton)

        webSocketClient.sendMessage(dataObject.toString())
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        controllerHelper.onGenericMotionEvent(event);
        return super.onGenericMotionEvent(event)
    }


}