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
package net.orpiske.mpt.main.actions;

import net.orpiske.mpt.common.exceptions.MaestroException;
import net.orpiske.mpt.maestro.notes.MaestroNote;
import org.apache.commons.cli.*;

import java.io.IOException;
import java.util.List;

public class MaestroAction extends Action {
    private CommandLine cmdLine;
    private Options options;

    private String maestroUrl;
    private String command;

    public MaestroAction(String[] args) {
        processCommand(args);
    }

    protected void processCommand(String[] args) {
        CommandLineParser parser = new PosixParser();

        options = new Options();

        options.addOption("h", "help", false, "prints the help");
        options.addOption("m", "maestro-url", true, "maestro URL to connect to");
        options.addOption("c", "command", true, "maestro command [ping, flush, stats, stop]");

        try {
            cmdLine = parser.parse(options, args);
        } catch (ParseException e) {
            help(options, -1);
        }

        if (cmdLine.hasOption("help")) {
            help(options, 0);
        }

        maestroUrl = cmdLine.getOptionValue('m');
        if (maestroUrl == null) {
            help(options, -1);
        }

        command = cmdLine.getOptionValue('c');
        if (command == null) {
            help(options, -1);
        }
    }

    public int run() {
        net.orpiske.mpt.maestro.Maestro maestro = null;
        try {
            maestro = new net.orpiske.mpt.maestro.Maestro(maestroUrl);

            switch (command) {
                case "ping": {
                    maestro.pingRequest();
                    break;
                }
                case "flush": {
                    maestro.flushRequest();
                    break;
                }
                case "stats": {
                    maestro.statsRequest();
                    break;
                }
                case "halt": {
                    maestro.halt();
                    break;
                }
                case "stop": {
                    maestro.stopSender();
                    maestro.stopReceiver();
                    maestro.stopInspector();
                    break;
                }
            }

            List<MaestroNote> replies = maestro.collect(1000, 10);

            for (MaestroNote note : replies) {
                System.out.println("Reply: " + note);
            }

            return 0;
        } catch (MaestroException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println(e.getMessage());
        }

        return 1;
    }
}
