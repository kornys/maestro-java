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
@GrabConfig(systemClassLoader=true)

@Grab(group='commons-cli', module='commons-cli', version='1.3.1')
@Grab(group='org.apache.commons', module='commons-lang3', version='3.6')

@Grab(group='org.msgpack', module='msgpack-core', version='0.8.3')

@GrabResolver(name='Eclipse', root='https://repo.eclipse.org/content/repositories/paho-releases/')
@Grab(group='org.eclipse.paho', module='org.eclipse.paho.client.mqttv3', version='1.1.1')

@GrabResolver(name='orpiske-bintray', root='https://dl.bintray.com/orpiske/libs-release')
@Grab(group='net.orpiske', module='maestro-client', version='1.2.0')
@Grab(group='net.orpiske', module='maestro-tests', version='1.2.0')

import net.orpiske.mpt.maestro.Maestro
import net.orpiske.mpt.maestro.client.MaestroNoteProcessor
import net.orpiske.mpt.maestro.notes.MaestroNote
import net.orpiske.mpt.maestro.notes.PingResponse
import net.orpiske.mpt.maestro.notes.TestFailedNotification
import net.orpiske.mpt.maestro.notes.TestSuccessfulNotification

import net.orpiske.mpt.test.TestExecutor

/**
 * This sample demonstrates how to create a more complex test execution with Maestro.
 * Note that, for most cases, the executors should extend the AbstractTestExecutor
 * and AbstractTestProcessor because they offer additional logic regarding handling the
 * test notifications/completeness and better multi-peer support.
 *
 * In this test, the following behavior is being defined:
 *
 * 1. A executor is defined for the test
 * 2. A processor to process the replies from the peers
 * 3. Basic test setup logic
 *
 * URLs for the Maestro Broker and the test broker are set via MAESTRO_BROKER and
 * BROKER_URL environment variables
 */

/**
 * A simple test executor class
 */
class ShortTestExecutorCount implements TestExecutor {
    private Maestro maestro
    private int rate = 300
    private int parallelCount = 1
    private final maximumLatency = 500
    private String brokerURL

    ShortTestExecutorCount(Maestro maestro) {
        this.maestro = maestro;
    }

    String getBrokerURL() {
        return brokerURL
    }

    void setBrokerURL(String brokerURL) {
        this.brokerURL = brokerURL
    }


    /**
     * These two methods are NO-OP in this case because there are no multiple iterations,
     * therefore cool down period is not required/used
     */
    long getCoolDownPeriod() {
        return 0;
    }

    void setCoolDownPeriod(long period) {
        // NO-OP
    }

    /**
     * The simple processor for Maestro responses
     */
    class ShortTestProcessor extends MaestroNoteProcessor {
        @Override
        protected void processPingResponse(PingResponse note) {
            println  "Elapsed time from " + note.getName() + ": " + note.getElapsed() + " ms"
        }

        @Override
        protected void processNotifySuccess(TestSuccessfulNotification note) {
            println "Test successful on " + note.getName()
        }

        @Override
        protected void processNotifyFail(TestFailedNotification note) {
            println "Test failed on " + note.getName()
        }


    }

    private boolean processReplies() {
        println "Collecting replies "
        List<MaestroNote> replies = maestro.collect(1000, 10)

        (new ShortTestProcessor()).process(replies)
    }

    private void setTestParameters(String brokerURL) {
        println "Sending ping request"
        maestro.pingRequest()

        println "Setting broker"
        maestro.setBroker(brokerURL)

        println "Setting rate"
        maestro.setRate(rate);

        println "Setting parallel count"
        maestro.setParallelCount(parallelCount)

        println "Setting duration"
        maestro.setDuration("30")

        println "Setting fail-condition-latency"
        maestro.setFCL(maximumLatency)

        // Variable message size
        maestro.setMessageSize("~100")
    }

    private void startServices() {
        maestro.startReceiver()
        maestro.startInspector()
        maestro.startSender()
    }

    /**
     * Test execution logic
     * @return
     */
    boolean run() {
        setTestParameters(brokerURL)
        startServices()
        processReplies()

        println "Waiting a while for the tests to kick off"
        Thread.sleep(21000)

        processReplies()
    }
}

/**
 * Test setup.
 */
maestroURL = System.getenv("MAESTRO_BROKER")
brokerURL = System.getenv("BROKER_URL")

println "Connecting to " + maestroURL
maestro = new Maestro(maestroURL)

/**
 * Test execution via the test executor
 */
ShortTestExecutorCount executor = new ShortTestExecutorCount(maestro)
executor.setBrokerURL(brokerURL)
executor.run();
maestro.stop()

