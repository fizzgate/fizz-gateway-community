/*
 *  Copyright (C) 2020 the original author or authors.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.fizzgate.util;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author hongqiaowei
 */

public abstract class ReactorUtils {

    public static final Object        OBJ               = new Object();

    public static final Object        NULL              = OBJ;

    public static final Object        Void              = OBJ;

    @Deprecated
    public static final Throwable     EMPTY_THROWABLE   = Utils.throwableWithoutStack(null);

    private ReactorUtils() {
    }

    public static Mono<?> getInitiateMono() {
        return Mono.just(OBJ);
    }

    public static Flux<?> getInitiateFlux() {
        return Flux.just(OBJ);
    }
}
