/*******************************************************************************
 * Copyright 2013 One Platform Foundation
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 ******************************************************************************/

package org.onepf.life2;


import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

public class LifeView extends SurfaceView implements Runnable {

    // SurfaceView login related members
    SurfaceHolder holder;
    Thread thread = null;
    volatile boolean running = false;
    GameActivity baseActivity;

    // Size members
    int viewWidth;
    int viewHeight;
    int cellWidth = 0;
    int cellHeight = 0;

    // Field
    int fieldWidth;
    int fieldHeight;
    int[][] field = null;

    // Edits
    boolean editMode = true;
    int changeCount;

    // Cell images & background
    Bitmap activeCellBitmap = null;
    Bitmap emptyCellBitmap = null;
    int backgroundColor;

    public LifeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.baseActivity = (GameActivity) context;
        final SharedPreferences settings = baseActivity.getSharedPreferencesForCurrentUser();
        changeCount = settings.getInt(GameActivity.CHANGES, 50);
        backgroundColor = baseActivity.getResources().getColor(R.color.background);
        holder = getHolder();
        activeCellBitmap = BitmapFactory.decodeResource(getResources(),
                R.drawable.cell_active_green);
        emptyCellBitmap = BitmapFactory.decodeResource(getResources(),
                R.drawable.cell_empty);
        cellWidth = activeCellBitmap.getWidth();
        cellHeight = activeCellBitmap.getHeight();
    }


