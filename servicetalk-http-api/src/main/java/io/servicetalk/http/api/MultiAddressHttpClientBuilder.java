/*
 * Copyright © 2018-2019, 2021-2022 Apple Inc. and the ServiceTalk project authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.servicetalk.http.api;

import io.servicetalk.buffer.api.BufferAllocator;
import io.servicetalk.client.api.ServiceDiscovererEvent;
import io.servicetalk.concurrent.api.Executor;
import io.servicetalk.transport.api.IoExecutor;

/**
 * A builder of {@link StreamingHttpClient} instances which have a capacity to call any server based on the parsed
 * absolute-form URL address information from each {@link StreamingHttpRequest}.
 * <p>
 * It also provides a good set of default settings and configurations, which could be used by most users as-is or
 * could be overridden to address specific use cases.
 *
 * @param <U> the type of address before resolution (unresolved address)
 * @param <R> the type of address after resolution (resolved address)
 * @see <a href="https://tools.ietf.org/html/rfc7230#section-5.3.2">absolute-form rfc7230#section-5.3.2</a>
 */
public interface MultiAddressHttpClientBuilder<U, R> extends HttpClientBuilder<U, R, ServiceDiscovererEvent<R>> {
    /**
     * Initializes the {@link SingleAddressHttpClientBuilder} for each new client.
     * @param <U> The unresolved address type.
     * @param <R> The resolved address type.
     */
    @FunctionalInterface
    interface SingleAddressInitializer<U, R> {
        /**
         * Configures the passed {@link SingleAddressHttpClientBuilder} for the given {@code scheme} and
         * {@code address}.
         * @param scheme The scheme parsed from the request URI.
         * @param address The unresolved address.
         * @param builder The builder to customize and build a {@link StreamingHttpClient}.
         */
        void initialize(String scheme, U address, SingleAddressHttpClientBuilder<U, R> builder);

        /**
         * Appends the passed {@link SingleAddressInitializer} to this {@link SingleAddressInitializer} such that this
         * {@link SingleAddressInitializer} is applied first and then the passed {@link SingleAddressInitializer}.
         *
         * @param toAppend {@link SingleAddressInitializer} to append
         * @return A composite {@link SingleAddressInitializer} after the append operation.
         */
        default SingleAddressInitializer<U, R> append(SingleAddressInitializer<U, R> toAppend) {
            return (scheme, address, builder) -> {
                initialize(scheme, address, builder);
                toAppend.initialize(scheme, address, builder);
            };
        }
    }

    @Override
    MultiAddressHttpClientBuilder<U, R> ioExecutor(IoExecutor ioExecutor);

    @Override
    MultiAddressHttpClientBuilder<U, R> executor(Executor executor);

    @Override
    MultiAddressHttpClientBuilder<U, R> executionStrategy(HttpExecutionStrategy strategy);

    @Override
    MultiAddressHttpClientBuilder<U, R> bufferAllocator(BufferAllocator allocator);

    /**
     * Sets the {@link HttpHeadersFactory} to be used for creating {@link HttpHeaders} for new requests.
     *
     * @param headersFactory {@link HttpHeadersFactory} to be used for creating {@link HttpHeaders} for new requests
     * @return {@code this}
     */
    default MultiAddressHttpClientBuilder<U, R> headersFactory(HttpHeadersFactory headersFactory) {
        // FIXME: 0.43 - remove default implementation
        throw new UnsupportedOperationException(
                "MultiAddressHttpClientBuilder#headersFactory(HttpHeadersFactory) is not supported by " + getClass());
    }

    /**
     * Set a function which can customize options for each {@link StreamingHttpClient} that is built.
     * @param initializer Initializes the {@link SingleAddressHttpClientBuilder} used to build new
     * {@link StreamingHttpClient}s.
     * @return {@code this}
     */
    MultiAddressHttpClientBuilder<U, R> initializer(SingleAddressInitializer<U, R> initializer);

    /**
     * Enables <a href="https://datatracker.ietf.org/doc/html/rfc7231#section-6.4">redirection</a>.
     *
     * @param config {@link RedirectConfig} to configure redirection behavior. It can be used to tune what requests
     * should follow redirects and which parts of the original request (headers/payload body/trailers) should be
     * redirected to non-relative locations.
     * @return {@code this}.
     * @see RedirectConfigBuilder
     */
    MultiAddressHttpClientBuilder<U, R> followRedirects(RedirectConfig config);
}
