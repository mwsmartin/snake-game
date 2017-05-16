//---------------------------------------------------------------------
//
// Copyright (c) 2016 CWB Tech Limited All rights reserved
//
//
//---------------------------------------------------------------------
// File: GraphFragment.java
// Author: Kevin Kwok (kevinkwok@cwb-tech.com)
//         William Chan (williamchan@cwb-tech.com)
// Project: Glance
//---------------------------------------------------------------------

package com.cwb.glancesampleapp;

import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.app.Fragment;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;


import com.cwb.bleframework.GlanceStatus;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.util.HashMap;

public class GraphFragment extends Fragment {
    private String TAG = "GraphFragment";

    private static final int SWITCH_MODE_STATEMACHINE_DELAY = 500;
    private static final int SWITCH_MODE_STATEMACHINE_WAIT_MAX_TIMEOUT = 60000;
    private static final int SWITCH_MODE_STATEMACHINE_WAIT_MAX_TIMEOUT_COUNT = SWITCH_MODE_STATEMACHINE_WAIT_MAX_TIMEOUT / SWITCH_MODE_STATEMACHINE_DELAY;
    private static final int MSG_KEEP_4SEC_GRAPH_DATA = 1000;
    private static final int MSG_STATE_SET_STREAMING_MODE = MSG_KEEP_4SEC_GRAPH_DATA + 1;
    private static final int MSG_STATE_WAITING_SET_STREAMING_MODE = MSG_STATE_SET_STREAMING_MODE + 1;
    private static final int MSG_STATE_SET_CONNECTION_INTERVAL = MSG_STATE_WAITING_SET_STREAMING_MODE + 1;
    private static final int MSG_STATE_WAITING_SET_CONNECTION_INTERVAL = MSG_STATE_SET_CONNECTION_INTERVAL + 1;
    private static final int MSG_STATE_TIMEOUT = MSG_STATE_WAITING_SET_CONNECTION_INTERVAL + 1;
    
    private TextView mDataAnalysis;
    private TextView mDataAnalysis2;
    private TextView BtnLocation;
    private TextView First_score;

    private FrameLayout mGraphViewAccl;
    private FrameLayout mGraphViewGyro;
    private GraphDisplayXYZ mAccelGraph;
    private GraphDisplayXYZ mGyro;
    public Button mCapture;
    private Button Start;
    private Button Score;
    private Button Exit;
    private int enableswitch=1;
    private int lock_click_event=0;
    private int lock_enableswitch =0;
    private int mGraphX;
    private double mRMSAccel[];
    private double mRMSGYRO[];
    private double mGyroRawData[];
	private boolean isCapturing;
    public boolean mKeep4SecGraphData;
    public int mStateLoopCount = 0;
   public boolean mIsReceivedSetStreamingMode = false;
   public boolean mIsReceivedGetConnectionInterval = false;
    private int button_choose=0;
    private int showing_score=0;
    private int playing_game=0;


    FrameLayout snakeview;
    KeyHandler keyHandler = new KeyHandler();
    SnakeObj snake;
    GameObj backimg;
    Boolean isGameThreadStop = true;
    drawAction nowDrawWork;
    AppleObj apple;
    GameStat gameStat;
    Thread gameThread;
    TouchPoint touchPoint = new TouchPoint();
    int gameFPS = 25;
    int gamescore=0;
    String  score_recod=null;
    LayoutInflater inflater_score ;
    View layout_score ;
    AlertDialog.Builder editDialog ;
    AlertDialog mBuilder;
    String FILENAME = "Score";
    FileOutputStream fos ;
    FileInputStream fIn ;
    File path;
    File file;
    String path_str;

    //
    Bundle bundle = new Bundle();

    SurfaceView gameSurfaceView;
    SurfaceHolder surfaceHolder;


    public static Object mLock = new Object();
    FragmentManager manager = getFragmentManager();
    Context mContext = getActivity();

    public interface onGraphListener {
        public abstract void onEnableStreaming(boolean enable);
        public abstract void onSetConnectionIntervalHigh();
        public abstract void onSetStreamingMode();
        public abstract void onResetStreamingMode();
        public abstract void onKeyBack();
    }

