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

package we.proxy;

import org.springframework.http.HttpMethod;
import we.plugin.PluginConfig;
import we.util.Consts;
import we.util.JacksonUtils;

import java.util.List;

/**
 * @author hongqiaowei
 */

public class Route {

    public boolean            dedicatedLine       = false;

    public byte               type;

    public HttpMethod         method;

    public String             backendService;

    public String             backendPath;

    @Deprecated
    public String             query;

    public String             nextHttpHostPort;

    /**
     * use pluginConfigs(List<PluginConfig> pcs) method to update this value.
     */
    public List<PluginConfig> pluginConfigs;

    public boolean            pluginConfigsChange = false;

    public String             rpcMethod;

    public String             rpcParamTypes;

    public String             rpcVersion;

    public String             rpcGroup;

    public long               timeout             = 0;

    public int                retryCount          = 0;

    public long               retryInterval       = 0;

    public Route dedicatedLine(boolean b) {
        dedicatedLine = b;
        return this;
    }

    public Route type(int t) {
        type = (byte) t;
        return this;
    }

    public Route method(HttpMethod m) {
        method = m;
        return this;
    }

    public Route backendService(String bs) {
        backendService = bs;
        return this;
    }

    public Route backendPath(String bp) {
        backendPath = bp;
        return this;
    }

    @Deprecated
    public Route query(String qry) {
        query = qry;
        return this;
    }

    public Route pluginConfigs(List<PluginConfig> pcs) {
              pluginConfigs = pcs;
        pluginConfigsChange = true;
        return this;
    }

    public Route nextHttpHostPort(String nhhp) {
        nextHttpHostPort = nhhp;
        return this;
    }

    public Route rpcMethod(String m) {
        rpcMethod = m;
        return this;
    }

    public Route rpcParamTypes(String t) {
        rpcParamTypes = t;
        return this;
    }

    public Route rpcVersion(String v) {
        rpcVersion = v;
        return this;
    }

    public Route rpcGroup(String g) {
        rpcGroup = g;
        return this;
    }

    public Route timeout(long t) {
        timeout = t;
        return this;
    }

    public Route retryCount(int rc) {
        retryCount = rc;
        return this;
    }

    public Route retryInterval(long ri) {
        retryInterval = ri;
        return this;
    }

    @Deprecated
    public String getBackendPathQuery() {
        if (query != null) {
            return backendPath + Consts.S.QUESTION + query;
        }
        return backendPath;
    }

    @Override
    public String toString() {
        return JacksonUtils.writeValueAsString(this);
    }
}
