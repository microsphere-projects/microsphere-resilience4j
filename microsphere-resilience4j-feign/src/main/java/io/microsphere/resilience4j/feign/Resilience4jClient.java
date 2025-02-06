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
import feign.Request;
import feign.Response;
import io.microsphere.resilience4j.common.Resilience4jFacade;

import java.io.IOException;

import static io.microsphere.util.ExceptionUtils.wrap;

/**
 * {@link Client} for Resilience4j
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 * @see Client
 * @since 1.0.0
 */
public class Resilience4jClient implements Client {

    private final Client delegate;

    private final Resilience4jFacade facade;

    private final String entryNamePrefix;

    public Resilience4jClient(Client delegate, Resilience4jFacade facade, String entryNamePrefix) {
        this.delegate = delegate;
        this.facade = facade;
        this.entryNamePrefix = entryNamePrefix;
    }

    @Override
    public Response execute(Request request, Request.Options options) throws IOException {
        String entryName = buildEntryName(request);
        try {
            return this.facade.call(entryName, () -> this.delegate.execute(request, options));
        } catch (Throwable e) {
            throw wrap(e, IOException.class);
        }
    }

    private String buildEntryName(Request request) {
        StringBuilder entryNameBuilder = new StringBuilder(entryNamePrefix);
        Request.HttpMethod httpMethod = request.httpMethod();
        return entryNameBuilder.toString();
    }

}
