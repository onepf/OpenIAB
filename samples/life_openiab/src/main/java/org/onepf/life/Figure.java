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

import java.util.Arrays;

/**
 * Package org.onepf.life
 * Author: Ruslan Sayfutdinov, Kirill Rozov
 * Date: 13/03/13
 */
public final class Figure extends BoolMatrix {
    public static final Figure GLIDER = new Figure(
            new int[][]{
                    {0, 1, 0},
                    {0, 0, 1},
                    {1, 1, 1}
            });

    public static final Figure BIG_GLIDER = new Figure(
            new int[][]{
                    {0, 0, 0, 1, 0},
                    {0, 0, 0, 0, 1},
                    {1, 0, 0, 0, 1},
                    {0, 1, 1, 1, 1}
            });

    public static final Figure PERIODIC = new Figure(
            new int[][]{
                    {0, 0, 1, 0, 0, 0, 0, 1, 0, 0},
                    {1, 1, 0, 1, 1, 1, 1, 0, 1, 1},
                    {0, 0, 1, 0, 0, 0, 0, 1, 0, 0}
            });

    public static final Figure ROBOT = new Figure(
            new int[][]{
                    {0, 0, 0, 1, 1, 0, 1, 1, 0, 0, 0},
                    {0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0},
                    {0, 0, 0, 0, 1, 1, 1, 0, 0, 0, 0},
                    {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                    {0, 0, 0, 0, 1, 1, 1, 0, 0, 0, 0},
                    {1, 1, 0, 1, 0, 1, 0, 1, 0, 1, 1},
                    {1, 1, 0, 1, 0, 0, 0, 1, 0, 1, 1},
                    {0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0},
                    {0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0},
                    {0, 0, 0, 0, 1, 1, 1, 0, 0, 0, 0},
                    {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                    {0, 0, 0, 0, 1, 1, 1, 0, 0, 0, 0},
                    {0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0},
                    {0, 0, 0, 1, 1, 0, 1, 1, 0, 0, 0}
            });

    private Figure(int[][] points) {
        super(points);
    }

    private Figure(Parcel in) {
        super(in);
    }

    public static final Parcelable.Creator<Figure> CREATOR = new Parcelable.Creator<Figure>() {
        public Figure createFromParcel(Parcel source) {
            return new Figure(source);
        }

        public Figure[] newArray(int size) {
            return new Figure[size];
        }
    };
}