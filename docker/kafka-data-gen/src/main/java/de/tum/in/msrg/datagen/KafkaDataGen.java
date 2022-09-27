package de.tum.in.msrg.datagen;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.tum.in.msrg.datamodel.ClickEvent;
import de.tum.in.msrg.datamodel.UpdateEvent;
import io.prometheus.client.Counter;
import io.prometheus.client.exporter.HTTPServer;
import org.apache.commons.cli.*;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Hello world!
 *
 */
public class KafkaDataGen
{
    public static final Duration WINDOW_SIZE = Duration.of(60, ChronoUnit.SECONDS );
    public static final int EVENTS_PER_WINDOW = 5000;
    private static final List<String> pages = Arrays.asList("/help", "/index", "/shop", "/jobs", "/about", "/news");
    //this calculation is only accurate as long as pages.size() * EVENTS_PER_WINDOW divides the
    //window size
    public static final long DELAY = WINDOW_SIZE.toMillis() / pages.size() / EVENTS_PER_WINDOW;

    private final String bootstrap;
    private final long delay;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Logger LOGGER = LogManager.getLogger(KafkaDataGen.class);


    public KafkaDataGen(String bootstrap, long delay){
        this.bootstrap = bootstrap;

        this.delay = delay;
    }


    public static void main( String[] args ) throws ParseException, IOException {

        Options cliOptns = KafkaDataGen.createCLI();
        DefaultParser parser = new DefaultParser();
        CommandLine cmdLine = parser.parse(cliOptns, args);

        String bootstrap = cmdLine.getOptionValue("kafka", "kafka:9092");
        LOGGER.info(String.format("Kafka bootstrap server: %s", bootstrap));
//        String topic = cmdLine.getOptionValue("topic","input");
        long delay = Long.parseLong(cmdLine.getOptionValue("delay", "1000"));
        LOGGER.info(String.format("Thread sleep for 1 ms after %d records", delay));

        KafkaDataGen dataGen = new KafkaDataGen(bootstrap, delay);

        Path rootReportFolder = Paths.get("/reports", Instant.now().toString());
        Path createdDir = Files.createDirectories(rootReportFolder);
        FileWriter fileWriter = new FileWriter(createdDir.resolve("datagen.txt").toFile(), false);


        Properties kafkaPros = dataGen.getKafkaProps();
        LOGGER.info(String.format("Kafka properties: %s", kafkaPros.toString()));

        LOGGER.info("Creating Kafka producer and prom server at port 52923...");
        try (
                HTTPServer promServer = new HTTPServer(52923);
                KafkaProducer<String, String> kafkaProducer = new KafkaProducer<>(kafkaPros)
        ){
            LOGGER.info("Creating counters...");
            Counter recordsCounter = Counter.build("de_tum_in_msrg_datagen_records_total", "Total number of messages generated by the generator").labelNames("key").register();
            Counter idRolloverCounter = Counter.build("de_tum_in_msrg_datagen_id_rollover_total", "Total number of times ID roll overed").register();

            ClickIterator clickIterator = new ClickIterator();

            long counter = 0L;
            // Update the pages half way the window
            long nextUpdate = (WINDOW_SIZE.toMillis()/2);

            LOGGER.info("Producing records...");
            while (true){
                ClickEvent clickEvent = clickIterator.next();
                if (clickEvent.getTimestamp().getTime() > nextUpdate){
                    updatePages(clickEvent, kafkaProducer, dataGen.objectMapper);
                    nextUpdate += WINDOW_SIZE.toMillis();
                }



                ProducerRecord<String, String> record = new ProducerRecord<>("click", clickEvent.getPage(), dataGen.objectMapper.writeValueAsString(clickEvent));
                kafkaProducer.send(record);
                counter++;


                recordsCounter.labels(clickEvent.getPage()).inc();
                if (clickEvent.getId() == Long.MIN_VALUE){
                    idRolloverCounter.inc();
                }

                if (counter == dataGen.delay){
                    Thread.sleep(1);
                    counter = 0;
                    kafkaProducer.flush();
                }


            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        }


    }

    protected static void updatePages(ClickEvent clickEvent, KafkaProducer<String, String> kafkaProducer, ObjectMapper objectMapper) throws JsonProcessingException {
        for (String page : pages){
            UpdateEvent updateEvent = new UpdateEvent(clickEvent.getId(), clickEvent.getTimestamp(), page, "");
            ProducerRecord<String, String> producerRecord = new ProducerRecord<>("update", updateEvent.getPage(), objectMapper.writeValueAsString(updateEvent));
            kafkaProducer.send(producerRecord);
        }
    }

    protected Properties getKafkaProps(){
        Properties props = new Properties();

        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, this.bootstrap);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getCanonicalName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getCanonicalName());


        return props;
    }

    protected static Options createCLI(){
        Options options = new Options();

        Option kafkaOptn = Option.builder("kafka")
                .argName("bootstrap")
                .hasArg()
                .desc("Kafka bootstrap server")
                .build();

//        Option topicOptn = Option.builder("topic")
//                .argName("topic")
//                .hasArg()
//                .desc("Kafka input topic")
//                .build();

        Option delayOptn = Option.builder("delay")
                .argName("count")
                .hasArg()
                .desc("Delay of 1ms after count events")
                .build();



        options.addOption(kafkaOptn);
//        options.addOption(topicOptn);
        options.addOption(delayOptn);


        return options;
    }


    static class ClickIterator  {

        private Map<String, Long> nextTimestampPerKey;
        private int nextPageIndex;
        private long id = Long.MIN_VALUE;

        ClickIterator() {
            nextTimestampPerKey = new HashMap<>();
            nextPageIndex = 0;
        }

        ClickEvent next() {
            String page = nextPage();
            id++;
            if (id == Long.MAX_VALUE) {
                id = Long.MIN_VALUE;
            }
            return new ClickEvent(id, nextTimestamp(page), page);
        }

        private Date nextTimestamp(String page) {
            long nextTimestamp = nextTimestampPerKey.getOrDefault(page, 1L);
//			nextTimestampPerKey.put(page, nextTimestamp + WINDOW_SIZE.toMilliseconds() / EVENTS_PER_WINDOW);
            nextTimestampPerKey.put(page, nextTimestamp + (WINDOW_SIZE.toMillis()  / EVENTS_PER_WINDOW ) );
            return new Date(nextTimestamp);
        }

        private String nextPage() {
            String nextPage = pages.get(nextPageIndex);
            if (nextPageIndex == pages.size() - 1) {
                nextPageIndex = 0;
            } else {
                nextPageIndex++;
            }
            return nextPage;
        }
    }

    static class UpdateIterator {
        private long lastUpdated = 30000L;
    }

}
