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

package com.fizzgate.proxy.grpc;

import com.fizzgate.fizz.exception.FizzException;
import com.fizzgate.proxy.grpc.client.CallResults;
import com.fizzgate.proxy.grpc.client.GrpcProxyClient;
import com.fizzgate.proxy.grpc.client.core.GrpcMethodDefinition;
import com.fizzgate.proxy.grpc.client.utils.ChannelFactory;
import com.google.common.net.HostAndPort;
import com.google.common.util.concurrent.ListenableFuture;
import io.grpc.CallOptions;
import io.grpc.ManagedChannel;
import org.apache.dubbo.rpc.service.GenericException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import static com.fizzgate.proxy.grpc.client.utils.GrpcReflectionUtils.parseToMethodDefinition;
import static io.grpc.CallOptions.DEFAULT;
import static java.util.Collections.singletonList;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class GrpcGenericService {

	@Autowired
	private GrpcProxyClient grpcProxyClient;

	/**
	 * Generic invoke.
	 *
	 * @param payload                  the json string body
	 * @param grpcInterfaceDeclaration the interface declaration
	 * @return the mono object
	 * @throws FizzException the fizz runtime exception
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Mono<Object> send(final String payload, final GrpcInterfaceDeclaration grpcInterfaceDeclaration,
			HashMap<String, Object> attachments) {
		GrpcMethodDefinition methodDefinition = parseToMethodDefinition(
				grpcInterfaceDeclaration.getServiceName() + "." + grpcInterfaceDeclaration.getMethod());
		HostAndPort endPoint = HostAndPort.fromString(grpcInterfaceDeclaration.getEndpoint());
		if (endPoint == null) {
			throw new RuntimeException("can't find target endpoint");
		}
		Map<String, Object> metaHeaderMap = attachments;
		ManagedChannel channel = null;
		try {
			channel = ChannelFactory.create(endPoint, metaHeaderMap);
			CallOptions calloptions = DEFAULT.withDeadlineAfter(grpcInterfaceDeclaration.getTimeout(), TimeUnit.MILLISECONDS);
			
			CallResults callResults = new CallResults();
			ListenableFuture<Void> future = grpcProxyClient.invokeMethodAsync(methodDefinition, channel, calloptions,
					singletonList(payload), callResults);
			return Mono.fromFuture(new ListenableFutureAdapter(future).getCompletableFuture().thenApply(ret -> {
				return callResults.asJSON();
			})).onErrorMap(exception -> exception instanceof GenericException
					? new FizzException(((GenericException) exception).getExceptionMessage())
					: new FizzException((Throwable) exception));
		} finally {
			if (channel != null) {
				channel.shutdown();
			}
		}
	}
}
