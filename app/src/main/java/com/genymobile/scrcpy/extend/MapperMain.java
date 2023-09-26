package com.genymobile.scrcpy.extend;

import android.os.IBinder;

import com.genymobile.scrcpy.Device;
import com.genymobile.scrcpy.InvalidDisplayIdException;
import com.genymobile.scrcpy.Ln;
import com.genymobile.scrcpy.extend.InjectTouch.TouchControllers.TouchControllerInterface;
import com.genymobile.scrcpy.extend.InjectTouch.TouchControllers.TouchControllerNoRot;
import com.genymobile.scrcpy.wrappers.SurfaceControl;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;
import java.util.Arrays;

import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;

import java.io.Closeable;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class MapperMain {
    static int TouchActionRequire  = 0x00;
    static int TouchActionRelease  = 0x01;
    static int TouchActionMove     = 0x02;
    static int ControlOffscreen = 0x10;
    static int ControlOnscreen = 0x11;
    static final String SOCKET_NAME = "inputManagerInterfaceSocket";

    public static void TouchControllerInterface(String... args){
        //仅作为触屏控制接口
        //接收命令 并操作触屏
        int displayId;
        if (args.length == 0) {
            displayId = 0;
        }else if (args.length == 1) {
               try{
                   displayId = Integer.parseInt(args[0]);
               }catch (NumberFormatException e){
                   System.out.println("usage: [displayId] ;set displayId (Optional),default 0");
                   return;
               }
        }else{
            System.out.println("usage: [displayId] ;set displayId (Optional),default 0");
            return;
        }
        TouchControllerInterface touchController;
        try{
            touchController = new TouchControllerNoRot(displayId);
            System.out.println("inputManager is now running \nusing displayId:"+displayId);
        }catch (InvalidDisplayIdException e){
            System.out.println("InvalidDisplayID : "+e.getDisplayId()+"\nAvailableDisplayIds : " + Arrays.toString(e.getAvailableDisplayIds()) );
            return;
        }
        try {
            LocalSocket localSocket = new LocalSocket();
            localSocket.connect(new LocalSocketAddress("uds_input_manager" , LocalSocketAddress.Namespace.ABSTRACT ));
            InputStream ins = localSocket.getInputStream();
            byte[] data = new byte[10];
            while (true) {
                ins.read(data);
                int action = data[0];
                int id = data[1];
                int x = ByteBuffer.wrap( new byte[]{ data[5] , data[4] , data[3] , data[2]  }).getInt();
                int y = ByteBuffer.wrap( new byte[]{ data[9] , data[8] , data[7] , data[6]  }).getInt();
                if(action == TouchActionRequire){
                    touchController.requireTouch(x,y);
                }else if (action == TouchActionRelease){
                    touchController.releaseTouch(id);
                }else if (action == TouchActionMove){
                    touchController.touchMove(id,x,y);
                }else if (action == ControlOffscreen){
                    setDisplayPowerMode(Device.POWER_MODE_OFF);
                }else if (action == ControlOnscreen){
                    setDisplayPowerMode(Device.POWER_MODE_NORMAL);
                }
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void setDisplayPowerMode(int mode){
        IBinder d = SurfaceControl.getBuiltInDisplay();
        if (d == null) {
            Ln.e("Could not get built-in display");
        }
        SurfaceControl.setDisplayPowerMode(d, mode);
    }
}