    public onGraphListener onGraphListener = null;

    public void setOnGraphListener(onGraphListener listener) {
        onGraphListener = listener;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "  onCreateView");

        View view = inflater.inflate(R.layout.game_menu, container, false);
        snakeview = (FrameLayout)view.findViewById(R.id.frame);
        snake_activity();


               // Start = (Button) view.findViewById(R.id.butStartgame);
       // Score = (Button) view.findViewById(R.id.score);
        Exit = (Button) view.findViewById(R.id.exit_game);

         path =getActivity().getFilesDir();
       if(!path.exists()) file.mkdirs();
         path_str =path.toString();

        file=new File(path_str+"/score.txt");

       // if(!file.exists()) {file.mkdirs();
            /*try {
                // fos = getActivity().getApplication().openFileOutput(file,getActivity().MODE_PRIVATE);
                fos = new FileOutputStream(file,false);

                //if(gamescore>max_score)
                 fos.write("2".getBytes());

                //  fos.write("aftergame".getBytes());
                fos.close();
            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }//}*/

        readscore();
        savescore();
      /* mDataAnalysis = (TextView) view.findViewById(R.id.graph_analysis);
        mDataAnalysis2 = (TextView) view.findViewById(R.id.graph_analysis2);
        mGraphViewAccl = (FrameLayout) view.findViewById(R.id.graph_accel_rawdata);
        mGraphViewGyro = (FrameLayout) view.findViewById(R.id.graph_gyro_rawdata);
        mCapture = (Button) view.findViewById(R.id.graph_capture_4s);
        mCapture.setOnClickListener(mOn4sClickListener);*/
        Exit.setOnClickListener(mExit);
       // mDataAnalysis2 = (TextView) view.findViewById(R.id.analysis_test);
        BtnLocation =(TextView) view.findViewById(R.id.button_location);



        mCapture = (Button) view.findViewById(R.id.stream);
        mCapture.setOnClickListener(mOn4sClickListener);


        isCapturing = false;

