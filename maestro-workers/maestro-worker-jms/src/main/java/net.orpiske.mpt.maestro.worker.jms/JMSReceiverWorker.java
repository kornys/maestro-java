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

package net.orpiske.mpt.maestro.worker.jms;

import net.orpiske.mpt.common.client.MaestroReceiver;
import net.orpiske.mpt.common.duration.TestDuration;
import net.orpiske.mpt.common.duration.TestDurationBuilder;
import net.orpiske.mpt.common.exceptions.DurationParseException;
import net.orpiske.mpt.common.worker.MaestroReceiverWorker;
import net.orpiske.mpt.common.worker.MessageInfo;
import net.orpiske.mpt.common.worker.WorkerOptions;
import net.orpiske.mpt.common.worker.WorkerStateInfo;
import net.orpiske.mpt.common.writers.OneToOneWorkerChannel;
import org.HdrHistogram.Histogram;
import org.HdrHistogram.SingleWriterRecorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class JMSReceiverWorker implements MaestroReceiverWorker {
    private static final Logger logger = LoggerFactory.getLogger(JMSReceiverWorker.class);

    private TestDuration duration;
    private final AtomicLong messageCount = new AtomicLong(0);
    private volatile long startedEpochMillis = Long.MIN_VALUE;
    //TODO it could be injected by outside because the precision could be improved using ad-hoc clock timers
    private final SingleWriterRecorder latencyRecorder = new SingleWriterRecorder(TimeUnit.HOURS.toMillis(1), 3);
    //TODO the size need to be configured
    private final OneToOneWorkerChannel workerChannel = new OneToOneWorkerChannel(128 * 1024);
    private MaestroReceiver receiverEndpoint;

    private volatile WorkerStateInfo workerStateInfo = new WorkerStateInfo();

    private String url;


    @Override
    public long messageCount() {
        return messageCount.get();
    }

    @Override
    public WorkerStateInfo getWorkerState() {
        return workerStateInfo;
    }


    @Override
    public long startedEpochMillis() {
        return startedEpochMillis;
    }

    private void setFCL(String fcl) {

    }

    private void setBroker(String url) {
        this.url = url;
    }

    private void setDuration(String duration) {
        try {
            this.duration = TestDurationBuilder.build(duration);
        } catch (DurationParseException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setWorkerOptions(WorkerOptions workerOptions) {
        setBroker(workerOptions.getBrokerURL());
        setDuration(workerOptions.getDuration());
    }

    public void start() {
        startedEpochMillis = System.currentTimeMillis();
        logger.info("Starting the test");

        try {
            JMSReceiverClient client = new JMSReceiverClient();

            client.setUrl(url);

            workerStateInfo.setState(true, null, null);
            client.start();

            long count = 0;

            while (duration.canContinue(this) && isRunning()) {
                final MessageInfo info = client.receiveMessages();
                //TODO would be better to use a general purpose Clock API
                final long now = System.currentTimeMillis();
                final long creationTime = info.getCreationTime().toEpochMilli();
                final long elapsedMillis = now - creationTime;
                //we can perform fnc check here to fail the test
                //TODO use Histogram:recordValueWithExpectedInterval if the rate is known
                latencyRecorder.recordValue(elapsedMillis);
                workerChannel.emitRate(creationTime, now);
                count++;
                messageCount.lazySet(count);
            }

            logger.info("Test completed successfully");
            workerStateInfo.setState(false, WorkerStateInfo.WorkerExitStatus.WORKER_EXIT_SUCCESS, null);
        } catch (Exception e) {
            logger.error("Unable to start the receiver worker: {}", e.getMessage(), e);

            workerStateInfo.setState(false, WorkerStateInfo.WorkerExitStatus.WORKER_EXIT_FAILURE, e);
        }
    }

    @Override
    public Histogram takeLatenciesSnapshot(Histogram intervalHistogram) {
        return latencyRecorder.getIntervalHistogram(intervalHistogram);
    }

    @Override
    public boolean isRunning() {
        return workerStateInfo.isRunning();
    }

    @Override
    public void stop() {
        workerStateInfo.setState(false, WorkerStateInfo.WorkerExitStatus.WORKER_EXIT_STOPPED, null);
    }

    @Override
    public void halt() {
        stop();
    }

    @Override
    public void run() {
        start();
    }
}
