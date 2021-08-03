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

package we.util;

import io.netty.buffer.UnpooledByteBufAllocator;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import we.config.SystemConfig;
import we.constants.CommonConstants;
import we.filter.FilterResult;
import we.flume.clients.log4j2appender.LogService;
import we.legacy.RespEntity;
import we.plugin.auth.ApiConfig;
import we.plugin.auth.AuthPluginFilter;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author hongqiaowei
 */

public abstract class WebUtils {

    private static final Logger log = LoggerFactory.getLogger(WebUtils.class);

    private static final String clientService = "clientService";

    private static final String xForwardedFor = "X-FORWARDED-FOR";

    private static final String unknown = "unknown";

    private static final String loopBack = "127.0.0.1";

    private static final String binaryAddress = "0:0:0:0:0:0:0:1";

    private static final String directResponse = "directResponse";

    private static final String response = " response ";

    private static final String originIp = "originIp";

    private static final String clientRequestPath = "clientRequestPath";

    private static final String clientRequestPathPrefix = "clientRequestPathPrefix";

    private static final String clientRequestQuery = "clientRequestQuery";

    private static final String traceId = "traceId";

    private static String gatewayPrefix = SystemConfig.DEFAULT_GATEWAY_PREFIX;

    private static List<String> appHeaders = Stream.of("fizz-appid").collect(Collectors.toList());

    private static final String app = "app";

    public static final String BACKEND_SERVICE = "backendService";

    public static final String FILTER_CONTEXT = "filterContext";

    public static final String APPEND_HEADERS = "appendHeaders";

    public static final String PREV_FILTER_RESULT = "prevFilterResult";

    public static final String BACKEND_PATH = "backendPath";

    public static boolean LOG_RESPONSE_BODY = false;

    public static Set<String> LOG_HEADER_SET = Collections.EMPTY_SET;

    public static final DataBuffer EMPTY_BODY = new NettyDataBufferFactory(new UnpooledByteBufAllocator(false, true)).wrap(Constants.Symbol.EMPTY.getBytes());


    public static void setGatewayPrefix(String p) {
        gatewayPrefix = p;
    }

    public static void setAppHeaders(List<String> hdrs) {
        appHeaders = hdrs;
    }

    public static String getHeaderValue(ServerWebExchange exchange, String header) {
        return exchange.getRequest().getHeaders().getFirst(header);
    }

    public static List<String> getHeaderValues(ServerWebExchange exchange, String header) {
        return exchange.getRequest().getHeaders().get(header);
    }

    public static String getAppId(ServerWebExchange exchange) {
        String a = exchange.getAttribute(app);
        if (a == null) {
            HttpHeaders headers = exchange.getRequest().getHeaders();
            for (int i = 0; i < appHeaders.size(); i++) {
                a = headers.getFirst(appHeaders.get(i));
                if (a != null) {
                    exchange.getAttributes().put(app, a);
                    break;
                }
            }
        }
        return a;
    }

    public static String getClientService(ServerWebExchange exchange) {
        String svc = exchange.getAttribute(clientService);
        if (svc == null) {
            String p = exchange.getRequest().getPath().value();
            int secFS = p.indexOf(Constants.Symbol.FORWARD_SLASH, 1);
            if (StringUtils.isBlank(gatewayPrefix) || Constants.Symbol.FORWARD_SLASH_STR.equals(gatewayPrefix)) {
                svc = p.substring(1, secFS);
            } else {
                String prefix = p.substring(0, secFS);
                if (gatewayPrefix.equals(prefix) || SystemConfig.DEFAULT_GATEWAY_TEST_PREFIX.equals(prefix)) {
                    int trdFS = p.indexOf(Constants.Symbol.FORWARD_SLASH, secFS + 1);
                    svc = p.substring(secFS + 1, trdFS);
                } else {
                    throw Utils.runtimeExceptionWithoutStack("wrong prefix " + prefix);
                }
            }
            exchange.getAttributes().put(clientService, svc);
        }
        return svc;
    }

    public static void setBackendService(ServerWebExchange exchange, String service) {
        exchange.getAttributes().put(BACKEND_SERVICE, service);
    }

    public static String getBackendService(ServerWebExchange exchange) {
        return exchange.getAttribute(BACKEND_SERVICE);
    }

