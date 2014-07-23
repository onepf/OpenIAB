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

/**
 * Package org.onepf.life
 * Author: Ruslan Sayfutdinov
 * Date: 13/03/13
 */
public abstract class Figures {
    public static final int[][] glider = new int[][]
            {{0, 1, 0},
                    {0, 0, 1},
                    {1, 1, 1}};

    public static final int[][] bigGlider = new int[][]
            {{0, 0, 0, 1, 0},
                    {0, 0, 0, 0, 1},
                    {1, 0, 0, 0, 1},
                    {0, 1, 1, 1, 1}};

    public static final int[][] periodic = new int[][]
            {{0, 0, 1, 0, 0, 0, 0, 1, 0, 0},
                    {1, 1, 0, 1, 1, 1, 1, 0, 1, 1},
                    {0, 0, 1, 0, 0, 0, 0, 1, 0, 0}};

    public static final int[][] robot = new int[][]
            {{0, 0, 0, 1, 1, 0, 1, 1, 0, 0, 0},
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
                    {0, 0, 0, 1, 1, 0, 1, 1, 0, 0, 0}};
}