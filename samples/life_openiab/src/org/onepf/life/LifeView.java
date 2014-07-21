/*******************************************************************************
 * Copyright 2013-2014 One Platform Foundation
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

package org.onepf.life;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;

import java.util.Arrays;

public class LifeView extends SurfaceView {
    private static final int FIELD_PADDING = 3;

    private int cellWidth;
    private int cellHeight;

    // Field
    private int fieldWidth;
    private int fieldHeight;
    private int[][] field;

    // Edits
    private boolean editMode = true;
    private int changeCount;

    // Cell images
    private Bitmap activeCellBitmap;
    private Bitmap emptyCellBitmap;

    private LifeViewListener listener;


    public LifeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        changeCount = LifeViewListener.DEFAULT_CHANGES_COUNT;
        activeCellBitmap = BitmapFactory.decodeResource(getResources(),
                R.drawable.cell_active_green);
        emptyCellBitmap = BitmapFactory.decodeResource(getResources(),
                R.drawable.cell_empty);
        cellWidth = activeCellBitmap.getWidth();
        cellHeight = activeCellBitmap.getHeight();
    }

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
        invalidate();
    }

    public int getChangeCount() {
        return changeCount;
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
                    if (listener != null) {
                        listener.setChangeCount(changeCount);
                    }
                    field[cellX][cellY] = 1 - field[cellX][cellY];
                } else {
                    if (listener != null) {
                        listener.onNoChangesFound();
                    }
                }
            }
        }

        return true;
    }

    private int getNeighbour(int x, int y, int dx, int dy) {
        int x2 = (x + dx + fieldWidth) % fieldWidth;
        int y2 = (y + dy + fieldHeight) % fieldHeight;
        return field[x2][y2];
    }

    private int getNeighboursCount(int x, int y) {
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

    public void updateField() {
        if (!editMode) {
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
    }

    @Override
    public void onDraw(Canvas canvas) {
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
        paint.setColor(getResources().getColor(android.R.color.primary_text_dark));
        paint.setAlpha(160);
        paint.setTextSize((float) 0.7 * cellHeight);
        canvas.drawText("Changes: " + changeCount, cellWidth / 2, cellHeight, paint);
    }

    @Override
    public void onSizeChanged(int w, int h, int oldW, int oldH) {
        Log.d(GameActivity.TAG, "Old size: w = " + fieldWidth + ", h = " + fieldHeight);
        if (field == null) {
            fieldHeight = h / cellWidth;
            fieldWidth = w / cellHeight;
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

            field = newField;
            fieldHeight = newFieldHeight;
            fieldWidth = newFieldWidth;
        }
    }

    public void clearField() {
        for (int x = 0; x < fieldWidth; x++) {
            int[] row = field[x];
            Arrays.fill(row, 0);
        }
    }

    public void initField() {
        field = new int[fieldWidth][fieldHeight];
        clearField();
        drawFigure(Figures.glider);
    }

    public void drawFigure(int[][] figure) {
        if (figure != null && (figure.length + FIELD_PADDING > fieldHeight || figure[0].length + FIELD_PADDING > fieldWidth)) { //todo fix width check
            if (listener != null) {
                listener.notEnoughSpaceForFigure();
            }
        } else {
            clearField();
            if (figure != null) {
                for (int y = 0; y < figure.length; y++) {
                    for (int x = 0; x < figure[0].length; x++) {
                        field[x + FIELD_PADDING][y + FIELD_PADDING] = figure[y][x];
                    }
                }
            }
        }
    }

    public void setUp(LifeViewListener controller, int changeCount) {
        this.listener = controller;
        setChangeCount(changeCount);
    }
}
