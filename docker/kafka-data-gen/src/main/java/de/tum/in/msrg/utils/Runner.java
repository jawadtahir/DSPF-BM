package de.tum.in.msrg.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.tum.in.msrg.common.PageTSKey;
import de.tum.in.msrg.datagen.KafkaDataGen;
import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.exporter.HTTPServer;
import org.apache.commons.cli.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public class Runner {

    private static final Logger LOGGER = LogManager.getLogger(Runner.class);

    public static void main (String[] args) throws ParseException, IOException {
        Options cliOptns = createCLI();
        DefaultParser parser = new DefaultParser();
        CommandLine cmdLine = parser.parse(cliOptns, args);

        long benchmarkLength = Long.parseLong(cmdLine.getOptionValue("benchmarkLength", "300"));
        LOGGER.info(String.format("Benchmark length: %d", benchmarkLength));

        String bootstrap = cmdLine.getOptionValue("kafka", "kafka:9092");
        LOGGER.info(String.format("Kafka bootstrap server: %s", bootstrap));
//        String topic = cmdLine.getOptionValue("topic","input");
        long delay = Long.parseLong(cmdLine.getOptionValue("delay", "1000"));
        LOGGER.info(String.format("Thread sleep after %d records", delay));

        long delayLength = Long.parseLong(cmdLine.getOptionValue("length", "1"));
        LOGGER.info(String.format("Thread sleep for %d ms", delayLength));

        int eventsPerWindow = Integer.parseInt(cmdLine.getOptionValue("events", "5000"));
        LOGGER.info(String.format("Events per window: %d", eventsPerWindow));

        HTTPServer promServer = new HTTPServer(52923);
        Gauge unprocessedGauge = Gauge.build("de_tum_in_msrg_pgv_unprocessed", "Unprocessed events").labelNames("key").register();
        Counter processedCounter = Counter.build("de_tum_in_msrg_pgv_processed", "Total unique processed events").labelNames("key").register();
        Counter duplicateCounter = Counter.build("de_tum_in_msrg_pgv_duplicate", "Duplicate processed events").labelNames("key").register();
        Counter receivedInputCounter = Counter.build("de_tum_in_msrg_pgv_received_inputs", "Total received input events in output").labelNames("key").register();

        Map<PageTSKey, Date> inputTimeMap = new ConcurrentHashMap<>();
        Map<PageTSKey, List<Long>> inputIdMap = new ConcurrentHashMap<>();
        Map<PageTSKey, Map<Long, Long>> processedMap = new ConcurrentHashMap<>();
        AtomicLong largestWM = new AtomicLong(-100000L);

        ExecutorService datagenExecutor = Executors.newSingleThreadExecutor();
        Runnable datagen = () -> {
            KafkaDataGen dataGen = null;
            try {
                dataGen = new KafkaDataGen(benchmarkLength, bootstrap, delay, delayLength, eventsPerWindow, inputTimeMap,inputIdMap);
                dataGen.start();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        };
        datagenExecutor.submit(datagen);

        ExecutorService executorService = Executors.newCachedThreadPool();

        Runnable outputVerifier = new Verify(bootstrap, inputTimeMap, processedMap, eventsPerWindow, largestWM, processedCounter, duplicateCounter, receivedInputCounter);
        Runnable lateOutputVerifier = new VerifyLate(bootstrap, inputIdMap, processedMap, processedCounter, duplicateCounter, receivedInputCounter);

        executorService.submit(outputVerifier);
        executorService.submit(lateOutputVerifier);

        ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        Runnable e1Verifier = new VerifyE1(inputIdMap, processedMap, eventsPerWindow, largestWM, unprocessedGauge);
        scheduledExecutorService.scheduleWithFixedDelay(e1Verifier, 1, 1, TimeUnit.SECONDS);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("Shutdown hook");
            datagenExecutor.shutdown();
            LOGGER.info("Datagen shutdown1");
            try {
                LOGGER.info("Test");
                Thread.sleep(500);
                LOGGER.info("Test1");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            LOGGER.info("Closing prom...");
            executorService.shutdown();
            scheduledExecutorService.shutdown();
            promServer.close();
        }));


    }

    protected static Options createCLI() {
        Options options = new Options();

        Option benchmarkLengthOptn = Option.builder("benchmarkLength")
                .argName("benchmarkLengthInSeconds")
                .hasArg()
                .desc("Length of the benchmark in seconds")
                .build();

        Option kafkaOptn = Option.builder("kafka")
                .argName("bootstrap")
                .hasArg()
                .desc("Kafka bootstrap server")
                .build();

        Option epwOptn = Option.builder("events")
                .argName("eventsPerWindow")
                .hasArg()
                .desc("Events per window")
                .build();

        Option delayOptn = Option.builder("delay")
                .argName("count")
                .hasArg()
                .desc("Insert delay after count events")
                .build();

        Option delayLengthOptn = Option.builder("length")
                .argName("delayLength")
                .hasArg()
                .desc("Length of delay after count events [ms]")
                .build();



        options.addOption(kafkaOptn);
        options.addOption(epwOptn);
        options.addOption(delayOptn);
        options.addOption(delayLengthOptn);
        options.addOption(benchmarkLengthOptn);


        return options;
    }
}
