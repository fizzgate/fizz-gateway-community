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

package com.fizzgate.plugin.requestbody;

import org.apache.logging.log4j.ThreadContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.fizzgate.plugin.FizzPluginFilter;
import com.fizzgate.plugin.FizzPluginFilterChain;
import com.fizzgate.spring.http.server.reactive.ext.FizzServerHttpRequestDecorator;
import com.fizzgate.spring.web.server.ext.FizzServerWebExchangeDecorator;
import com.fizzgate.util.Consts;
import com.fizzgate.util.NettyDataBufferUtils;
import com.fizzgate.util.WebUtils;

import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Your plugin P can extend this class and override the doFilter method, then you can modify the request later.
 *
 * @author hongqiaowei
 */

@Component(RequestBodyPlugin.REQUEST_BODY_PLUGIN)
public class RequestBodyPlugin implements FizzPluginFilter {

    private final Logger LOGGER = LoggerFactory.getLogger(getClass());

    public static final String REQUEST_BODY_PLUGIN = "requestBodyPlugin";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, Map<String, Object> config) {

        // String traceId = WebUtils.getTraceId(exchange);
        // if (LOGGER.isDebugEnabled()) {
        //     org.apache.logging.log4j.ThreadContext.put(Consts.TRACE_ID, traceId);
        //     LOGGER.debug("{} start", getClass().getSimpleName());
        // }

        ServerHttpRequest req = exchange.getRequest();
        if (req instanceof FizzServerHttpRequestDecorator) {
            return doFilter(exchange, config);
        }
        return
                NettyDataBufferUtils.join(req.getBody()).defaultIfEmpty(NettyDataBufferUtils.EMPTY_DATA_BUFFER)
                        .flatMap(
                                body -> {
                                    FizzServerHttpRequestDecorator requestDecorator = new FizzServerHttpRequestDecorator(req);
                                    if (body != NettyDataBufferUtils.EMPTY_DATA_BUFFER) {
                                        try {
                                            requestDecorator.setBody(body);
                                        } finally {
                                            NettyDataBufferUtils.release(body);
                                        }
                                        // requestDecorator.getHeaders().remove(HttpHeaders.CONTENT_LENGTH);
                                    }
                                    ServerWebExchange mutatedExchange = exchange.mutate().request(requestDecorator).build();
                                    ServerWebExchange newExchange = mutatedExchange;
                                    MediaType contentType = req.getHeaders().getContentType();
                                    if (MediaType.APPLICATION_FORM_URLENCODED.isCompatibleWith(contentType)) {
                                        newExchange = new FizzServerWebExchangeDecorator(mutatedExchange);
                                    }
                                    // if (LOGGER.isDebugEnabled()) {
                                    //     ThreadContext.put(Consts.TRACE_ID, traceId);
                                    //     LOGGER.debug("{} request is decorated", traceId);
                                    // }
                                    return doFilter(newExchange, config);
                                }
                        );
    }

    public Mono<Void> doFilter(ServerWebExchange exchange, Map<String, Object> config) {
        String traceId = WebUtils.getTraceId(exchange);
        if (LOGGER.isDebugEnabled()) {
            org.apache.logging.log4j.ThreadContext.put(Consts.TRACE_ID, traceId);
            LOGGER.debug("{} end", getClass().getSimpleName());
        }
        return FizzPluginFilterChain.next(exchange);
    }
}
