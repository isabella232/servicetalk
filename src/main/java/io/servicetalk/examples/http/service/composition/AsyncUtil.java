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
package io.servicetalk.examples.http.service.composition;

import io.servicetalk.concurrent.api.Executor;
import io.servicetalk.concurrent.api.Single;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

// This class exists to fill the gap for missing operators in our async primitives.
final class AsyncUtil {

    /**
     * This operator is not currently available in ServiceTalk hence we provide a workaround to achieve the same
     * results as a zip. This operator merges heterogenous {@link Single} into a single holder object.
     *
     * @param first First {@link Single} to be zipped.
     * @param second Second {@link Single} to be zipped.
     * @param third Third {@link Single} to be zipped.
     * @param zipper {@link Zipper} function to combine the results of three {@link Single}s.
     * @param <T1> Type of item emitted by the first {@link Single}.
     * @param <T2> Type of item emitted by the second {@link Single}.
     * @param <T3> Type of item emitted by the third {@link Single}.
     * @param <R> Type of the final holder object.
     * @return A {@link Single} that will emit the final holder object.
     * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX zip operator.</a>
     */
    static <T1, T2, T3, R> Single<R> zip(Single<T1> first, Single<T2> second, Single<T3> third,
                                         Zipper<T1, T2, T3, R> zipper) {

        @SuppressWarnings("unchecked")
        Single<R> resp = first.concatWith((Single) second).concatWith(third)
                .reduce(Collector::new, (collector, aEntity) -> {
                    @SuppressWarnings("unchecked")
                    Collector<T1, T2, T3, R> c = (Collector<T1, T2, T3, R>) collector;
                    return c.add(aEntity);
                })
                .map(collector -> {
                    @SuppressWarnings("unchecked")
                    Collector<T1, T2, T3, R> c = (Collector<T1, T2, T3, R>) collector;
                    return c.zip(zipper);
                });

        return resp;
    }

    /**
     * This operator is not currently available in ServiceTalk hence we provide a workaround to achieve the same
     * results as a timeout. This operator applies a timeout to a {@link Single} such that if no result is emitted from
     * this {@link Single} then it is cancelled and a {@link TimeoutException} is emitted from the
     * {@link Single}.
     *
     * @param original {@link Single} on which a timeout is to be applied.
     * @param <T> Type of item emitted by the original and returned {@link Single}.
     * @return A {@link Single} that will emit the result of the original {@link Single} or terminate with a
     * {@link TimeoutException} if result is not emitted in the specified time.
     * @see <a href="http://reactivex.io/documentation/operators/timeout.html">ReactiveX timeout operator.</a>
     */
    static <T> Single<T> timeout(Single<T> original, Executor executor, Duration timeout) {
        // We do not currently have a timeout operator on our asynchronous sources, so we have to do the below
        // workaround to achieve timeout.
        return executor.schedule(timeout.toNanos(), NANOSECONDS)
                .merge(original.toPublisher())
                .first();
    }

    @FunctionalInterface
    interface Zipper<T1, T2, T3, R> {

        R zip(T1 first, T2 second, T3 third);

    }

    private static final class Collector<T1, T2, T3, R> {

        private final Object[] holder = new Object[3];
        private int index = 0;

        Collector<T1, T2, T3, R> add(Object entity) {
            holder[index++] = entity;
            return this;
        }

        @SuppressWarnings("unchecked")
        R zip(Zipper<T1, T2, T3, R> zipper) {
            return zipper.zip((T1) holder[0], (T2) holder[1], (T3) holder[2]);
        }
    }
}
