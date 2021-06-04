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

package we.plugin.auth;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpMethod;
import we.plugin.PluginConfig;
import we.util.JacksonUtils;
import we.util.UrlTransformUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author hongqiaowei
 */

public class ApiConfig {

    public static interface Type {
        static final byte UNDEFINED         = 0;
        static final byte SERVICE_AGGREGATE = 1;
        static final byte SERVICE_DISCOVERY = 2;
        static final byte REVERSE_PROXY     = 3;
        static final byte CALLBACK          = 4;
        static final byte DUBBO             = 5;
    }

    public  static final int    DELETED    = 1;

    public  static final char   ALLOW      = 'a';

    public  static final char   FORBID     = 'f';

    public  static final String ALL_METHOD = "AM";

    private static final String match_all  = "/**";

    private static final int    ENABLE     = 1;

    private static final int    UNABLE     = 0;

    public  int                id;                            // tb_api_auth.id

    public  int                isDeleted        = 0;          // tb_api_auth.is_deleted

    public  Set<String>        gatewayGroups    = Stream.of(GatewayGroup.DEFAULT).collect(Collectors.toSet());

    public  String             service;

    public  String             backendService;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    public  HttpMethod         method;

    public  Object             fizzMethod       = ALL_METHOD;

    public  String             path             = match_all;

    public  boolean            exactMatch       = false;

    public  String             backendPath;

    @JsonProperty("proxyMode")
    public  byte               type             = Type.SERVICE_DISCOVERY;

    private int                counter          = 0;

    public  List<String>       httpHostPorts;

    public  char               access           = ALLOW;

    public  List<PluginConfig> pluginConfigs;

    public  boolean            checkApp         = false;

    public  CallbackConfig     callbackConfig;

    public  String             rpcMethod;

    public  String             rpcParamTypes;

    public  String             rpcVersion;

    public  String             rpcGroup;

    public  long               timeout          = 0;

    public static boolean isAntPathPattern(String path) {
        boolean uriVar = false;
        for (int i = 0; i < path.length(); i++) {
            char c = path.charAt(i);
            if (c == '*' || c == '?') {
                return true;
            }
            if (c == '{') {
                uriVar = true;
                continue;
            }
            if (c == '}' && uriVar) {
                return true;
            }
        }
        return false;
    }

    public void setGatewayGroup(String ggs) {
        gatewayGroups.remove(GatewayGroup.DEFAULT);
        if (StringUtils.isBlank(ggs)) {
            gatewayGroups.add("*");
        } else {
            Arrays.stream(StringUtils.split(ggs, ',')).forEach(
                    gg -> {
                        gatewayGroups.add(gg.trim());
                    }
            );
        }
    }

    public void setPath(String p) {
        if (StringUtils.isNotBlank(p)) {
            if ("/".equals(p)) {
                path = match_all;
            } else {
                path = p.trim();
                if (!isAntPathPattern(path)) {
                    exactMatch = true;
                }
            }
        } else {
            path = match_all;
        }
    }

    public void setMethod(String m) {
        method = HttpMethod.resolve(m);
        if (method == null) {
            fizzMethod = ALL_METHOD;
        } else {
            fizzMethod = method;
        }
    }

    public void setAppEnable(int v) {
        if (v == ENABLE) {
            checkApp = true;
        } else {
            checkApp = false;
        }
    }

    @JsonIgnore
    public String getNextHttpHostPort() {
        int i = counter++;
        if (i < 0) {
            i = Math.abs(i);
        }
        return httpHostPorts.get(
                   i % httpHostPorts.size()
               );
    }

    public String transform(String reqPath) {
        if (exactMatch) {
            return backendPath;
        }
        return UrlTransformUtils.transform(path, backendPath, reqPath);
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ApiConfig) {
            ApiConfig that = (ApiConfig) obj;
            return this.id == that.id;
        }
        return false;
    }

    @Override
    public String toString() {
        return JacksonUtils.writeValueAsString(this);
    }
}
