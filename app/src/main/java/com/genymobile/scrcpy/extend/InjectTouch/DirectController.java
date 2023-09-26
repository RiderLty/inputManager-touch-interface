package com.genymobile.scrcpy.extend.InjectTouch;

import android.os.SystemClock;
import android.view.InputDevice;
import android.view.MotionEvent;

import com.genymobile.scrcpy.Device;
import com.genymobile.scrcpy.Ln;
import com.genymobile.scrcpy.Point;
import com.genymobile.scrcpy.Pointer;
import com.genymobile.scrcpy.PointersState;
import com.genymobile.scrcpy.Position;

public class DirectController {
    private final Device device;
    private long lastTouchDown;
    private final PointersState pointersState = new PointersState();
    private final MotionEvent.PointerProperties[] pointerProperties = new MotionEvent.PointerProperties[PointersState.MAX_POINTERS];
    private final MotionEvent.PointerCoords[] pointerCoords = new MotionEvent.PointerCoords[PointersState.MAX_POINTERS];
    private static final int DEFAULT_DEVICE_ID = 0;
    public DirectController(Device device){
        this.device = device;
        for (int i = 0; i < PointersState.MAX_POINTERS; ++i) {
            MotionEvent.PointerProperties props = new MotionEvent.PointerProperties();
            props.toolType = MotionEvent.TOOL_TYPE_FINGER;

            MotionEvent.PointerCoords coords = new MotionEvent.PointerCoords();
            coords.orientation = 0;
            coords.size = 0;

            pointerProperties[i] = props;
            pointerCoords[i] = coords;
        }
    }

    public boolean injectTouch(int action, long pointerId, Position position, float pressure, int buttons) {

        long now = SystemClock.uptimeMillis();

        Point point = device.getPhysicalPoint(position);
        if (point == null) {
            Ln.w("Ignore touch event, it was generated for a different device size");
            return false;
        }

        int pointerIndex = pointersState.getPointerIndex(pointerId);
        if (pointerIndex == -1) {
            Ln.w("Too many pointers for touch event");
            return false;
        }
        Pointer pointer = pointersState.get(pointerIndex);
        pointer.setPoint(point);
        pointer.setPressure(pressure);
        pointer.setUp(action == MotionEvent.ACTION_UP);

        int pointerCount = pointersState.update(pointerProperties, pointerCoords);

        if (pointerCount == 1) {
            if (action == MotionEvent.ACTION_DOWN) {
                lastTouchDown = now;
            }
        } else {
            // secondary pointers must use ACTION_POINTER_* ORed with the pointerIndex
            if (action == MotionEvent.ACTION_UP) {
                action = MotionEvent.ACTION_POINTER_UP | (pointerIndex << MotionEvent.ACTION_POINTER_INDEX_SHIFT);
            } else if (action == MotionEvent.ACTION_DOWN) {
                action = MotionEvent.ACTION_POINTER_DOWN | (pointerIndex << MotionEvent.ACTION_POINTER_INDEX_SHIFT);
            }
        }

        // Right-click and middle-click only work if the source is a mouse
        boolean nonPrimaryButtonPressed = (buttons & ~MotionEvent.BUTTON_PRIMARY) != 0;
        int source = nonPrimaryButtonPressed ? InputDevice.SOURCE_MOUSE : InputDevice.SOURCE_TOUCHSCREEN;
        if (source != InputDevice.SOURCE_MOUSE) {
            // Buttons must not be set for touch events
            buttons = 0;
        }

        MotionEvent event = MotionEvent.obtain(
                lastTouchDown,
                now,
                action,
                pointerCount,
                pointerProperties,
                pointerCoords,
                0,
                buttons,
                1f,
                1f,
                DEFAULT_DEVICE_ID,
                0,
                source,
                0);
        return device.injectEvent(event, Device.INJECT_MODE_ASYNC);
    }
}
