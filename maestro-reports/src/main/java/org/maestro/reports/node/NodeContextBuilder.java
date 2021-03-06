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

package org.maestro.reports.node;

import org.maestro.common.Constants;
import org.maestro.common.test.InspectorProperties;
import org.maestro.common.test.TestProperties;
import org.maestro.reports.files.ReportDirInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static org.maestro.common.PropertyUtils.loadProperties;

public class NodeContextBuilder {
    private static final Logger logger = LoggerFactory.getLogger(NodeContextBuilder.class);

    private NodeContextBuilder() {}

    public static Map<String, Object> toContext(ReportDirInfo reportDirInfo) {
        Map<String, Object> context = new HashMap<>();

        logger.trace("Loading context for {}", reportDirInfo);

        context.put("node", reportDirInfo.getNodeHost());
        context.put("nodeType", reportDirInfo.getNodeType());
        context.put("testNumber", reportDirInfo.getTestNum());
        context.put("result", reportDirInfo.getResultTypeString());
        context.put("reportDirInfo", reportDirInfo);
        context.put("maestroVersion", Constants.VERSION);

        loadProperties(new File(reportDirInfo.getReportDir(), TestProperties.FILENAME), context);
        loadProperties(new File(reportDirInfo.getReportDir(), InspectorProperties.FILENAME), context);
        loadProperties(new File(reportDirInfo.getReportDir(),"broker.properties"), context);
        loadProperties(new File(reportDirInfo.getReportDir(),"rate.properties"), context);

        /**
         * The latency.properties file is generated by HDR Histogram Plotter library. They are stored with nanosecond
         * precision, later in the template processing they are divided by 1000 to adjust for microsecond precision
         */
        loadProperties(new File(reportDirInfo.getReportDir(),"latency.properties"), context);

        return context;
    }



}
