/*
 * Copyright © 2018 Apple Inc. and the ServiceTalk project authors
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
package io.servicetalk.examples.http.service.composition.backends;

import io.servicetalk.concurrent.api.Completable;
import io.servicetalk.concurrent.api.CompositeCloseable;
import io.servicetalk.transport.api.IoExecutor;
import io.servicetalk.transport.api.ServerContext;

import com.fasterxml.jackson.databind.ObjectMapper;

import static io.servicetalk.buffer.netty.BufferAllocators.DEFAULT_ALLOCATOR;
import static io.servicetalk.concurrent.api.AsyncCloseables.newCompositeCloseable;
import static io.servicetalk.concurrent.api.Completable.completed;
import static io.servicetalk.concurrent.internal.Await.awaitIndefinitely;
import static io.servicetalk.examples.http.service.composition.backends.MetadataBackend.newMetadataService;
import static io.servicetalk.examples.http.service.composition.backends.PortRegistry.METADATA_BACKEND_ADDRESS;
import static io.servicetalk.examples.http.service.composition.backends.PortRegistry.RATINGS_BACKEND_ADDRESS;
import static io.servicetalk.examples.http.service.composition.backends.PortRegistry.RECOMMENDATIONS_BACKEND_ADDRESS;
import static io.servicetalk.examples.http.service.composition.backends.PortRegistry.USER_BACKEND_ADDRESS;
import static io.servicetalk.examples.http.service.composition.backends.RatingBackend.newRatingService;
import static io.servicetalk.examples.http.service.composition.backends.RecommendationBackend.newRecommendationsService;
import static io.servicetalk.examples.http.service.composition.backends.UserBackend.newUserService;
import static io.servicetalk.transport.netty.NettyIoExecutors.createIoExecutor;

/**
 * A server starter for all backends in this example.
 */
public final class BackendsStarter {

    private BackendsStarter() {
        // No instances.
    }

    public static void main(String[] args) throws Exception {
        // Create an AutoCloseable representing all resources used in this example.
        try (CompositeCloseable resources = newCompositeCloseable()) {
            // Used for serialization/deserialization in all backends.

            final ObjectMapper mapper = new ObjectMapper();
            // Shared IoExecutor for the application.
            IoExecutor ioExecutor = createIoExecutor();
            // Add it as a resource to be cleaned up at the end.
            resources.concat(ioExecutor);

            // This is a single Completable used to await closing of all backends started by this class. It is used to
            // provide a way to not let main() exit.
            Completable allServicesOnClose = completed();

            BackendStarter starter = new BackendStarter(ioExecutor);
            final ServerContext recommendationService =
                    starter.start(RECOMMENDATIONS_BACKEND_ADDRESS.getPort(), "recommendation-service",
                            newRecommendationsService(mapper));
            allServicesOnClose = allServicesOnClose.merge(recommendationService.onClose());

            final ServerContext metadataService =
                    starter.start(METADATA_BACKEND_ADDRESS.getPort(), "metadata-service",
                            newMetadataService(mapper));
            allServicesOnClose = allServicesOnClose.merge(metadataService.onClose());

            final ServerContext userService =
                    starter.start(USER_BACKEND_ADDRESS.getPort(), "user-service",
                            newUserService(mapper, DEFAULT_ALLOCATOR));
            allServicesOnClose = allServicesOnClose.merge(userService.onClose());

            final ServerContext ratingService =
                    starter.start(RATINGS_BACKEND_ADDRESS.getPort(), "rating-service",
                            newRatingService(mapper));
            allServicesOnClose = allServicesOnClose.merge(ratingService.onClose());

            // Await termination of all backends started by this class.
            awaitIndefinitely(allServicesOnClose);
        }
    }
}
