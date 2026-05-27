/*
 * Copyright (C) 2026 Livelandr
 *
 * This file is part of Flintcore.
 *
 * Flintcore is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Flintcore is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package com.livelandr.flintcore.core.util;

public class TickDelayTask {
    int ticksLeft = 0;
    boolean done = false;
    Runnable func;

    TickDelayTask(int ticks, Runnable action) {
        this.ticksLeft = ticks;
        func = action;
    }

    boolean tick() {
        if (!done && --ticksLeft <= 0) {
            func.run();
            done = true;
            return true;
        }

        return false;
    }
}
