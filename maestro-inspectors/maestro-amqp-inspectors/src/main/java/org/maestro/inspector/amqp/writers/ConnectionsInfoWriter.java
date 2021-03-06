package org.maestro.inspector.amqp.writers;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.io.IOUtils;
import org.maestro.common.inspector.types.ConnectionsInfo;
import org.maestro.common.writers.InspectorDataWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * A router link information writer for AMQP Inspector.
 */
public class ConnectionsInfoWriter implements InspectorDataWriter<ConnectionsInfo>, AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(ConnectionsInfoWriter.class);
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private BufferedWriter writer;
    private CSVPrinter csvPrinter;

    public ConnectionsInfoWriter(final File logDir, final String name) throws IOException {
        File outputFile = new File(logDir, name + ".csv");

        writer = Files.newBufferedWriter(Paths.get(outputFile.getPath()), Charset.defaultCharset());
        csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT
                .withHeader("Timestamp", "Container", "Host", "Role", "Dir", "Opened", "Identity", "User",
                        "sasl", "Encrypted", "sslProto", "sslCipher",
                        "Tenant", "Authenticated", "Properties"));
    }

    /**
     * Close csv printer
     */
    @Override
    public void close() {
        IOUtils.closeQuietly(csvPrinter);
        IOUtils.closeQuietly(writer);
    }

    /**
     * Write single record line into csv file
     * @param now current time
     * @param object one line record
     */
    @SuppressWarnings("unchecked")
    private void write(final LocalDateTime now, final Object object) {
        if (object instanceof Map) {
            final Map<String, Object> ConnectionsInfo = (Map<String, Object>) object;

            logger.trace("Connections information: {}", ConnectionsInfo);

            try {
                String timestamp = now.format(formatter);

                csvPrinter.printRecord(timestamp,
                        ConnectionsInfo.get("container"), ConnectionsInfo.get("host"),
                        ConnectionsInfo.get("role"), ConnectionsInfo.get("dir"),
                        ConnectionsInfo.get("opened"), ConnectionsInfo.get("identity"),
                        ConnectionsInfo.get("user"), ConnectionsInfo.get("sasl"),
                        ConnectionsInfo.get("isEncrypted"), ConnectionsInfo.get("sslProto"),
                        ConnectionsInfo.get("sslCipher"), ConnectionsInfo.get("tenant"),
                        ConnectionsInfo.get("isAuthenticated"), ConnectionsInfo.get("properties"));


            } catch (IOException e) {
                logger.error("Unable to write record: {}", e.getMessage(), e);
            }
        }
        else {
            logger.warn("Invalid value type for connections");
        }
    }

    /**
     * Write collected data
     * @param now current time
     * @param data data for print
     */
    @SuppressWarnings("unchecked")
    @Override
    public void write(final LocalDateTime now, final ConnectionsInfo data) {
        logger.trace("Connections information: {}", data);

        List connectionProperties = data.getConnectionProperties();

        connectionProperties.forEach(map -> write(now, map));
    }
}
