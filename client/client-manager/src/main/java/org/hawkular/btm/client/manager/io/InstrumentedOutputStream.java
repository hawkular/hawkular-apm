/*
 * Copyright 2015 Red Hat, Inc. and/or its affiliates
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
package org.hawkular.btm.client.manager.io;

import java.io.IOException;
import java.io.OutputStream;

import org.hawkular.btm.api.model.admin.Direction;
import org.hawkular.btm.client.api.BusinessTransactionCollector;

/**
 * @author gbrown
 */
public class InstrumentedOutputStream extends OutputStream {

    private BusinessTransactionCollector collector;
    private Direction direction;
    private OutputStream os;
    private String initiateLinkId;

    public InstrumentedOutputStream(BusinessTransactionCollector collector, Direction direction,
                        OutputStream os, String initiateLinkId) {
        this.collector = collector;
        this.direction = direction;
        this.os = os;
        this.initiateLinkId = initiateLinkId;

        if (direction == Direction.In) {
            collector.initInBuffer(null, this);
        } else {
            collector.initOutBuffer(null, this);
        }
    }

    @Override
    public void write(byte[] b) throws IOException {
        if (direction == Direction.In) {
            collector.appendInBuffer(null, this, b, 0, b.length);
        } else {
            collector.appendOutBuffer(null, this, b, 0, b.length);
        }
        os.write(b);
    }

    @Override
    public void write(byte[] b, int offset, int len) throws IOException {
        if (direction == Direction.In) {
            collector.appendInBuffer(null, this, b, offset, len);
        } else {
            collector.appendOutBuffer(null, this, b, offset, len);
        }
        os.write(b, offset, len);
    }

    /* (non-Javadoc)
     * @see java.io.OutputStream#write(int)
     */
    @Override
    public void write(int arg0) throws IOException {
        os.write(arg0);
    }

    /* (non-Javadoc)
     * @see java.io.OutputStream#flush()
     */
    @Override
    public void flush() throws IOException {
        os.flush();
    }

    /* (non-Javadoc)
     * @see java.io.OutputStream#close()
     */
    @Override
    public void close() throws IOException {
        if (direction == Direction.In) {
            collector.recordInBuffer(null, this);
        } else {
            collector.recordOutBuffer(null, this);
        }
        if (initiateLinkId != null) {
            collector.session().initiateCorrelation(initiateLinkId);
            collector.session().unlink();
        }
        os.close();
    }

    public String toString() {
        return "InstrumentedOutputStream[" + os + "]";
    }
}
