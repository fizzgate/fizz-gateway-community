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

package com.fizzgate.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;

import com.fizzgate.stats.FlowStat;
import com.fizzgate.stats.ResourceTimeWindowStat;
import com.fizzgate.stats.TimeWindowStat;
import com.fizzgate.stats.ratelimit.ResourceRateLimitConfig;
import com.fizzgate.util.Consts;
import com.fizzgate.util.DateTimeUtils;
import com.fizzgate.util.JacksonUtils;
import com.fizzgate.util.ResourceIdUtils;

import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author hongqiaowei
 */

@RefreshScope
@RestController
@RequestMapping("/admin/flowStat")
public class FlowControlController {

    private static final Logger log = LoggerFactory.getLogger(FlowControlController.class);

    @Value("${flowControl:false}")
    private boolean flowControl;

    @Autowired(required = false)
    private FlowStat flowStat;

    @GetMapping("/globalConcurrentsRps")
    public Mono<String> globalConcurrentsRps(ServerWebExchange exchange, @RequestParam(value = "recent", required = false, defaultValue = "3") int recent) {

        long concurrents = 0;
        double rps = 0;
        Map<String, Object> result = new HashMap<>();
        result.put("concurrents", concurrents);
        result.put("rps", rps);

        if (flowControl) {
            try {
                long currentTimeSlot = flowStat.currentTimeSlotId();
                long startTimeSlot = currentTimeSlot - recent * 1000;
                TimeWindowStat timeWindowStat = null;
                List<ResourceTimeWindowStat> wins = flowStat.getResourceTimeWindowStats(ResourceIdUtils.NODE_RESOURCE, startTimeSlot, currentTimeSlot, recent);
                if (wins == null || wins.isEmpty()) {
                    result.put("rps", 0);
                } else {
                    concurrents = flowStat.getConcurrentRequests(ResourceIdUtils.NODE_RESOURCE);
                    result.put("concurrents", concurrents);
                    timeWindowStat = wins.get(0).getWindows().get(0);
                    BigDecimal winrps = timeWindowStat.getRps();
                    if (winrps == null) {
                        rps = 0;
                    } else {
                        rps = winrps.doubleValue();
                    }
                    result.put("rps", rps);
                }
                if (log.isDebugEnabled()) {
                    long compReqs = -1;
                    if (timeWindowStat != null) {
                        compReqs = timeWindowStat.getCompReqs();
                    }
                    log.debug(toDP19(startTimeSlot) + " - " + toDP19(currentTimeSlot) + " result: " + JacksonUtils.writeValueAsString(result) + ", complete reqs: " + compReqs);
                }

            } catch (Throwable t) {
                log.error("get current global concurrents and rps error", t);
            }
        }

        return Mono.just(JacksonUtils.writeValueAsString(result));
    }

    private String toDP19(long startTimeSlot) {
        return DateTimeUtils.convert(startTimeSlot, Consts.DP.DP19);
    }
}
