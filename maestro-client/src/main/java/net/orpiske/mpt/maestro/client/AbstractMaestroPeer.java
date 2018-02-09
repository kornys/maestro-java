package net.orpiske.mpt.maestro.client;

import net.orpiske.mpt.common.URLUtils;
import net.orpiske.mpt.common.exceptions.MaestroConnectionException;
import net.orpiske.mpt.maestro.exceptions.MalformedNoteException;
import net.orpiske.mpt.maestro.notes.MaestroNote;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;

// TODO: configure LWT
public abstract class AbstractMaestroPeer<T extends MaestroNote> implements MqttCallback {
    private static final Logger logger = LoggerFactory.getLogger(AbstractMaestroPeer.class);

    private MqttClient inboundEndPoint;
    protected String clientName;
    protected String id;
    private final MaestroNoteDeserializer<? extends T> deserializer;

    public AbstractMaestroPeer(final String url, final String clientName, MaestroNoteDeserializer<? extends T> deserializer) throws MaestroConnectionException {

        String adjustedUrl = URLUtils.sanizeURL(url);

        UUID uuid = UUID.randomUUID();
        String clientId = uuid.toString();
        MemoryPersistence memoryPersistence = new MemoryPersistence();

        this.id = clientId;
        this.clientName = clientName;

        try {
            inboundEndPoint = new MqttClient(adjustedUrl, clientName + ".inbound." + clientId, memoryPersistence);
            inboundEndPoint.setCallback(this);
        }
        catch (MqttException e) {
            throw new MaestroConnectionException("Unable create a MQTT client instance : " + e.getMessage(),
                    e);
        }

        this.deserializer = deserializer;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void connectionLost(Throwable throwable) {
        logger.warn("Connection lost");
    }

    public boolean isConnected() {
        return inboundEndPoint.isConnected();
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

    }

    public void connect() throws MaestroConnectionException {
        logger.debug("Connecting to maestro");
        MqttConnectOptions connOpts = new MqttConnectOptions();

        connOpts.setCleanSession(true);
        connOpts.setKeepAliveInterval(15000);

        try {
            inboundEndPoint.connect();
        }
        catch (MqttException e) {
            throw new MaestroConnectionException("Unable to establish a connection to Maestro: " + e.getMessage(), e);
        }
    }

    public void disconnect() throws MaestroConnectionException {
        logger.debug("Disconnecting from maestro");

        try {
            inboundEndPoint.disconnect();
        }
        catch (MqttException e) {
            throw new MaestroConnectionException("Unable to disconnect cleanly from Maestro: " + e.getMessage(), e);
        }
    }

    public void subscribe(final String[] topics) throws MaestroConnectionException {
        logger.debug("Subscribing to maestro topics {}", Arrays.toString(topics));

        int qos[] = new int[topics.length];

        for (int i = 0; i < topics.length; i++) {
            qos[i] = 0;
        }

        try {
            inboundEndPoint.subscribe(topics, qos);
        }
        catch (MqttException e) {
            throw new MaestroConnectionException("Unable to subscribe to Maestro topics: " + e.getMessage(), e);
        }
    }

    public void messageArrived(String s, MqttMessage mqttMessage) {
        logger.trace("Message arrived on topic {}", s);

        byte[] payload = mqttMessage.getPayload();

        try {
            final T note = deserializer.deserialize(payload);
            logger.trace("Message type: " + note.getClass());

            noteArrived(note);
        } catch (MalformedNoteException e) {
            logger.error("Invalid message type: " + e.getMessage(), e);
        } catch (IOException e) {
            logger.error("I/O error: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unhandled exception: " + e.getMessage(), e);
        }
    }


    /**
     * The entry point for handling Maestro messages
     * @param note the note that arrived
     *
     * @throws IOException for I/O related errors
     * @throws MaestroConnectionException for Maestro related errors
     */
    protected abstract void noteArrived(T note) throws MaestroConnectionException;

    public abstract boolean isRunning();


}