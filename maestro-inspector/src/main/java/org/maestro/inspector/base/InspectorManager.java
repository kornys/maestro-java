/*
 * Copyright 2018 Otavio R. Piske <angusyoung@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.maestro.inspector.base;

import org.maestro.client.notes.MaestroInspectorEventListener;
import org.maestro.client.notes.SetRequest;
import org.maestro.client.notes.StartInspector;
import org.maestro.client.notes.StopInspector;
import org.maestro.common.exceptions.MaestroException;
import org.maestro.common.inspector.MaestroInspector;
import org.maestro.worker.common.ds.MaestroDataServer;
import org.maestro.worker.common.MaestroWorkerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

public class InspectorManager extends MaestroWorkerManager implements MaestroInspectorEventListener {
    private static final Logger logger = LoggerFactory.getLogger(InspectorManager.class);
    private static final String INSPECTOR_ROLE = "inspector";
    private Thread inspectorThread;
    private InspectorContainer inspectorContainer;
    private MaestroInspector inspector;
    private File logDir;
    private HashMap<String, String> inspectorMap = new HashMap<>();

    public InspectorManager(final String maestroURL, final String host, final MaestroDataServer dataServer, final File logDir) throws MaestroException
    {
        super(maestroURL, INSPECTOR_ROLE, host, dataServer);

        this.logDir = logDir;

        inspectorMap.put("InterconnectInspector", "org.maestro.inspector.amqp.InterconnectInspector");
        inspectorMap.put("ArtemisInspector", "org.maestro.inspector.activemq.ArtemisInspector");
    }

    private void createInspector(final String inspectorType) throws IllegalAccessException, InstantiationException, ClassNotFoundException {
        @SuppressWarnings("unchecked")
        Class<MaestroInspector> clazz = (Class<MaestroInspector>) Class.forName(inspectorType);

        inspector = clazz.newInstance();
        inspector.setEndpoint(getClient());
        inspector.setBaseLogDir(logDir);
    }

    @Override
    public void handle(StartInspector note) {
        logger.debug("Start inspector request received");

        try {
            createInspector(inspectorMap.get(note.getPayload()));

            inspector.setDuration(getWorkerOptions().getDuration());

            inspectorContainer = new InspectorContainer(inspector);

            inspectorThread = new Thread(inspectorContainer);
            inspectorThread.start();

            getClient().replyOk();
        }
        catch (Throwable t) {
            logger.error("Unable to start inspector: {}", t.getMessage(), t);
            getClient().replyInternalError();
        }
    }


    @Override
    public void handle(SetRequest note) {
        super.handle(note);

        if (note.getOption() == SetRequest.Option.MAESTRO_NOTE_OPT_SET_MI) {
            String value = note.getValue();
            try {
                URL url = new URL(value);

                /*
                 * Jolokia client does not handle well the userinfo part of the URL ... that's
                 * why it reformats the URL without it.
                 */
                String newUrl = url.getProtocol() + "://" + url.getHost() + ":" + url.getPort() + "/" + url.getPath();
                inspector.setUrl(newUrl);

                String userInfo = url.getUserInfo();
                if (userInfo != null) {
                    String[] parts = userInfo.split(":");
                    String username = parts[0];
                    inspector.setUser(username);

                    if (parts.length == 2) {
                        inspector.setPassword(parts[1]);
                    }
                }
            } catch (MalformedURLException e) {
                logger.error("Unable to parse management interface URL {}: {}", value, e.getMessage(), e);
                getClient().replyInternalError();
            }
        }
    }

    @Override
    public void handle(StopInspector note) {
        logger.debug("Stop inspector request received");

        if (inspectorThread != null) {
            inspectorThread.interrupt();
        }
    }
}

