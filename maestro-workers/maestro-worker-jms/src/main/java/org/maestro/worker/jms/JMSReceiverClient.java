/*
 *  Copyright 2017 Otavio R. Piske <angusyoung@gmail.com>
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.maestro.worker.jms;

import org.maestro.common.content.ContentStrategy;
import org.maestro.common.jms.ReceiverClient;

import javax.jms.*;
import java.lang.IllegalStateException;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

final class JMSReceiverClient extends JMSClient implements ReceiverClient {
    private static final long RECEIVE_TIMEOUT_MILLIS = 1000L;
    private static final int PAYLOAD_SIZE = Long.BYTES;
    private Session session;
    private MessageConsumer consumer;
    private ByteBuffer payloadBytes;

    @Override
    public void start() throws Exception {
        super.start();
        try {
            session = connection.createSession(false, opts.getSessionMode());
            consumer = session.createConsumer(destination);
            payloadBytes = ByteBuffer.allocate(PAYLOAD_SIZE).order(ContentStrategy.CONTENT_ENDIANNESS);
        } catch (Throwable t) {
            JMSResourceUtil.capturingClose(consumer);
            this.consumer = null;
            JMSResourceUtil.capturingClose(session);
            this.session = null;
            JMSResourceUtil.capturingClose(connection);
            this.connection = null;
        }
    }


    @Override
    public long receiveMessages(boolean acknowledge) throws Exception {
        final Message message = consumer.receive(RECEIVE_TIMEOUT_MILLIS);

        if (message == null) {
            return ReceiverClient.noMessagePayload();
        }else if (acknowledge) {
            message.acknowledge();
        }
        final BytesMessage bytesMessage = (BytesMessage) message;
        //just read the benchmark minimum payload
        final int readBytes = bytesMessage.readBytes(payloadBytes.array(), PAYLOAD_SIZE);
        if (readBytes == PAYLOAD_SIZE || readBytes == -1) {
            //can read the timestamp using the default endianness of the content strategy
            return payloadBytes.getLong(0);
        }
        throw new IllegalStateException("the received message hasn't any benchmark payload");
    }

    @Override
    public void stop() {
        JMSResourceUtil.capturingClose(consumer);
        this.consumer = null;
        JMSResourceUtil.capturingClose(session);
        this.session = null;
        super.stop();
    }
}
