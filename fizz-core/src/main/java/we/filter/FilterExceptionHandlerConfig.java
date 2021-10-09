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

package we.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;
import we.config.SystemConfig;
import we.exception.ExecuteScriptException;
import we.exception.RedirectException;
import we.exception.StopAndResponseException;
import we.fizz.exception.FizzRuntimeException;
import we.flume.clients.log4j2appender.LogService;
import we.legacy.RespEntity;
import we.util.*;

import java.net.URI;

/**
 * @author hongqiaowei
 */

@Configuration
public class FilterExceptionHandlerConfig {

    public static class FilterExceptionHandler implements WebExceptionHandler {

        private static final Logger log = LoggerFactory.getLogger(FilterExceptionHandler.class);
        private static final String filterExceptionHandler = "filterExceptionHandler";

        @Override
        public Mono<Void> handle(ServerWebExchange exchange, Throwable t) {
            String traceId = WebUtils.getTraceId(exchange);
            ServerHttpResponse resp = exchange.getResponse();
            if (SystemConfig.FIZZ_ERR_RESP_HTTP_STATUS_ENABLE) {
                if (t instanceof ResponseStatusException) {
                    resp.setStatusCode( ((ResponseStatusException) t).getStatus() );
                } else {
                    resp.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
                }
            }

            if (t instanceof StopAndResponseException) {
                StopAndResponseException ex = (StopAndResponseException) t;
                if (ex.getData() != null) {
                    resp.getHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
                    return resp.writeWith(Mono.just(resp.bufferFactory().wrap(ex.getData().toString().getBytes())));
                }
            }
            if (t instanceof RedirectException) {
                RedirectException ex = (RedirectException) t;
                if (ex.getRedirectUrl() != null) {
                    resp.setStatusCode(HttpStatus.MOVED_PERMANENTLY);
                    resp.getHeaders().setLocation(URI.create(ex.getRedirectUrl()));
                    return Mono.empty();
                }
            }

            if (t instanceof ExecuteScriptException) {
                ExecuteScriptException ex = (ExecuteScriptException) t;
                resp.getHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
                RespEntity rs = null;
                if (ex.getStepContext() != null && ex.getStepContext().returnContext()) {
                    rs = new RespEntity(HttpStatus.INTERNAL_SERVER_ERROR.value(), t.getMessage(), traceId, ex.getStepContext());
                    return resp.writeWith(Mono.just(resp.bufferFactory().wrap(JacksonUtils.writeValueAsString(rs).getBytes())));
                } else {
                    rs = new RespEntity(HttpStatus.INTERNAL_SERVER_ERROR.value(), t.getMessage(), traceId);
                    return resp.writeWith(Mono.just(resp.bufferFactory().wrap(rs.toString().getBytes())));
                }
            }

            if (t instanceof FizzRuntimeException) {
                FizzRuntimeException ex = (FizzRuntimeException) t;
                log.error(ex.getMessage(), LogService.BIZ_ID, traceId, ex);
                resp.getHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
                RespEntity rs = null;
                if (ex.getStepContext() != null && ex.getStepContext().returnContext()) {
                    rs = new RespEntity(HttpStatus.INTERNAL_SERVER_ERROR.value(), t.getMessage(), traceId, ex.getStepContext());
                    return resp.writeWith(Mono.just(resp.bufferFactory().wrap(JacksonUtils.writeValueAsString(rs).getBytes())));
                } else {
                    rs = new RespEntity(HttpStatus.INTERNAL_SERVER_ERROR.value(), t.getMessage(), traceId);
                    return resp.writeWith(Mono.just(resp.bufferFactory().wrap(rs.toString().getBytes())));
                }
            }

            Mono<Void> vm;
            Object fc = exchange.getAttribute(WebUtils.FILTER_CONTEXT);
            if (fc == null) { // t came from flow control filter
                StringBuilder b = ThreadContext.getStringBuilder();
                WebUtils.request2stringBuilder(exchange, b);
                log.error(b.toString(), LogService.BIZ_ID, traceId, t);
                String s = WebUtils.jsonRespBody(HttpStatus.INTERNAL_SERVER_ERROR.value(), t.getMessage(), traceId);
                resp.getHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
                vm = resp.writeWith(Mono.just(resp.bufferFactory().wrap(s.getBytes())));
            } else {
                vm = WebUtils.responseError(exchange, filterExceptionHandler, HttpStatus.INTERNAL_SERVER_ERROR.value(), t.getMessage(), t);
            }
            return vm;
        }
    }

    @Bean
    @Order(-10)
    public FilterExceptionHandler filterExceptionHandler() {
        return new FilterExceptionHandler();
    }
}
