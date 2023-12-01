package com.example.synccontrol

import android.os.Build
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import java.math.RoundingMode
import java.text.DecimalFormat

class ControllerHelper {
    private var controllerInputEventListener: ControllerInputEventListener? = null
    private val keyBindings = HashMap<String, String>()

    init {
        println("Controller helper called")
        val release = Build.VERSION.RELEASE.replace("(\\d+[.]\\d+)(.*)".toRegex(), "$1").toDouble()
        println(release)
        if (release < 9) {
            keyBindings["B"] = "cross"
            keyBindings["C"] = "circle"
            keyBindings["A"] = "square"
            keyBindings["X"] = "triangle"
            keyBindings["R2"] = "options"
            keyBindings["L2"] = "share"
            keyBindings["Z"] = "right_1"
            keyBindings["Y"] = "left_1"
            keyBindings["MODE"] = "ps"
            keyBindings["START"] = "right_thumb"
            keyBindings["SELECT"] = "left_thumb"
            keyBindings["THUMBL"] = "pad"
            keyBindings["R1"] = "right_2"
            keyBindings["L1"] = "left_2"
        } else {
            keyBindings["DEL"] = "square"
            keyBindings["BACK"] = "circle"
            keyBindings["A"] = "cross"
            keyBindings["Y"] = "triangle"
            keyBindings["R1"] = "right_1"
            keyBindings["L1"] = "left_1"
            keyBindings["R2"] = "right_2"
            keyBindings["L2"] = "left_2"
            keyBindings["MODE"] = "ps"
            keyBindings["START"] = "options"
            keyBindings["SELECT"] = "share"
            keyBindings["THUMBR"] = "right_thumb"
            keyBindings["THUMBL"] = "left_thumb"
            keyBindings["1"] = "pad"
        }
    }

    /* EVENTS INPUT */
    fun onGenericMotionEvent(event: MotionEvent): Boolean {

        if (event.source and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK &&
            event.action == MotionEvent.ACTION_MOVE
        ) {
            val historySize = event.historySize
            for (i in 0 until historySize) {
                processJoystickInput(event, i)
            }
            processJoystickInput(event, -1)
        }
        return true
    }

    fun onKeyDown(keyCode: Int): Boolean {
        println(KeyEvent.keyCodeToString(keyCode));
        val keyId = bindKeyCode(keyCode)
        if (keyId != null) {
            if (controllerInputEventListener != null) {
                controllerInputEventListener!!.run(keyId, 0, 0)
            }
            return true
        }
        return false
    }

    private fun bindKeyCode(keyCode: Int): String? {
        var keyCodeLabel = KeyEvent.keyCodeToString(keyCode)
        keyCodeLabel = keyCodeLabel.replace("KEYCODE_BUTTON_", "")
        return if (keyBindings.containsKey(keyCodeLabel)) {
            keyBindings[keyCodeLabel]
        } else {
            keyCodeLabel = keyCodeLabel.replace("KEYCODE_", "")
            if (keyBindings.containsKey(keyCodeLabel)) {
                keyBindings[keyCodeLabel]
            } else {
                null
            }
        }
    }

    private val gameControllerIds: ArrayList<Int>
        private get() {
            val gameControllerDeviceIds = ArrayList<Int>()
            val deviceIds = InputDevice.getDeviceIds()
            for (deviceId in deviceIds) {
                val dev = InputDevice.getDevice(deviceId)
                val sources = dev!!.sources
                if (sources and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD
                    || (sources and InputDevice.SOURCE_JOYSTICK
                            == InputDevice.SOURCE_JOYSTICK)
                ) {
                    if (!gameControllerDeviceIds.contains(deviceId)) {
                        gameControllerDeviceIds.add(deviceId)
                    }
                }
            }
            return gameControllerDeviceIds
        }

    fun haveControllerConnected(): Boolean {
        return gameControllerIds.size != 0
    }

    val controllerId: Int
        get() = gameControllerIds[0]

    private fun processJoystickInput(event: MotionEvent, historyPos: Int) {
        val inputDevice = event.device
        val axisType: String
        var x = getCenteredAxis(event, inputDevice, MotionEvent.AXIS_X, historyPos)
        var y = getCenteredAxis(event, inputDevice, MotionEvent.AXIS_Y, historyPos)

        val lx = getCenteredAxis(
            event, inputDevice,
            MotionEvent.AXIS_X, historyPos
        )

        val rx = getCenteredAxis(
            event, inputDevice,
            MotionEvent.AXIS_Z, historyPos
        )

        val ly = getCenteredAxis(
            event, inputDevice,
            MotionEvent.AXIS_Y, historyPos
        )

        val ry = getCenteredAxis(
            event, inputDevice,
            MotionEvent.AXIS_RZ, historyPos
        )

        println("ANALOG:  $lx, $ly, $rx, $ry")
        println("AXIS:  $x, $y")
        if (x != 0f || y != 0f) {
            axisType = "left"
        } else {
            x = getCenteredAxis(event, inputDevice, MotionEvent.AXIS_Z, historyPos)
            y = getCenteredAxis(event, inputDevice, MotionEvent.AXIS_RZ, historyPos)
            if (x != 0f || y != 0f) {
                axisType = "right"
            } else {
                x = getCenteredAxis(event, inputDevice, MotionEvent.AXIS_HAT_X, historyPos)
                y = getCenteredAxis(event, inputDevice, MotionEvent.AXIS_HAT_Y, historyPos)
                axisType = if (x != 0f || y != 0f) {
                    "hat"
                } else {
                    //                    x = getCenteredAxis(event, inputDevice, MotionEvent.AXIS_LTRIGGER, historyPos);
                    //                    y = getCenteredAxis(event, inputDevice, MotionEvent.AXIS_RTRIGGER, historyPos);
                    //                    if (x != 0 || y != 0) {
                    //                        axisType = "shoulder";
                    //                    } else {
                    //                        axisType = "reset";
                    //                    }
                    "reset"
                }
            }
        }
        if (controllerInputEventListener != null) {
            controllerInputEventListener!!.run(axisType, getAngle(x, y), getStrength(x, y))
        }
    }

    interface ControllerInputEventListener {
        fun run(keyId: String?, angle: Int, strength: Int)
    }

    fun registerOnControllerInputEventListener(controllerInputEventListener: ControllerInputEventListener?) {
        this.controllerInputEventListener = controllerInputEventListener
    }

    private fun getAngle(x: Float, y: Float): Int {
        val angle = roundFloat(Math.toDegrees(Math.atan2(y.toDouble(), x.toDouble())).toFloat())
        return if (angle < 0) angle + 360 else angle
    }

    private fun getStrength(x: Float, y: Float): Int {
        val strength = Math.sqrt((x * x + y * y).toDouble()).toFloat() * 100
        return if (strength >= 100) 100 else roundFloat(strength)
    }

    private fun roundFloat(value: Float): Int {
        val df = DecimalFormat("#")
        df.roundingMode = RoundingMode.CEILING
        return df.format(value.toDouble()).toInt()
    }

    companion object {
        fun getCenteredAxis(
            event: MotionEvent,
            device: InputDevice,
            axis: Int,
            historyPos: Int
        ): Float {
            val range = device.getMotionRange(axis, event.source)
            if (range != null) {
                val flat = range.flat
                val value =
                    if (historyPos < 0) event.getAxisValue(axis) else event.getHistoricalAxisValue(
                        axis,
                        historyPos
                    )
                if (Math.abs(value) > flat) {
                    return value
                }
            }
            return 0F
        }
    }
}