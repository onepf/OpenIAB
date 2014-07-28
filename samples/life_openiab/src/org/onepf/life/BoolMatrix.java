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

import android.os.Parcel;
import android.os.Parcelable;

import org.onepf.life.util.ParcelableUtils;

import java.util.Arrays;

/**
 * Created by krozov on 7/25/14.
 */
public abstract class BoolMatrix implements Parcelable {
    protected final boolean[][] field;
    private final int width;
    private final int height;

    protected BoolMatrix(int[][] points) {
        width = points.length;
        height = points[0].length;
        this.field = new boolean[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                this.field[x][y] = points[x][y] != 0;
            }
        }
    }

    protected BoolMatrix(int width, int height) {
        this.width = width;
        this.height = height;
        this.field = new boolean[width][height];
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        ParcelableUtils.writeArray(dest, this.field);
        dest.writeInt(this.width);
        dest.writeInt(this.height);
    }

    protected BoolMatrix(Parcel in) {
        this.field = ParcelableUtils.readArray(in);
        this.width = in.readInt();
        this.height = in.readInt();
    }

    public void clear() {
        for (boolean[] row : field) {
            Arrays.fill(row, false);
        }
    }

    public void markAt(int x, int y, boolean marked) {
        field[x][y] = marked;
    }

    public boolean isMarked(int x, int y) {
        return field[x][y];
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        BoolMatrix matrix = (BoolMatrix) o;
        return height == matrix.height
                && width == matrix.width
                && Arrays.deepEquals(this.field, matrix.field);

    }

    @Override
    public int hashCode() {
        int result = this.width;
        result = 31 * result + this.height;
        result = 31 * result + Arrays.deepHashCode(this.field);
        return result;
    }
}
