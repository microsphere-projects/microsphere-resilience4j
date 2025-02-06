/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.microsphere.resilience4j.feign;


import feign.Client;
import feign.MethodMetadata;
import feign.Request;
import feign.RequestTemplate;
import feign.Response;
import feign.codec.Encoder;

import java.io.IOException;
import java.lang.reflect.Method;

import static io.microsphere.reflect.MethodUtils.invokeMethod;
import static io.microsphere.resilience4j.feign.DelegatingInvocationHandler.getArguments;

/**
 * Delegating {@link Client}
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 * @see Client
 * @since 1.0.0
 */
public class DelegatingClient<T> implements Client {

    private final Encoder encoder;

    private final T delegate;

    public DelegatingClient(Encoder encoder, T delegate) {
        this.encoder = encoder;
        this.delegate = delegate;
    }

    @Override
    public Response execute(Request request, Request.Options options) throws IOException {
        RequestTemplate requestTemplate = request.requestTemplate();
        MethodMetadata methodMetadata = requestTemplate.methodMetadata();
        Method method = methodMetadata.method();
        Object[] args = getArguments();
        Object result = invokeMethod(delegate, method, args);
        encoder.encode(result, method.getReturnType(), requestTemplate);
        return Response.builder()
                .request(request)
                .status(200)
                .body(requestTemplate.body())
                .build();
    }
}
