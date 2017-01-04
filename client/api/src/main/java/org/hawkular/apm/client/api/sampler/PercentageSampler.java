/*
 * Copyright 2015-2017 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hawkular.apm.client.api.sampler;

import java.util.BitSet;
import java.util.Random;

import org.hawkular.apm.api.model.trace.Trace;

/**
 * This sampler reports given percentage of created trace instances.
 * Sample traces are chosen uniformly. It does not check any
 * attributes on given trace object, therefore sampling decision
 * is not idempotent.
 *
 * @author Pavol Loffay
 */
public class PercentageSampler implements Sampler {

    private BitSet bitSet;
    private int i;

    protected PercentageSampler(int percentage) {
        bitSet = randomBitSet(100, percentage, new Random());
    }

    /**
     * @param percentage How many percent of created traces should be recorded. 1 = 1%...
     */
    public static Sampler withPercentage(int percentage) {
        if (percentage <= 0) {
            return Sampler.NEVER_SAMPLE;
        } else if (percentage >= 100) {
            return Sampler.ALWAYS_SAMPLE;
        }

        return new PercentageSampler(percentage);
    }

    @Override
    public synchronized boolean isSampled(Trace trace) {
        boolean sample = bitSet.get(i++);
        i = i == 100 ? 0 : i;
        return sample;
    }

    /**
     * Algorithm from http://stackoverflow.com/a/12822025/4158442
     */
    private static BitSet randomBitSet(int size, int cardinality, Random random) {
        BitSet result = new BitSet(size);
        int[] chosen = new int[cardinality];

        for (int i = 0; i < cardinality; ++i) {
            chosen[i] = i;
            result.set(i);
        }
        for (int i = cardinality; i < size; ++i) {
            int j = random.nextInt(i+1);
            if (j < cardinality) {
                result.clear(chosen[j]);
                result.set(i);
                chosen[j] = i;
            }
        }
        return result;
    }
}
