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


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.DrawableRes;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public class LifeView extends SurfaceView {
    private static final int FIELD_PADDING = 3;

    private final int cellWidth;
    private final int cellHeight;

    private Field field;
    private Figure figure = Figure.GLIDER;

    // Edits
    private boolean editMode = true;
    private int changeCount;

    // Cell images
    private Bitmap activeCellBitmap;
    private Bitmap emptyCellBitmap;

    private Listener listener;
    private final Paint paint;

    private final Runnable redrawRunnable = new Runnable() {
        @Override
        public void run() {
            Canvas canvas = getHolder().lockCanvas();
            updateField();
            getHolder().unlockCanvasAndPost(canvas);
            invalidate();
            postDelayed(this, 16L);
        }
    };

    public LifeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        activeCellBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.cell_active_green);
        emptyCellBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.cell_empty);
        cellWidth = activeCellBitmap.getWidth();
        cellHeight = activeCellBitmap.getHeight();

        paint = new Paint();
        paint.setColor(getResources().getColor(android.R.color.primary_text_light));
        paint.setAlpha(160);
        paint.setTextSize((float) 0.7 * cellHeight);

        setWillNotDraw(false);

        if (!this.editMode) {
            postDelayed(redrawRunnable, 16L);
        }
    }

    public void setActiveCellBitmap(@DrawableRes int bitmapResId) {
        activeCellBitmap = BitmapFactory.decodeResource(getResources(), bitmapResId);
    }

    public void setEditMode(boolean editMode) {
        this.editMode = editMode;
        if (editMode) {
            removeCallbacks(redrawRunnable);
        } else {
            postDelayed(redrawRunnable, 16L);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        removeCallbacks(redrawRunnable);
        super.onDetachedFromWindow();
    }

    public boolean isInEditMode() {
        return editMode;
    }

    public void setChangeCount(int changeCount) {
        if (this.changeCount != changeCount) {
            this.changeCount = changeCount;
            invalidate();
        }
    }

    public int getChangeCount() {
        return changeCount;
    }

    @Override
    public boolean onTouchEvent(@NotNull MotionEvent event) {
        if (editMode) {
            int cellX = (int) (event.getX() / cellWidth);
            int cellY = (int) (event.getY() / cellHeight);
            int action = event.getAction();
            if (action == MotionEvent.ACTION_DOWN && cellX >= 0 && cellY >= 0
                    && cellX < field.getWidth() && cellY < field.getHeight()) {
                if (getChangeCount() > 0) {
                    changeCount--;
                    if (listener != null) {
                        listener.onChangeCountModified(changeCount);
                    }
                    field.markAt(cellX, cellY, !field.isMarked(cellX, cellY));
                } else {
                    if (listener != null) {
                        listener.onNoChangesFound();
                    }
                }
            }
            invalidate();
        }

        return true;
    }

    private boolean getNeighbour(int x, int y, int dx, int dy) {
        return field.isMarked(
                (x + dx + field.getWidth()) % field.getWidth(),
                (y + dy + field.getHeight()) % field.getHeight()
        );
    }

    private int getNeighboursCount(int x, int y) {
        int count = 0;

        // Check all the cell's neighbours
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                // except the cell itself
                if (dx != 0 || dy != 0) {
                    count += getNeighbour(x, y, dx, dy) ? 1 : 0;
                }
            }
        }
        return count;
    }

    public void updateField() {
        if (!editMode) {
            final Field field2 = new Field(field.getWidth(), field.getHeight());
            for (int x = 0; x < field2.getWidth(); x++) {
                for (int y = 0; y < field2.getHeight(); y++) {
                    switch (getNeighboursCount(x, y)) {
                        case 2:
                            field2.markAt(x, y, field.isMarked(x, y));
                            break;

                        case 3:
                            field2.markAt(x, y, true);
                            break;
                    }
                }
            }
            field = field2;
        }
    }

    @Override
    public void onDraw(Canvas canvas) {
        for (int x = 0; x < field.getWidth(); x++) {
            for (int y = 0; y < field.getHeight(); y++) {
                canvas.drawBitmap(field.isMarked(x, y) ? activeCellBitmap : emptyCellBitmap, x * cellWidth, y * cellHeight, null);
            }
        }
        canvas.drawText("Changes: " + changeCount, cellWidth / 2, cellHeight, paint);
    }

    @Override
    public void onSizeChanged(int w, int h, int oldW, int oldH) {
        if (field != null) {
            Log.d(GameActivity.TAG, "Old size: w = " + field.getWidth() + ", h = " + field.getHeight());
        }
        if (field == null) {
            field = new Field(w / cellHeight, h / cellWidth);
            drawFigure(this.figure);
        } else {
            field = new Field(w / cellWidth, h / cellHeight, field);
        }
        Log.d(GameActivity.TAG, "New size: w = " + field.getWidth() + ", h = " + field.getHeight());
    }

    public void drawFigure(Figure figure) {
        this.figure = figure;
        if (figure == null) {
            field.clear();
        } else {
            if (figure.getWidth() + FIELD_PADDING > field.getHeight()
                    || figure.getHeight() + FIELD_PADDING > field.getWidth()) { //todo fix width check
                if (listener != null) {
                    listener.onNotEnoughSpaceForFigure(figure);
                }
            } else {
                for (int y = 0; y < figure.getWidth(); y++) {
                    final int y1 = y + FIELD_PADDING;
                    for (int x = 0; x < figure.getHeight(); x++) {
                        field.markAt(x + FIELD_PADDING, y1, figure.isMarked(x, y));
                    }
                }
            }
        }
    }

    public void setUp(Listener l, int changeCount) {
        this.listener = l;
        setChangeCount(changeCount);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState ss = new SavedState(superState);
        ss.figure = this.figure;
        ss.field = this.field;
        return ss;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state instanceof SavedState) {
            SavedState ss = (SavedState) state;
            super.onRestoreInstanceState(ss);
            this.field = ss.field;
            updateField();
        } else {
            super.onRestoreInstanceState(state);
        }
    }

    private static final class Field extends BoolMatrix {

        protected Field(int width, int height) {
            super(width, height);
        }

        private Field(int width, int height, Field oldField) {
            this(width, height);

            final int w = Math.min(width, oldField.getWidth());
            final int h = Math.min(height, oldField.getHeight());
            for (int x = 0; x < w; x++) {
                System.arraycopy(oldField.field[x], 0, field[x], 0, h);
            }
        }

        private Field(Parcel in) {
            super(in);
        }

        public static final Creator<Field> CREATOR = new Creator<Field>() {
            @Override
            public Field createFromParcel(Parcel parcel) {
                return new Field(parcel);
            }

            @Override
            public Field[] newArray(int length) {
                return new Field[length];
            }
        };
    }

    public final static class SavedState extends BaseSavedState {
        private Field field;
        private Figure figure;

        SavedState(Parcelable superState) {
            super(superState);
        }

        SavedState(Parcel source) {
            super(source);
            this.field = source.readParcelable(Field.class.getClassLoader());
            this.figure = source.readParcelable(Figure.class.getClassLoader());
        }

        @Override
        public void writeToParcel(@NotNull Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeParcelable(field, 0);
            dest.writeParcelable(figure, 0);
        }

        public static final Creator<SavedState> CREATOR = new Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel source) {
                return new SavedState(source);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

    public static interface Listener {
        void onChangeCountModified(int newChangeCount);

        void onNoChangesFound();

        void onNotEnoughSpaceForFigure(Figure figure);
    }
}