    public static byte getApiConfigType(ServerWebExchange exchange) {
        ApiConfig ac = getApiConfig(exchange);
        if (ac == null) {
            return ApiConfig.Type.UNDEFINED;
        } else {
            return ac.type;
        }
    }

    public static ApiConfig getApiConfig(ServerWebExchange exchange) {
        Object authRes = getFilterResultDataItem(exchange, AuthPluginFilter.AUTH_PLUGIN_FILTER, AuthPluginFilter.RESULT);
        if (authRes != null && authRes instanceof ApiConfig) {
            return (ApiConfig) authRes;
        } else {
            return null;
        }
    }

    public static Mono<Void> getDirectResponse(ServerWebExchange exchange) {
        return (Mono<Void>) exchange.getAttributes().get(WebUtils.directResponse);
    }

    public static Map<String, FilterResult> getFilterContext(ServerWebExchange exchange) {
        return (Map<String, FilterResult>) exchange.getAttribute(FILTER_CONTEXT);
    }

    public static FilterResult getFilterResult(ServerWebExchange exchange, String filter) {
        return getFilterContext(exchange).get(filter);
    }

    public static Map<String, Object> getFilterResultData(ServerWebExchange exchange, String filter) {
        return getFilterResult(exchange, filter).data;
    }

    public static Object getFilterResultDataItem(ServerWebExchange exchange, String filter, String key) {
        return getFilterResultData(exchange, filter).get(key);
    }

    public static Mono<Void> buildDirectResponse(ServerWebExchange exchange, HttpStatus status, HttpHeaders headers, String bodyContent) {
        return buildDirectResponse(exchange.getResponse(), status, headers, bodyContent);
    }

    public static Mono buildDirectResponseAndBindContext(ServerWebExchange exchange, HttpStatus status, HttpHeaders headers, String bodyContent) {
        Mono<Void> mv = buildDirectResponse(exchange, status, headers, bodyContent);
        exchange.getAttributes().put(WebUtils.directResponse, mv);
        return mv;
    }

    public static Mono buildJsonDirectResponse(ServerWebExchange exchange, HttpStatus status, HttpHeaders headers, String json) {
        if (headers == null) {
            headers = new HttpHeaders();
        }
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        return buildDirectResponse(exchange, status, headers, json);
    }

    public static Mono buildJsonDirectResponseAndBindContext(ServerWebExchange exchange, HttpStatus status, HttpHeaders headers, String json) {
        if (headers == null) {
            headers = new HttpHeaders();
        }
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        return buildDirectResponseAndBindContext(exchange, status, headers, json);
    }

    public static Mono<Void> buildDirectResponse(ServerHttpResponse clientResp, HttpStatus status, HttpHeaders headers, String bodyContent) {
        if (clientResp.isCommitted()) {
            log.warn(bodyContent + ", but client resp is committed, " + clientResp.getStatusCode());
            return Mono.error(new RuntimeException(bodyContent, null, false, false) {
            });
        }
        if (status != null) {
            clientResp.setStatusCode(status);
        }
        if (headers != null) {
            headers.forEach(
                    (h, vs) -> {
                        clientResp.getHeaders().addAll(h, vs);
                    }
            );
        }
        if (bodyContent == null) {
            bodyContent = Constants.Symbol.EMPTY;
        }
        return clientResp
                .writeWith(Mono.just(clientResp.bufferFactory().wrap(bodyContent.getBytes())));
    }

    public static void transmitSuccessFilterResult(ServerWebExchange exchange, String filter, Map<String, Object> data) {
        FilterResult fr = FilterResult.SUCCESS_WITH(filter, data);
        bind(exchange, filter, fr);
    }

    public static Mono transmitSuccessFilterResultAndEmptyMono(ServerWebExchange exchange, String filter, Map<String, Object> data) {
        transmitSuccessFilterResult(exchange, filter, data);
        return Mono.empty();
    }

    public static void transmitFailFilterResult(ServerWebExchange exchange, String filter) {
        FilterResult fr = FilterResult.FAIL(filter);
        bind(exchange, filter, fr);
    }

    public static void transmitFailFilterResult(ServerWebExchange exchange, String filter, Throwable cause) {
        FilterResult fr = FilterResult.FAIL_WITH(filter, cause);
        bind(exchange, filter, fr);
    }

