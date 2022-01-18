package pt.rd.udpviewmulticast.benchmark;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class NetworkDegrader {

    private static int _run(String command) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder(String.format("tc qdisc %s", command).split(" "));
        builder.inheritIO();

        Process process = builder.start();
        process.waitFor(1, TimeUnit.SECONDS);
        return process.exitValue();
    }

    /*

     */

    public static int addRule(String netInterface, String rule) throws IOException, InterruptedException {
        return _run(String.format("add dev %s root netem %s", netInterface, rule));
    }

    public static int clearRules(String netInterface) throws IOException, InterruptedException {
        return _run(String.format("del dev %s root", netInterface));
    }


}