        return view;
    }

    private View.OnClickListener mExit = new View.OnClickListener(){
        @Override
        public void onClick(View v) {

        onGraphListener.onKeyBack();
        }
    };

    private View.OnClickListener mOn4sClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!isCapturing){
                if (onGraphListener != null) {
                    onGraphListener.onEnableStreaming(true);
                }
                ((Button) v).setText(R.string.chip_dev_graph_stop_capturing);
                mGraphX = 0;
                mKeep4SecGraphData = false;
                readTimePlot();
            }else{
                if (onGraphListener != null) {
                    onGraphListener.onEnableStreaming(false);
                }
                ((Button) v).setText(R.string.chip_dev_graph_capture_4s);
            }
            isCapturing = !isCapturing;
        }
    };

    private void readTimePlot(){
       // mGraphViewAccl.removeAllViews();
       // mGraphViewGyro.removeAllViews();
        mAccelGraph = new GraphDisplayXYZ(getActivity(), 4096-500, 4096+500);
       // mGraphViewAccl.addView(mAccelGraph.getView());
        mGyro = new GraphDisplayXYZ(getActivity(),4096-500, 4096+500);
      //  mGraphViewGyro.addView(mGyro.getView());
    }
    public void readscore(){
       // if(score_recod=="0"){score_recod="";}
      StringBuilder sb = new StringBuilder();
        //int length = (int) file.length();
      // byte[] bytes = new byte[length];
       // String contents = new String;
        try{

           fIn = new FileInputStream(file);
           int ch;
           while((ch = fIn.read()) != -1){
               sb.append((char)ch);
           }
          // fIn.read(contents.getBytes());

           fIn.close();

        }

        catch (IOException ioe)
        {ioe.printStackTrace();}

             score_recod=sb.toString();

    }
    public boolean isInteger( String input )
    {
        try
        {
            Integer.parseInt(input);
            return true;
        }
        catch( Exception e )
        {
            return false;
        }
    }
    public void savescore(){
        int max_score;
        String max;
        if(isInteger(score_recod))
        {  max_score = Integer.parseInt(score_recod);
          }
        else {score_recod="0";
            max_score = Integer.parseInt(score_recod);
        }

        if(max_score<gamescore)
        {
            score_recod=Integer.toString(gamescore);
            max= score_recod;
        }
        else
        max = score_recod;

        try {
            // fos = getActivity().getApplication().openFileOutput(file,getActivity().MODE_PRIVATE);
            fos = new FileOutputStream(file,false);

            //if(gamescore>max_score)
            fos.write(max.getBytes());

            //  fos.write("aftergame".getBytes());
            fos.close();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }


       // String str = Integer.toString(gamescore);

       /* try {
           // fos = getActivity().getApplication().openFileOutput(file,getActivity().MODE_PRIVATE);
           fos = new FileOutputStream(path_str+"/score.txt");

              //if(gamescore>max_score)
              // fos.write(str.getBytes());

            //  fos.write("aftergame".getBytes());
            fos.close();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }*/

}

    public void processMotionData(HashMap<String, GlanceStatus.SensorData> kvs)
    {
        if (kvs.containsKey("accel") && kvs.containsKey("gyro")) {
            receiveRawDataGraph(kvs.get("accel"), kvs.get("gyro"));
    }
    }

    public void receiveRawDataGraph(GlanceStatus.SensorData accel, GlanceStatus.SensorData gyro){
        if (mAccelGraph == null) {
            Log.e(TAG, "accelGraph not init");
            return;
        } else if (!mAccelGraph.isRealTimeUpdate()) {
            Log.e(TAG, "accelGraph not in real time mdoe");
            return;
        }
        if (mAccelGraph.getSize() == 0) {
            mHandler.sendEmptyMessageDelayed(MSG_KEEP_4SEC_GRAPH_DATA, 4000);
        }
        if (mKeep4SecGraphData) {
            mAccelGraph.removeFirst();
        }
       mAccelGraph.addPoint(mGraphX, accel);
        mGyro.addPoint(mGraphX, gyro);
        mRMSAccel = mAccelGraph.getRMS();
        mRMSGYRO = mGyro.getRMS();
        mGyroRawData = mGyro.getrawdata();




       // filter for noise
        if(mGyroRawData[1]>-100)
            enableswitch=1;


        if(enableswitch==1&&lock_enableswitch==0){

        if(mGyroRawData[1]<-5000)
        { if(button_choose==0)
            button_choose=2;
            else
            button_choose--;
            enableswitch=0;
        }
           }
       // mDataAnalysis2.setText("Root Mean Square:\n");
       // mDataAnalysis2.append(String.format("Gyro: %.02f %.02f %.02f\n", mGyroRawData[0], mGyroRawData[1], mGyroRawData[2]));
        if(button_choose==0)
        {    BtnLocation.setText("Button location:\n Start Game");

        }
        else if(button_choose==1)
        {    BtnLocation.setText("Button location:\nScore");

        }
        else if (button_choose==2)
        {   BtnLocation.setText("Button location:\nExit");

        }


        //mGyroRawData[1]>4500 is treated as a "click" action
        if(mGyroRawData[0]<200)
            lock_click_event=0;//  noise filter
        if(lock_click_event==0){
            //mGyroRawData[0]>5000  is "click"
            if(mGyroRawData[0]>5000){

            lock_click_event=1;


            if(button_choose==0&&playing_game==0)
            {   playing_game=1;
                lock_enableswitch=1;
                readyGame();

            }
           else if(showing_score==1&&button_choose==1) {
                mBuilder.dismiss();
                showing_score=0;//user finish watching score
                lock_enableswitch=0;// allow switching button
            }

            else if(button_choose==1&&showing_score==0)
            {   readscore();
                savescore();
                set_score_dialog();
                mBuilder.show();
                showing_score=1;//user is watching the score
                lock_enableswitch=1;// prevent switching in button

            }
            else if (button_choose==2)
            {  onGraphListener.onKeyBack();

            }

        }}

        /*mDataAnalysis.setText("Raw Data:\n");
        mDataAnalysis.append(String.format("Gyro: %.02f %.02f %.02f\n", mGyroRawData[0], mGyroRawData[1], mGyroRawData[2]));
        mDataAnalysis2.setText("Root Mean Square:\n");
        mDataAnalysis2.append(String.format("Gyro: %.02f %.02f %.02f\n", mRMSGYRO[0], mRMSGYRO[1], mRMSGYRO[2]));*/

    }

    public void snake_activity(){

        gameSurfaceView = new SurfaceView(getActivity());
        surfaceHolder = gameSurfaceView.getHolder();
        surfaceHolder.addCallback(new SurfaceHolder.Callback() {
            public void surfaceDestroyed(SurfaceHolder arg0) {
            }
            public void surfaceCreated(SurfaceHolder arg0) {
                if (backimg == null) {
                    // 第一次Activity載入時
                    Resources rs = getResources();
                    backimg = new GameObj(rs.getDrawable(R.drawable.backimg));
                    SurfaceView sv = gameSurfaceView;
                    backimg.setRect(new Rect(sv.getLeft(), sv.getTop(), sv
                            .getRight(), sv.getBottom()));
                    //readyGame();
                } /*else {
                    // 經由Activity返回載入時
                    draw(nowDrawWork);
                  //  openOptionsMenu();

                }*/
            }

            public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2,
                                       int arg3) {

            }
        });

        //setContentView(gameSurfaceView);
        snakeview.addView(gameSurfaceView);
    }
    enum drawAction {
        ready, game, pause, over
    }
    void drawReady(Canvas canvas) {
        clear(canvas);
        Paint pt = new Paint();
        pt.setTextAlign(Paint.Align.CENTER);
        pt.setARGB(255, 0, 0, 255);
        pt.setTextSize(30);
        canvas.drawText(gameStat.getCountdownTime() + "SEC before game START-", backimg
                .centerX(), backimg.centerY(), pt);
        if (gameStat.isTimeOver())
            startGame();
    }
    void clear(Canvas canvas) {
        Paint p = new Paint();
        p.setARGB(100, 0, 0, 0);
        backimg.draw(canvas);
    }

    void draw(drawAction action, Canvas canvas) {
        switch (action) {
            case ready:
                drawReady(canvas);
                break;
            case game:
                drawGame(canvas);
                break;
            case pause:
                drawPause(canvas);
                break;
            case over:
                drawOver(canvas);
                break;
        }
    }
    void drawPause(Canvas canvas) {
        draw(nowDrawWork, canvas);
        Paint pt = new Paint();
        pt.setARGB(30, 0, 0, 100);
        canvas.drawRect(backimg.getRect(), pt);
        pt.setTextAlign(Paint.Align.CENTER);
        pt.setARGB(150, 200, 200, 200);
        pt.setTextSize(50);
        canvas.drawText("-遊戲暫停-", backimg.centerX(), backimg.centerY(), pt);
    }
    void drawOver(Canvas canvas) {
        // 執行緒停止
        gameThreadStop();
        drawGame(canvas);
        Paint pt = new Paint();
        pt.setARGB(30, 30, 30, 30);
        canvas.drawRect(backimg.getRect(), pt);
        pt.setTextAlign(Paint.Align.CENTER);
        pt.setARGB(100, 0, 0, 255);
        pt.setTextSize(50);
        canvas.drawText("-OVER-", backimg.centerX(), backimg.centerY(), pt);
        playing_game=0;
        lock_enableswitch=0;
        gamescore=gameStat.getgameScore();
       // savescore();

    }
    void drawGame(Canvas canvas) {
        clear(canvas);
        apple.draw(canvas);
        snake.draw(canvas);
        gameStat.draw(canvas);
        touchPoint.draw(canvas);
    }
    public void gameThreadStart() {
        isGameThreadStop = false;
       // powerControl(true);
        if (gameThread == null) {
            gameThread = new Thread(gameRun);
            gameThread.start();
        } else if (!gameThread.isAlive()) {
            gameThread = new Thread(gameRun);
            gameThread.start();
        }
    }
    Runnable gameRun = new Runnable() {
        public void run() {
            long delayTime = 1000 / gameFPS;
            while (!isGameThreadStop) {
                long startTime = System.currentTimeMillis();
                if (nowDrawWork == drawAction.game)
                    gameUpdate();
                draw(nowDrawWork);
                long endTime = System.currentTimeMillis();
                long waitTime = delayTime - (startTime - endTime);
                if (waitTime > 0) {
                    try {
                        Thread.sleep(waitTime);
                    } catch (InterruptedException e) {
                    }
                }
            }
        }
    };
    boolean isKeyDown(int keyCode) {
        return keyHandler.isKeyDown(keyCode);
    }
    void gameUpdate() {
        boolean isChangeMove = false;

        // 觸控事件處理
        if (touchPoint.isChangeVector) {
            snake.move(touchPoint.lastVectorX, touchPoint.lastVectorY);
            isChangeMove = true;
        } else {
            // 按鍵事件處理
            // turn right
            if (mGyroRawData[2]<-3000) {
                snake.move(1, 0);
                isChangeMove = true;
            }
            //turn left
            if (mGyroRawData[2]>3500) {
                snake.move(-1, 0);
                isChangeMove = true;
            }
            //turn up
            if (mGyroRawData[0]>3000) {
                snake.move(0, -1);
                isChangeMove = true;
            }
            //turn down
            if (mGyroRawData[0]<-3000) {
                snake.move(0, 1);
                isChangeMove = true;
            }
        }
        // 沒有改變移動則往之前方向移動
        if (!isChangeMove)
            snake.move();

        // 更新貪食蛇
        snake.update();

        // 吃到蘋果處理
        if (snake.isEatApple(apple)) {
            // 增加長度
            snake.add();
            // 增加時間
            gameStat.addTime(3000);

            // 蘋果位置變更
            while (snake.isEatApple(apple))
                apple.random(backimg.getRect());
        }
        // 更新遊戲分數
        gameStat.updateScroe(snake.getLength());

        // 判斷是否結束遊戲
        if (gameStat.isTimeOver())
        {
            nowDrawWork = drawAction.over;}
    }

    void draw(drawAction action) {
        Canvas canvas = null;
        try {
            canvas = surfaceHolder.lockCanvas(null);
            synchronized (surfaceHolder) {
                draw(action, canvas);
            }
        } finally {
            if (canvas != null) {
                surfaceHolder.unlockCanvasAndPost(canvas);
            }
        }
    }
    void startGame() {
        gameStat = new GameStat(System.currentTimeMillis() + 30000);
        nowDrawWork = drawAction.game;
    }

    // 暫停遊戲
    void pauseGame() {
        gameThreadStop();
        if (nowDrawWork != drawAction.over) {
            gameStat.timePause();
            draw(drawAction.pause);
        }

    }
    void resumeGame() {
        if (nowDrawWork != drawAction.over) {
            gameThreadStart();
            gameStat.timeResume();
        }
    }
    public void gameThreadStop() {
        isGameThreadStop = true;
        //powerControl(false);
    }

    void readyGame() {
        gameThreadStop();
        nowDrawWork = drawAction.ready;
        Resources rs = getResources();
        snake = new SnakeObj(getActivity(), backimg.getRect());
        apple = new AppleObj(rs.getDrawable(R.drawable.apple), backimg
                .getRect());
        apple.random(backimg.getRect());
        gameStat = new GameStat(System.currentTimeMillis() + 3000);
        gameThreadStart();
    }

    public void set_score_dialog(){
         inflater_score = getActivity().getLayoutInflater();
        layout_score= inflater_score.inflate(R.layout.score, null);
        First_score = (TextView) layout_score.findViewById(R.id.first_score);
        First_score.setText(score_recod);
         editDialog = new AlertDialog.Builder(getActivity());
        editDialog.setView(layout_score);
        mBuilder = editDialog.create();


    }





    public void SetStreamingModeSuccess()
    {
        synchronized(mLock) {
            mIsReceivedSetStreamingMode = true;
        }
    }

    public void start()
    {
        mHandler.sendEmptyMessage(MSG_STATE_SET_STREAMING_MODE);
    }

    public void SetConnectionIntervalSuccess()
    {
        synchronized(mLock) {
            mIsReceivedGetConnectionInterval = true;
        }
    }

    public void keyBack()
    {
        if (onGraphListener != null) {
            onGraphListener.onEnableStreaming(false);
            onGraphListener.onResetStreamingMode();
            onGraphListener.onKeyBack();
        }
    }

    /**
     * Instances of static inner classes do not hold an implicit
     * reference to their outer class.
     */
    private static class MyHandler extends Handler {
        private final WeakReference<GraphFragment> mDevFragment;

        public MyHandler(GraphFragment fragment) {
            mDevFragment = new WeakReference<GraphFragment>(fragment);
        }

        @Override
        public void handleMessage(Message msg) {
            GraphFragment fragment = mDevFragment.get();
            int ret = 0;
            if (fragment != null) {
                synchronized(fragment.mLock) {
                    switch (msg.what) {

                        case MSG_KEEP_4SEC_GRAPH_DATA:
                            fragment.mKeep4SecGraphData = true;
                            break;
                        case  MSG_STATE_SET_STREAMING_MODE:
                            if (fragment.onGraphListener != null) {
                                fragment.mStateLoopCount = 0;
                                fragment.mIsReceivedSetStreamingMode = false;
                                fragment.onGraphListener.onSetStreamingMode();
                                fragment.mHandler.sendEmptyMessageDelayed(MSG_STATE_WAITING_SET_STREAMING_MODE, SWITCH_MODE_STATEMACHINE_DELAY);
                            }
                            else {
                                fragment.mHandler.sendEmptyMessage(MSG_STATE_TIMEOUT);
                            }
                            break;
                        case  MSG_STATE_WAITING_SET_STREAMING_MODE:
                            if (fragment.mIsReceivedSetStreamingMode) {
                                fragment.mHandler.sendEmptyMessageDelayed(MSG_STATE_SET_CONNECTION_INTERVAL, SWITCH_MODE_STATEMACHINE_DELAY);
                            } else {
                                if (++fragment.mStateLoopCount < SWITCH_MODE_STATEMACHINE_WAIT_MAX_TIMEOUT_COUNT) {
                                    fragment.mHandler.sendEmptyMessageDelayed(MSG_STATE_WAITING_SET_STREAMING_MODE, SWITCH_MODE_STATEMACHINE_DELAY);
                                } else {
                                    fragment.mHandler.sendEmptyMessage(MSG_STATE_TIMEOUT);
                                }
                            }
                            break;
                        case  MSG_STATE_SET_CONNECTION_INTERVAL:
                            if (fragment.onGraphListener != null) {
                                fragment.mStateLoopCount = 0;
                                fragment.mIsReceivedGetConnectionInterval = false;
                                fragment.onGraphListener.onSetConnectionIntervalHigh();
                                fragment.mHandler.sendEmptyMessageDelayed(MSG_STATE_WAITING_SET_CONNECTION_INTERVAL, SWITCH_MODE_STATEMACHINE_DELAY);
                            }
                            else {
                                fragment.mHandler.sendEmptyMessage(MSG_STATE_TIMEOUT);
                            }
                            break;
                        case  MSG_STATE_WAITING_SET_CONNECTION_INTERVAL:
                            if (fragment.mIsReceivedGetConnectionInterval) {
                                if (fragment.mCapture != null)
                                {
                                    fragment.mCapture.setEnabled(true);
                                }
                            } else {
                                if (++fragment.mStateLoopCount < SWITCH_MODE_STATEMACHINE_WAIT_MAX_TIMEOUT_COUNT) {
                                    fragment.mHandler.sendEmptyMessageDelayed(MSG_STATE_WAITING_SET_STREAMING_MODE, SWITCH_MODE_STATEMACHINE_DELAY);
                                } else {
                                    fragment.mHandler.sendEmptyMessage(MSG_STATE_TIMEOUT);
                                }
                            }
                            break;
                        case  MSG_STATE_TIMEOUT:
                            if (fragment.getActivity() != null) {
                                Toast.makeText(fragment.getActivity(), R.string.chip_dev_graph_fail, Toast.LENGTH_SHORT).show();
                            }
                            break;
                    }
                }
            }
        }
    }

    public final MyHandler mHandler = new MyHandler(this);
}