    private static void bind(ServerWebExchange exchange, String filter, FilterResult fr) {
        Map<String, FilterResult> fc = getFilterContext(exchange);
        fc.put(filter, fr);
        fc.put(PREV_FILTER_RESULT, fr);
    }

    public static FilterResult getPrevFilterResult(ServerWebExchange exchange) {
        return getFilterContext(exchange).get(PREV_FILTER_RESULT);
    }

    public static String getClientReqPath(ServerWebExchange exchange) {
        String p = exchange.getAttribute(clientRequestPath);
        if (p == null) {
            p = exchange.getRequest().getPath().value();
            int secFS = p.indexOf(Constants.Symbol.FORWARD_SLASH, 1);
            if (StringUtils.isBlank(gatewayPrefix) || Constants.Symbol.FORWARD_SLASH_STR.equals(gatewayPrefix)) {
                p = p.substring(secFS);
            } else {
                String prefix = p.substring(0, secFS);
                if (gatewayPrefix.equals(prefix) || SystemConfig.DEFAULT_GATEWAY_TEST_PREFIX.equals(prefix)) {
                    int trdFS = p.indexOf(Constants.Symbol.FORWARD_SLASH, secFS + 1);
                    p = p.substring(trdFS);
                } else {
                    throw Utils.runtimeExceptionWithoutStack("wrong prefix " + prefix);
                }
            }
            exchange.getAttributes().put(clientRequestPath, p);
        }
        return p;
    }

    public static void setBackendPath(ServerWebExchange exchange, String path) {
        exchange.getAttributes().put(BACKEND_PATH, path);
    }

    public static String getBackendPath(ServerWebExchange exchange) {
        return exchange.getAttribute(BACKEND_PATH);
    }

    public static String getClientReqPathPrefix(ServerWebExchange exchange) {
        String prefix = exchange.getAttribute(clientRequestPathPrefix);
        if (prefix == null) {
            if (StringUtils.isBlank(gatewayPrefix) || Constants.Symbol.FORWARD_SLASH_STR.equals(gatewayPrefix)) {
                prefix = Constants.Symbol.FORWARD_SLASH_STR;
            } else {
                String path = exchange.getRequest().getPath().value();
                int secFS = path.indexOf(Constants.Symbol.FORWARD_SLASH, 1);
                prefix = path.substring(0, secFS);
                if (gatewayPrefix.equals(prefix) || SystemConfig.DEFAULT_GATEWAY_TEST_PREFIX.equals(prefix)) {
                    prefix = prefix + Constants.Symbol.FORWARD_SLASH;
                } else {
                    throw Utils.runtimeExceptionWithoutStack("wrong prefix " + prefix);
                }
            }
            exchange.getAttributes().put(clientRequestPathPrefix, prefix);
        }
        return prefix;
    }

    public static String getClientReqQuery(ServerWebExchange exchange) {
        String qry = exchange.getAttribute(clientRequestQuery);
        if (qry != null && StringUtils.EMPTY.equals(qry)) {
            return null;
        } else {
            if (qry == null) {
                URI uri = exchange.getRequest().getURI();
                qry = uri.getQuery();
                if (qry == null) {
                    exchange.getAttributes().put(clientRequestQuery, StringUtils.EMPTY);
                } else {
                    if (StringUtils.indexOfAny(qry, Constants.Symbol.LEFT_BRACE, Constants.Symbol.FORWARD_SLASH, Constants.Symbol.HASH) > 0) {
                        qry = uri.getRawQuery();
                    }
                    exchange.getAttributes().put(clientRequestQuery, qry);
                }
            }
            return qry;
        }
    }

    public static String getClientReqPathQuery(ServerWebExchange exchange) {
        String relativeUri = getClientReqPath(exchange);
        String qry = getClientReqQuery(exchange);
        if (qry != null) {
            relativeUri = relativeUri + Constants.Symbol.QUESTION + qry;
        }
        return relativeUri;
    }

    public static String appendQuery(String path, ServerWebExchange exchange) {
        String qry = getClientReqQuery(exchange);
        if (qry != null) {
            return path + Constants.Symbol.QUESTION + qry;
        }
        return path;
    }

