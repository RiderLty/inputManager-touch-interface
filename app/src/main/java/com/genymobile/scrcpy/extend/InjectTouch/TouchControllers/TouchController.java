package com.genymobile.scrcpy.extend.InjectTouch.TouchControllers;

import android.view.MotionEvent;

import com.genymobile.scrcpy.Device;
import com.genymobile.scrcpy.InvalidDisplayIdException;
import com.genymobile.scrcpy.Options;
import com.genymobile.scrcpy.PointersState;
import com.genymobile.scrcpy.Position;
import com.genymobile.scrcpy.Size;
import com.genymobile.scrcpy.extend.InjectTouch.DirectController;

public class TouchController implements TouchControllerInterface {
    Device device;
    DirectController directController;
    Boolean [] downPointers = new Boolean[PointersState.MAX_POINTERS];
    int [][] downPointersPositions = new int[PointersState.MAX_POINTERS][2];

    public TouchController(int DisplayID){
        Options op = new Options();
        op.setDisplayId(DisplayID);
        try{
            device = new Device(op);
        }catch (InvalidDisplayIdException e){
            throw e;
        }
        directController = new DirectController(device);
        for (int i = 0; i < downPointers.length; i++) {
            downPointers[i] = false;
        }
    }
    public int requireTouch(int __x,int __y){
        Size screenSize = device.getScreenInfo().getVideoSize();
        int width = screenSize.getWidth();
        int height = screenSize.getHeight();
        int rot = device.getScreenInfo().getDeviceRotation();
        int x = __x;
        int y = __y;
        if (rot == 1){//左侧向下
            x = __y;
            y = height - __x;
        }else if (rot == 3){//右侧向下
            x = width - __y;
            y = __x;
        }else if (rot == 2){//逆向
            x = width - __x;
            y = height - __y;
        }
        for (int i = 0; i < downPointers.length; i++) {
            if(downPointers[i] == false){
                downPointers[i] = true;
                downPointersPositions[i][0] = x;
                downPointersPositions[i][1] = y;
                directController.injectTouch(MotionEvent.ACTION_DOWN, i, new Position(x,y,width,height), 0xffff, 0);
                return i;
            }
        }
        return -1;
    }
    public int releaseTouch(int id){
        if(id == -1){ return -1; }
        Size screenSize = device.getScreenInfo().getVideoSize();
        directController.injectTouch(MotionEvent.ACTION_UP, id, new Position(downPointersPositions[id][0],downPointersPositions[id][1],screenSize.getWidth(),screenSize.getHeight()), 0xffff, 0);
        downPointers[id] = false;
        return -1;
    }
    public int touchMove(int id,int __x,int __y){
        if (id == -1) { return -1; }
        Size screenSize = device.getScreenInfo().getVideoSize();
        int width = screenSize.getWidth();
        int height = screenSize.getHeight();
        int rot = device.getScreenInfo().getDeviceRotation();
        int x = __x;
        int y = __y;
        if (rot == 1){//左侧向下
            x = __y;
            y = height - __x;
        }else if (rot == 3){//右侧向下
            x = width - __y;
            y = __x;
        }else if (rot == 2){//逆向
            x = width - __x;
            y = height - __y;
        }
        directController.injectTouch(MotionEvent.ACTION_MOVE, id, new Position(x,y,width,height), 0xffff, 0);
        downPointersPositions[id][0] = x;
        downPointersPositions[id][1] = y;
        return id;
    }
    public void stop(){
    }
}

