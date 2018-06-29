/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/*
  NOTE: this a fork of Justin Ross' Quiver at:
  https://raw.githubusercontent.com/ssorj/quiver/master/java/quiver-jms-driver/src/main/java/net/ssorj/quiver/QuiverArrowJms.java
  <p>
  The code was modified to integrate more tightly with maestro.
 */

package org.maestro.worker.jms;

import org.maestro.common.jms.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;

/**
 * A basic JMS client
 */
class JMSClient implements Client {
    private static final Logger logger = LoggerFactory.getLogger(JMSClient.class);

    protected String url = null;
    protected Destination destination = null;
    protected Connection connection = null;
    protected JmsOptions opts;

    protected int number = -1;

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    @Override
    public void start() throws Exception {
        logger.debug("Starting the JMS client");

        Destination destination;
        Connection connection = null;
        try {
            opts = new JmsOptions(url);
            final JMSProtocol protocol = opts.getProtocol();
            logger.debug("JMS client is running test with the protocol {}", protocol.name());

            final ConnectionFactory factory = protocol.createConnectionFactory(opts.getConnectionUrl());
            logger.trace("Connection factory created");

            String destinationName = opts.getPath().substring(1);
            logger.debug("Requested destination name: {}", destinationName);

            final Integer configuredLimitDestinations = opts.getConfiguredLimitDestinations();

            if (configuredLimitDestinations != null) {
                if (number < 0) {
                    throw new IllegalArgumentException("JMSClient::number must be >= 0 when limitDestinations is configured");
                }
                final int limitDestinations = configuredLimitDestinations;
                if (limitDestinations <= 0) {
                    throw new IllegalArgumentException("limitDestinations must be > 0");
                }
                logger.debug("Client requested a client-specific limit to the number of destinations: {}", limitDestinations);
                final int destinationId = number % limitDestinations;
                destinationName = destinationName + '.' + destinationId;
                logger.info("Requested destination name after using client-specific limit to the number of destinations: {}",
                        destinationName);
            } else {
                //original behaviour maintained for backward compatibility
                logger.info("Requested destination name: {}", destinationName);
            }

            //doesn't need to use any enum yet
            final String type = opts.getType();

            switch (type) {
                case "queue":
                    logger.debug("Creating a queue-based destination");
                    destination = protocol.createQueue(destinationName);
                    break;
                case "topic":
                    logger.debug("Creating a topic-based destination");
                    destination = protocol.createTopic(destinationName);
                    break;
                default:
                    throw new UnsupportedOperationException("not supported destination type: " + type);
            }

            logger.debug("Creating the connection");
            connection = factory.createConnection();
            logger.debug("Connection created successfully");
        } catch (Throwable t) {
            logger.warn("Something wrong happened while initializing the JMS client: {}", t.getMessage(), t);

            JMSResourceUtil.capturingClose(connection);
            throw t;
        }
        this.destination = destination;
        this.connection = connection;
        this.connection.start();
    }

    @Override
    public void stop() {
        logger.debug("Stopping the JMS client");
        final Throwable t = JMSResourceUtil.capturingClose(connection);
        this.connection = null;
        if (t != null) {
            t.printStackTrace();
        }
    }

    @Override
    public void setUrl(String url) {
        this.url = url;
    }
}
