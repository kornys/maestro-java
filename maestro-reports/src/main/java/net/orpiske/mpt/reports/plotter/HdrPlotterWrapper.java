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

package net.orpiske.mpt.reports.plotter;

import net.orpiske.hhp.plot.HdrData;
import net.orpiske.hhp.plot.HdrLogProcessorWrapper;
import net.orpiske.hhp.plot.HdrReader;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class HdrPlotterWrapper implements PlotterWrapper {
    private static Logger logger = LoggerFactory.getLogger(HdrPlotterWrapper.class);
    private static String DEFAULT_UNIT_RATE = "1000";

    private HdrLogProcessorWrapper processorWrapper;
    private HdrReader reader = new HdrReader();

    public HdrPlotterWrapper() {
        this(DEFAULT_UNIT_RATE);
    }

    public HdrPlotterWrapper(final String unitRate) {
         processorWrapper = new HdrLogProcessorWrapper(unitRate);
    }

    @Override
    public boolean plot(final File file) {
        logger.debug("Plotting HDR file {}", file.getPath());

        try {
            if (!file.exists()) {
                throw new IOException("File " + file.getPath() + " does not exist");
            }

            String csvFile = processorWrapper.convertLog(file.getPath());

            // CSV Reader
            HdrData hdrData = reader.read(csvFile);

            // HdrPlotterWrapper
            net.orpiske.hhp.plot.HdrPlotter plotter = new net.orpiske.hhp.plot.HdrPlotter(FilenameUtils.removeExtension(file.getPath()));

            plotter.setOutputWidth(1024);
            plotter.setOutputHeight(600);
            plotter.plot(hdrData.getPercentile(), hdrData.getValue());

            return true;
        }
        catch (Throwable t) {
            FilenameUtils.removeExtension(file.getPath());

            handlePlotException(file, t);
        }

        return false;
    }
}