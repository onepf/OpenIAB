/*
 * Copyright 2012-2014 One Platform Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onepf.life;

import android.graphics.BitmapFactory;
import android.graphics.Canvas;

/**
 * Created by akarimova on 30.12.13.
 */
public class LifeViewRenderer implements Runnable {

    private Thread thread;
    private volatile boolean running;
    private LifeView lifeView;


    public LifeViewRenderer(LifeView lifeView) {
        this.lifeView = lifeView;
    }

    @Override
    public void run() {
        while (running) {
            if (lifeView.getHolder().getSurface().isValid()) {
                Canvas canvas = lifeView.getHolder().lockCanvas();
                lifeView.updateField();
                lifeView.onDraw(canvas);
                lifeView.getHolder().unlockCanvasAndPost(canvas);
                try {
                    Thread.sleep(16);
                } catch (InterruptedException ignore) {
                }
            }
        }
    }

    public void resume() {
        running = true;
        thread = new Thread(this);
        thread.start();
    }

    public void pause() {
        running = false;
        try {
            thread.join();
        } catch (InterruptedException ignored) {
        }
    }

    public void updateCellColor(boolean hasOrangeCells) {
        lifeView.setActiveCellBitmap(BitmapFactory.decodeResource(
                lifeView.getResources(), hasOrangeCells ? R.drawable.cell_active_orange
                        : R.drawable.cell_active_green));
    }

    public void drawFigure(int[][] figure) {
        lifeView.drawFigure(figure);
    }

    public void setChangeCount(int changeCount) {
        lifeView.setChangeCount(changeCount);
    }
}
