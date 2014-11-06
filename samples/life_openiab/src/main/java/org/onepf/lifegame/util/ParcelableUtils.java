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

package org.onepf.lifegame.util;

import android.os.Parcel;

import org.jetbrains.annotations.NotNull;

/**
 * Created by krozov on 7/25/14.
 */
public final class ParcelableUtils {

    private ParcelableUtils() {

    }

    public static void writeArray(@NotNull Parcel out, boolean[][] array) {
        out.writeInt(array.length);
        for (boolean[] booleans : array) {
            out.writeBooleanArray(booleans);
        }
    }

    public static boolean[][] readArray(@NotNull Parcel in) {
        boolean[][] array = new boolean[in.readInt()][];
        if (array.length > 0) {
            for (int i = 0; i < array.length; i++) {
                array[i] = in.createBooleanArray();
            }
        }
        return array;
    }
}
