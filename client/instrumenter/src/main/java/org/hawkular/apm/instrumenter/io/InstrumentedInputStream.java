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
package org.hawkular.apm.instrumenter.io;

import java.io.IOException;
import java.io.InputStream;

import org.hawkular.apm.api.model.config.Direction;
import org.hawkular.apm.client.collector.TraceCollector;

/**
 * This class provides an instrumented proxy for an input stream.
 *
 * @author gbrown
 */
public class InstrumentedInputStream extends InputStream {

    private TraceCollector collector;
    private Direction direction;
    private InputStream is;

    /**
     * The constructor for the instrumented input stream.
     *
     * @param collector The collector
     * @param direction The direction
     * @param is The original input stream
     */
    public InstrumentedInputStream(TraceCollector collector, Direction direction, InputStream is) {
        this.collector = collector;
        this.direction = direction;
        this.is = is;

        if (direction == Direction.In) {
            collector.initInBuffer(null, this);
        } else {
            collector.initOutBuffer(null, this);
        }
    }

    @Override
    public int available() throws IOException {
        return is.available();
    }

    @Override
    public void mark(int readlimit) {
        is.mark(readlimit);
    }

    @Override
    public boolean markSupported() {
        return is.markSupported();
    }

    @Override
    public int read() throws IOException {
        return is.read();
    }

    @Override
    public int read(byte[] b) throws IOException {
        int len=is.read(b);
        if (direction == Direction.In) {
            collector.appendInBuffer(null, this, b, 0, len);
        } else {
            collector.appendOutBuffer(null, this, b, 0, len);
        }
        return len;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int actuallen=is.read(b, off, len);
        if (direction == Direction.In) {
            collector.appendInBuffer(null, this, b, off, actuallen);
        } else {
            collector.appendOutBuffer(null, this, b, off, actuallen);
        }
        return actuallen;
    }

    @Override
    public void reset() throws IOException {
        is.reset();
    }

    @Override
    public long skip(long n) throws IOException {
        return is.skip(n);
    }

    @Override
    public void close() throws IOException {
        if (direction == Direction.In) {
            collector.recordInBuffer(null, this);
        } else {
            collector.recordOutBuffer(null, this);
        }
        is.close();
    }

    public String toString() {
        return "InstrumentedInputStream[" + is + "]";
    }
}
