package com.genymobile.scrcpy.extend.InjectTouch.TouchControllers;

import java.util.concurrent.Semaphore;

public class ThreadSafeTouchController implements TouchControllerInterface {
    TouchController delegate;
    private boolean running = true;
    private static int REQUIRE = 0;
    private static int MOVE = 1;
    private static int RELEASE = 2;
    private static int BREAK = 3;

    private final Semaphore exec = new Semaphore(0);
    private final Semaphore result = new Semaphore(0);
    private int action,id,x,y;
    private int retID;


    public ThreadSafeTouchController(int DisplayID) {
        delegate = new TouchController(DisplayID);
    }

    private synchronized int excuter(int action,int id,int x,int y){
            this.action = action;
            this.id = id;
            this.x = x;
            this.y = y;
            exec.release();
            try {
                result.acquire();
                return retID;
            } catch (InterruptedException e) {
                e.printStackTrace();
                return -1;
            }
    }
    public int requireTouch(int x , int y){
        return excuter(REQUIRE,-1,x,y);
    }
    public int touchMove(int id,int x,int y){
        return excuter(MOVE,id,x,y);
    }
    public int releaseTouch(int id){
        return excuter(RELEASE,id,-1,-1);
    }

    public void mainLoop(){
        while (true){
            try {
                exec.acquire();
                if(action == REQUIRE){
                    retID = delegate.requireTouch(x,y);
                    result.release();
                }else if (action == MOVE){
                    retID = id;
                    result.release();
                    delegate.touchMove(id,x,y);
                }else if (action == RELEASE){
                    retID = -1;
                    result.release();
                    delegate.releaseTouch(id);
                }else if(action == BREAK)  {
                    result.release();
                    break;
                }
            }catch (InterruptedException e){
                e.printStackTrace();
            }
        }
    }

    public void stop(){
        excuter(BREAK,-1,-1,-1);
    }
}
