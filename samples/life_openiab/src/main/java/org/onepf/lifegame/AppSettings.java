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

package org.onepf.lifegame;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.onepf.lifegame.util.SharedPreferencesEditorCompat;

/**
 * Created by krozov on 7/24/14.
 */
public final class AppSettings {

    private static final String KEY_HAS_ORANGE_CELLS = "has_orange_cells";
    private static final String KEY_HAS_FIGURES = "has_figures";
    private static final String KEY_CHANGES_COUNT = "changes_count";
    private static final int DEFAULT_CHANGES_COUNT = 50;

    private static AppSettings instance;

    private final SharedPreferences prefs;

    public static AppSettings getInstance(Context context) {
        if (instance == null) {
            instance = new AppSettings(context);
        }
        return instance;
    }

    private AppSettings(Context context) {
        prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
    }

    public boolean isHasFigures() {
        return prefs.getBoolean(KEY_HAS_FIGURES, false);
    }

    public void setHasFigures(boolean hasFigures) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_HAS_FIGURES, hasFigures);
        SharedPreferencesEditorCompat.apply(editor);
    }

    public boolean isHasOrangeCells() {
        return prefs.getBoolean(KEY_HAS_ORANGE_CELLS, false);
    }

    public void setHasOrangeCells(boolean hasOrangeCells) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_HAS_ORANGE_CELLS, hasOrangeCells);
        SharedPreferencesEditorCompat.apply(editor);
    }

    public int getChangesCount() {
        return prefs.getInt(KEY_CHANGES_COUNT, DEFAULT_CHANGES_COUNT);
    }

    public void setChangesCount(int changesCount) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_CHANGES_COUNT, changesCount);
        SharedPreferencesEditorCompat.apply(editor);
    }
}
