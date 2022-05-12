package de.tum.in.msrg.datagen;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.tum.in.msrg.datamodel.ClickEvent;
import org.apache.commons.cli.*;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.time.Duration;
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

    private String bootstrap;
    private String topic;
    private long delay;
    private final ObjectMapper objectMapper = new ObjectMapper();


    public KafkaDataGen(String bootstrap, String topic, long delay){
        this.bootstrap = bootstrap;
        this.topic = topic;
        this.delay = delay;
    }


    public static void main( String[] args ) throws ParseException {

        Options cliOptns = KafkaDataGen.createCLI();
        DefaultParser parser = new DefaultParser();
        CommandLine cmdLine = parser.parse(cliOptns, args);

        String bootstrap = cmdLine.getOptionValue("kafka", "kafka:9092");
        String topic = cmdLine.getOptionValue("topic","input");
        long delay = Long.parseLong(cmdLine.getOptionValue("delay", "1000"));

        KafkaDataGen dataGen = new KafkaDataGen(bootstrap, topic, delay);
        Properties kafkaPros = dataGen.getKafkaProps();

        KafkaProducer<String, String> kafkaProducer = new KafkaProducer<String, String>(kafkaPros);
        Thread shutdownKafka = new Thread(()->{
            kafkaProducer.flush();
            kafkaProducer.close();
        });
        Runtime.getRuntime().addShutdownHook(shutdownKafka);

        ClickIterator clickIterator = new ClickIterator();

        long counter = 0L;

        while (true){
            ClickEvent clickEvent = clickIterator.next();
            try {
                ProducerRecord<String, String> record = new ProducerRecord<>(dataGen.topic, clickEvent.getPage(), dataGen.objectMapper.writeValueAsString(clickEvent));
                kafkaProducer.send(record);
                counter++;

                if (counter == dataGen.delay){
                    Thread.sleep(1);
                    counter = 0;
                    kafkaProducer.flush();
                }
            } catch (JsonProcessingException | InterruptedException | KafkaException e) {
                e.printStackTrace();
            }
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

        Option topicOptn = Option.builder("topic")
                .argName("topic")
                .hasArg()
                .desc("Kafka input topic")
                .build();

        Option delayOptn = Option.builder("delay")
                .argName("count")
                .hasArg()
                .desc("Delay of 1ms after count events")
                .build();



        options.addOption(kafkaOptn);
        options.addOption(topicOptn);
        options.addOption(delayOptn);


        return options;
    }


    static class ClickIterator  {

        private Map<String, Long> nextTimestampPerKey;
        private int nextPageIndex;

        ClickIterator() {
            nextTimestampPerKey = new HashMap<>();
            nextPageIndex = 0;
        }

        ClickEvent next() {
            String page = nextPage();
            return new ClickEvent(nextTimestamp(page), page);
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

}
