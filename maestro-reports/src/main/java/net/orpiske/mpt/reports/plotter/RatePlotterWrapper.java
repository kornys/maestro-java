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

import net.orpiske.mdp.plot.*;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

public class RatePlotterWrapper implements PlotterWrapper {
    private static final Logger logger = LoggerFactory.getLogger(RatePlotterWrapper.class);

    // NOTE: this is to work-around a bug in the records generated by an older version of the C backend. In that bug
    // the first few records would have the epoch as their value.
    private static final DateFilter dateFilter;
    private final FilteredRateDataProcessor rateDataProcessor = new FilteredRateDataProcessor(dateFilter);
    private final RateReader rateReader = new FastRateReader(rateDataProcessor);

    static {
        dateFilter = new DateFilter(new GregorianCalendar(2000, Calendar.JANUARY, 1).getTime());
    }

    @Override
    public boolean plot(final File file) {
        logger.debug("Plotting MPT compressed file {}", file.getPath());

        // Removes the gz
        String baseName = FilenameUtils.removeExtension(file.getPath());

        // Removes the csv
        baseName = FilenameUtils.removeExtension(baseName);

        try {
            if (!file.exists()) {
                throw new IOException("File " + file.getPath() + " does not exist");
            }

            rateReader.read(file.getPath());

            RateData rateData = rateDataProcessor.getRateData();

            // PlotterWrapper
            net.orpiske.mdp.plot.RatePlotter plotter = new net.orpiske.mdp.plot.RatePlotter(FilenameUtils.removeExtension(baseName));

            List<Date> ratePeriods = rateData.getRatePeriods();
            logger.info("Number of rate records to plot: {}", ratePeriods.size());
            logger.info("Number of rate records in error: {}", rateData.getErrorCount());

            if (logger.isDebugEnabled()) {
                for (Date d : ratePeriods) {
                    logger.debug("Adding date record for plotting: {}", d);
                }
            }

            plotter.setOutputWidth(1024);
            plotter.setOutputHeight(600);
            plotter.plot(ratePeriods, rateData.getRateValues());

            RatePropertyWriter.write(rateData, file.getParentFile());

            return true;
        }
        catch (Throwable t) {
            handlePlotException(file, t);
        }

        return false;
    }
}