    public static Map<String, String> appendHeader(ServerWebExchange exchange, String name, String value) {
        Map<String, String> hdrs = getAppendHeaders(exchange);
        hdrs.put(name, value);
        return hdrs;
    }

    public static Map<String, String> getAppendHeaders(ServerWebExchange exchange) {
        return (Map<String, String>) exchange.getAttribute(APPEND_HEADERS);
    }

    public static HttpHeaders mergeAppendHeaders(ServerWebExchange exchange) {
        ServerHttpRequest req = exchange.getRequest();
        Map<String, String> appendHeaders = getAppendHeaders(exchange);
        if (appendHeaders.isEmpty()) {
            return req.getHeaders();
        }
        HttpHeaders hdrs = new HttpHeaders();
        req.getHeaders().forEach(
                (h, vs) -> {
                    hdrs.addAll(h, vs);
                }
        );
        appendHeaders.forEach(
                (h, v) -> {
                    List<String> vs = hdrs.get(h);
                    if (vs != null && !vs.isEmpty()) {
                        vs.clear();
                        vs.add(v);
                    } else {
                        hdrs.add(h, v);
                    }
                }
        );
        return hdrs;
    }

    public static void request2stringBuilder(ServerWebExchange exchange, StringBuilder b) {
        ServerHttpRequest req = exchange.getRequest();
        request2stringBuilder(req.getId(), req.getMethod(), req.getURI().toString(), req.getHeaders(), null, b);
    }

    public static void request2stringBuilder(String reqId, HttpMethod method, String uri, HttpHeaders headers, Object body, StringBuilder b) {
        b.append(reqId).append(Constants.Symbol.SPACE).append(method).append(Constants.Symbol.SPACE).append(uri);
        if (headers != null) {
            final boolean[] f = {false};
            LOG_HEADER_SET.forEach(
                    h -> {
                        String v = headers.getFirst(h);
                        if (v != null) {
                            if (!f[0]) {
                                b.append(Constants.Symbol.LINE_SEPARATOR);
                                f[0] = true;
                            }
                            Utils.addTo(b, h, Constants.Symbol.EQUAL, v, Constants.Symbol.TWO_SPACE_STR);
                        }
                    }
            );
        }
        // body to b
    }

    public static void response2stringBuilder(String rid, ClientResponse clientResponse, StringBuilder b) {
        b.append(rid).append(response).append(clientResponse.statusCode());
        HttpHeaders headers = clientResponse.headers().asHttpHeaders();
        final boolean[] f = {false};
        LOG_HEADER_SET.forEach(
                h -> {
                    String v = headers.getFirst(h);
                    if (v != null) {
                        if (!f[0]) {
                            b.append(Constants.Symbol.LINE_SEPARATOR);
                            f[0] = true;
                        }
                        Utils.addTo(b, h, Constants.Symbol.EQUAL, v, Constants.Symbol.TWO_SPACE_STR);
                    }
                }
        );
        // body to b
    }

    private static Mono<Void> responseError(ServerWebExchange exchange, String filter, int code, String msg, Throwable t, boolean bindContext) {
        // Mono<DataBuffer> reqBodyMono = getRequestBody(exchange);
        // final DataBuffer[] reqBody = {null};
        // if (reqBodyMono != null) {
        //     reqBodyMono.subscribe(
        //             db -> {
        //                 reqBody[0] = db;
        //                 DataBufferUtils.retain(reqBody[0]);
        //             }
        //     );
        // }
        String rid = exchange.getRequest().getId();
        // Schedulers.parallel().schedule(() -> {
        StringBuilder b = ThreadContext.getStringBuilder();
        request2stringBuilder(exchange, b);
        // if (reqBody[0] != null) {
        //     DataBufferUtils.release(reqBody[0]);
        // }
        b.append(Constants.Symbol.LINE_SEPARATOR);
        b.append(filter).append(Constants.Symbol.SPACE).append(code).append(Constants.Symbol.SPACE).append(msg);
        if (t == null) {
            log.error(b.toString(), LogService.BIZ_ID, rid);
        } else {
            log.error(b.toString(), LogService.BIZ_ID, rid, t);
            Throwable[] suppressed = t.getSuppressed();
            if (suppressed != null && suppressed.length != 0) {
                log.error(StringUtils.EMPTY, suppressed[0]);
            }
        }
        // });
        if (filter != null) {
            if (t == null) {
                transmitFailFilterResult(exchange, filter);
            } else {
                transmitFailFilterResult(exchange, filter, t);
            }
        }
        if (bindContext) {
            return buildJsonDirectResponseAndBindContext(exchange, HttpStatus.OK, null, RespEntity.toJson(code, msg, rid));
        } else {
            return buildJsonDirectResponse(exchange, HttpStatus.OK, null, RespEntity.toJson(code, msg, rid));
        }
    }

