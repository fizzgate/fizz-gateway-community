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

package com.fizzgate.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import com.fizzgate.config.WebClientConfig;

/**
 * @author hongqiaowei
 */

@Configuration
@ConfigurationProperties(prefix = ProxyWebClientConfig.prefix)
public class ProxyWebClientConfig extends WebClientConfig {

    protected static final String prefix         = "proxy-webclient";

    public    static final String proxyWebClient = "proxyWebClient";

    @Bean(proxyWebClient)
    public WebClient webClient() {
        log.info("proxy web client: {}", this);
        return super.webClient();
    }
}
