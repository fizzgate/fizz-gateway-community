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

package com.fizzgate.plugin;

import com.fizzgate.util.Consts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;

import com.fizzgate.Fizz;
import com.fizzgate.FizzAppContext;
import com.fizzgate.proxy.Route;
import com.fizzgate.util.ReactorUtils;
import com.fizzgate.util.WebUtils;

import reactor.core.publisher.Mono;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author hongqiaowei
 */

public final class FizzPluginFilterChain {

    private static final Logger LOGGER = LoggerFactory.getLogger(FizzPluginFilterChain.class);

    private static final String pluginConfigsIt  = "pcsit@";

    public  static final String WEB_FILTER_CHAIN = "wfc@";

    private FizzPluginFilterChain() {
    }

    public static Mono<Void> next(ServerWebExchange exchange) {
        if (WebUtils.ignorePlugin(exchange)) {
            WebFilterChain chain = exchange.getAttribute(WEB_FILTER_CHAIN);
            return chain.filter(exchange);
        }
        Iterator<PluginConfig> it = exchange.getAttribute(pluginConfigsIt);
        Route route = WebUtils.getRoute(exchange);
        if (it == null || route.pluginConfigsChange) {
            List<PluginConfig> pcs = route.pluginConfigs;
            it = pcs.iterator();
            Map<String, Object> attris = exchange.getAttributes();
            attris.put(pluginConfigsIt, it);
            route.pluginConfigsChange = false;
        }
        if (it.hasNext()) {
            PluginConfig pc = it.next();
            FizzPluginFilter pf = Fizz.context.getBean(pc.plugin, FizzPluginFilter.class);

            String traceId = WebUtils.getTraceId(exchange);
            if (LOGGER.isDebugEnabled()) {
                org.apache.logging.log4j.ThreadContext.put(Consts.TRACE_ID, traceId);
                LOGGER.debug("{} start", pc.plugin);
            }

            Mono<Void> m = pf.filter(exchange, pc.config);
            if (pf instanceof PluginFilter) {
                boolean f = false;
                while (it.hasNext()) {
                    PluginConfig pc0 = it.next();
                    FizzPluginFilter pf0 = Fizz.context.getBean(pc0.plugin, FizzPluginFilter.class);
                    m = m.thenReturn(ReactorUtils.Void).flatMap(
                                                               v -> {
                                                                   return pf0.filter(exchange, pc0.config);
                                                               }
                                                       );
                    if (pf0 instanceof PluginFilter) {
                    } else {
                        f = true;
                        break;
                    }
                }
                if (!f && !it.hasNext()) {
                    WebFilterChain chain = exchange.getAttribute(WEB_FILTER_CHAIN);
                    m = m.thenReturn(ReactorUtils.Void).flatMap(
                                                               v -> {
                                                                   return chain.filter(exchange);
                                                               }
                                                       );
                }
            }
            return m;
        } else {
            WebFilterChain chain = exchange.getAttribute(WEB_FILTER_CHAIN);
            return chain.filter(exchange);
        }
    }

    @Deprecated
    public static Mono<Void> next(ServerWebExchange exchange, List<PluginConfig> pcs) {
        Iterator<PluginConfig> it = pcs.iterator();
        Map<String, Object> attris = exchange.getAttributes();
        attris.put(pluginConfigsIt, it);
        return next(exchange);
    }
}