    public static Mono<Void> responseErrorAndBindContext(ServerWebExchange exchange, String filter, int code, String msg) {
        return responseError(exchange, filter, code, msg, null, true);
    }

    public static Mono<Void> responseErrorAndBindContext(ServerWebExchange exchange, String filter, int code, String msg, Throwable t) {
        return responseError(exchange, filter, code, msg, t, true);
    }

    public static Mono<Void> responseError(ServerWebExchange exchange, int code, String msg) {
        return responseError(exchange, null, code, msg, null, false);
    }

    public static Mono<Void> responseError(ServerWebExchange exchange, String reporter, int code, String msg, Throwable t) {
        return responseError(exchange, reporter, code, msg, t, false);
    }

    public static Mono<Void> responseErrorAndBindContext(ServerWebExchange exchange, String filter, HttpStatus httpStatus) {
        ServerHttpResponse response = exchange.getResponse();
        String rid = exchange.getRequest().getId();
        StringBuilder b = ThreadContext.getStringBuilder();
        request2stringBuilder(exchange, b);
        b.append(Constants.Symbol.LINE_SEPARATOR);
        b.append(filter).append(Constants.Symbol.SPACE).append(httpStatus);
        log.error(b.toString(), LogService.BIZ_ID, rid);
        transmitFailFilterResult(exchange, filter);
        return buildDirectResponseAndBindContext(exchange, httpStatus, new HttpHeaders(), Constants.Symbol.EMPTY);
    }

    public static Mono<Void> responseErrorAndBindContext(ServerWebExchange exchange, String filter, HttpStatus httpStatus,
                                                         HttpHeaders headers, String content) {
        ServerHttpResponse response = exchange.getResponse();
        String rid = exchange.getRequest().getId();
        StringBuilder b = ThreadContext.getStringBuilder();
        request2stringBuilder(exchange, b);
        b.append(Constants.Symbol.LINE_SEPARATOR);
        b.append(filter).append(Constants.Symbol.SPACE).append(httpStatus);
        log.error(b.toString(), LogService.BIZ_ID, rid);
        transmitFailFilterResult(exchange, filter);
        headers = headers == null ? new HttpHeaders() : headers;
        content = StringUtils.isBlank(content) ? Constants.Symbol.EMPTY : content;
        return buildDirectResponseAndBindContext(exchange, httpStatus, headers, content);
    }

    public static String getOriginIp(ServerWebExchange exchange) {
        String ip = exchange.getAttribute(originIp);
        if (ip == null) {
            ServerHttpRequest req = exchange.getRequest();
            String v = req.getHeaders().getFirst(xForwardedFor);
            if (StringUtils.isBlank(v)) {
                ip = req.getRemoteAddress().getAddress().getHostAddress();
            } else {
                ip = StringUtils.split(v, Constants.Symbol.COMMA)[0].trim();
                if (ip.equalsIgnoreCase(unknown)) {
                    ip = req.getRemoteAddress().getAddress().getHostAddress();
                } else if (ip.equals(binaryAddress)) {
                    ip = loopBack;
                }
            }
            exchange.getAttributes().put(originIp, ip);
        }
        return ip;
    }

    public static String getTraceId(ServerWebExchange exchange) {
        String id = exchange.getAttribute(traceId);
        if (id == null) {
            ServerHttpRequest request = exchange.getRequest();
            String v = request.getHeaders().getFirst(CommonConstants.HEADER_TRACE_ID);
            if (StringUtils.isNotBlank(v)) {
                id = v;
            } else {
                id = CommonConstants.TRACE_ID_PREFIX + request.getId();
            }
        }
        return id;
    }
}