//    public void onBuyUpgradeEvent() {
//        String requestId = PurchasingManager.initiatePurchaseRequest(getResources().getString(R.string.consumable_sku));
//        Log.d(GameActivity.TAG, "Buy requestId = " + requestId);
//        baseActivity.storeRequestId(requestId, GameActivity.CHANGES);
//    }

    public void setActiveCellBitmap(Bitmap bitmap) {
        activeCellBitmap = bitmap;
    }

    public void setEditMode(boolean editMode) {
        this.editMode = editMode;
    }

    public boolean getEditMode() {
        return editMode;
    }

    public void setChangeCount(int changeCount) {
        this.changeCount = changeCount;
    }

    public int getChangeCount() {
        return changeCount;
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
        } catch (InterruptedException ignore) {
        }
    }

    void alert(String message) {
        AlertDialog.Builder bld = new AlertDialog.Builder(baseActivity);
        bld.setMessage(message);
        bld.setNeutralButton("OK", null);
        bld.create().show();
    }

    void complain(String message) {
        alert("Error: " + message);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (editMode) {
            int cellX = (int) (event.getX() / cellWidth);
            int cellY = (int) (event.getY() / cellHeight);
            int action = event.getAction();
            if (action == MotionEvent.ACTION_DOWN && cellX >= 0 && cellY >= 0
                    && cellX < fieldWidth && cellY < fieldHeight) {
                if (getChangeCount() > 0) {
                    changeCount--;
                    final SharedPreferences.Editor editor = baseActivity.getSharedPreferencesForCurrentUser().edit();
                    editor.putInt(GameActivity.CHANGES, changeCount);
                    editor.commit();
                    field[cellX][cellY] = 1 - field[cellX][cellY];
                } else {
                    showBuyChangesDialog();
                }
            }
        }

        return true;
    }

    public void showBuyChangesDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(
                baseActivity);
        builder.setTitle(R.string.changes_ended)
                .setMessage(R.string.changes_ended_message)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.yes,
                        new DialogInterface.OnClickListener() {
                            public void onClick(
                                    DialogInterface dialog,
                                    int which) {
                                baseActivity.getBillingHelper().onBuyChanges();
                            }
                        }).setNegativeButton(android.R.string.no, null).show();
    }

    @SuppressLint("WrongCall")
    @Override
    public void run() {
        while (running) {
            if (holder.getSurface().isValid()) {
                Canvas canvas = holder.lockCanvas();
                if (!editMode) {
                    updateField();
                }
                onDraw(canvas);
                holder.unlockCanvasAndPost(canvas);
                try {
                    Thread.sleep(16);
                } catch (InterruptedException ignore) {
                }
            }
        }
    }

    int getNeighbour(int x, int y, int dx, int dy) {
        int x2 = (x + dx + fieldWidth) % fieldWidth;
        int y2 = (y + dy + fieldHeight) % fieldHeight;
        return field[x2][y2];
    }

    int getNeighboursCount(int x, int y) {
        int count = 0;

        // Check all the cell's neighbours
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                // except the cell itself
                if (dx != 0 || dy != 0) {
                    count += getNeighbour(x, y, dx, dy);
                }
            }
        }
        return count;
    }

    void updateField() {
        int[][] field2 = new int[fieldWidth][fieldHeight];
        for (int x = 0; x < fieldWidth; x++) {
            for (int y = 0; y < fieldHeight; y++) {

                field2[x][y] = field[x][y];
                int neighbours = getNeighboursCount(x, y);

                if (neighbours < 2) {
                    field2[x][y] = 0;
                } else if (neighbours == 2) {
                    field2[x][y] = field[x][y];
                } else if (neighbours == 3) {
                    field2[x][y] = 1;
                } else {
                    field2[x][y] = 0;
                }
            }
        }
        field = field2;
    }

    @SuppressLint("ResourceAsColor")
    @Override
    public synchronized void onDraw(Canvas canvas) {
        canvas.drawColor(backgroundColor);
        for (int x = 0; x < fieldWidth; x++) {
            for (int y = 0; y < fieldHeight; y++) {
                if (field[x][y] == 1) {
                    canvas.drawBitmap(activeCellBitmap, x * cellWidth, y
                            * cellHeight, null);
                } else {
                    canvas.drawBitmap(emptyCellBitmap, x * cellWidth, y
                            * cellHeight, null);
                }
            }
        }
        Paint paint = new Paint();
        paint.setColor(android.R.color.primary_text_dark);
        paint.setAlpha(160);
        paint.setTextSize((float) 0.7 * cellHeight);
        canvas.drawText("Changes: " + changeCount, cellWidth / 2, cellHeight, paint);
    }

    @Override
    public synchronized void onSizeChanged(int w, int h, int oldW, int oldH) {
        Log.d(GameActivity.TAG, "Old size: w = " + fieldWidth + ", h = " + fieldHeight);
        if (field == null) {
            viewHeight = w;
            viewWidth = h;
            fieldHeight = viewWidth / cellWidth;
            fieldWidth = viewHeight / cellHeight;
            initField();
        } else {
            int newFieldWidth = w / cellWidth;
            int newFieldHeight = h / cellHeight;
            Log.d(GameActivity.TAG, "New size: w = " + newFieldWidth + ", h = " + newFieldHeight);
            int[][] newField = new int[newFieldWidth][newFieldHeight];
            for (int x = 0; x < newFieldWidth; x++) {
                for (int y = 0; y < newFieldHeight; y++) {
                    newField[x][y] = 0;
                }
            }
            for (int x = 0; x < Math.min(fieldWidth, newFieldWidth); x++) {
                System.arraycopy(field[x], 0, newField[x], 0, Math.min(fieldHeight, newFieldHeight));
            }

            viewHeight = w;
            viewWidth = h;
            field = newField;
            fieldHeight = newFieldHeight;
            fieldWidth = newFieldWidth;
        }
    }

    public void clearField() {
        for (int x = 0; x < fieldWidth; x++) {
            for (int y = 0; y < fieldHeight; y++) {
                field[x][y] = 0;
            }
        }
    }

    public void initField() {
        field = new int[fieldWidth][fieldHeight];
        clearField();
        drawFigure(Figures.glider);
    }

    public void drawFigure(int[][] figure) {
        final int delta = 3;
        if (figure != null && (figure.length + delta > fieldHeight || figure[0].length + delta > fieldWidth)) {
            Toast.makeText(baseActivity.getApplicationContext(), R.string.not_enough_space, Toast.LENGTH_SHORT).show();
        } else {
            clearField();
            if (figure != null) {
                for (int y = 0; y < figure.length; y++) {
                    for (int x = 0; x < figure[0].length; x++) {
                        field[x + delta][y + delta] = figure[y][x];
                    }
                }
            }
        }
    }
}
