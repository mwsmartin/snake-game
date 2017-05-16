package com.cwb.glancesampleapp;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by test1 on 4/5/2016.
 */
public class TouchPoint {
    //最後偵測向量X
    public float lastVectorX;
    //最後偵測向量Y
    public float lastVectorY;

    //是否有改變向量
    public boolean isChangeVector=false;

    //觸控點座標
    List<PointF> points = new ArrayList<PointF>();

    public TouchPoint(){
    }

    //處理觸控點座標
    public void update(android.view.MotionEvent event){
        for (int i = 0; i < event.getHistorySize(); i+=3) {
            points.add(new PointF(event.getHistoricalX(i), event
                    .getHistoricalY(i)));
        }

        changeVector();
    }

    //改變偵測觸控向量
    private void changeVector(){
        if (points.size() > 1) {//感測2點以上
            this.lastVectorX = points.get(points.size() - 1).x
                    - points.get(0).x;
            this.lastVectorY = points.get(points.size() - 1).y
                    - points.get(0).y;
        }
        isChangeVector=true;
    }

    //畫出觸控點路徑
    public void draw(Canvas canvas){
        if (points.size() > 1) {//感測2點以上
            Paint p = new Paint();
            p.setARGB(255, 0, 0, 0);
            p.setStrokeWidth(3);
            for (int i = 0; i < points.size() - 1; i++) {
                float x1=points.get(i).x;
                float y1=points.get(i).y;
                float x2=points.get(i+1).x;
                float y2=points.get(i+1).y;
                canvas.drawLine(x1,y1,x2,y2, p);
            }
        }
        this.resetPoint();
    }

    //重新偵測觸控向量
    public void resetPoint(){
        isChangeVector=false;
        points.clear();
    }
}
