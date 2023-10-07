package de.tum.in.msrg.datagen;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.tum.in.msrg.common.Constants;
import de.tum.in.msrg.common.PageTSKey;
import de.tum.in.msrg.datamodel.ClickEvent;
import de.tum.in.msrg.datamodel.UpdateEvent;
import io.prometheus.client.Counter;
import io.prometheus.client.exporter.HTTPServer;
import org.apache.commons.cli.*;
import org.apache.commons.lang3.RandomUtils;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
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



    private final Long benchmarkLength;
    private final String bootstrap;
    private final long delay;
    private final long delayLength;
    private final int eventsPerWindow;
    private final List<String> pages;
    private final Map<PageTSKey, Date> inputTimeMap;
    private final Map<PageTSKey, List<Long>> inputIdMap;
    private final Counter recordsCounter;
    private final Counter expectedCounter;


    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Logger LOGGER = LogManager.getLogger(KafkaDataGen.class);


    public KafkaDataGen(
            Long benchmarkLength,
            String bootstrap,
            long delay,
            long delayLength,
            int eventsPerWindow,
            Map<PageTSKey, Date> inputTimeMap,
            Map<PageTSKey, List<Long>> inputIdMap,
            Counter recordsCounter,
            Counter expectedCounter) throws IOException {

        this.benchmarkLength = benchmarkLength;
        this.bootstrap = bootstrap;
        this.delay = delay;
        this.delayLength = delayLength;
        this.eventsPerWindow = eventsPerWindow;
        this.pages = Constants.PAGES;
        this.inputTimeMap = inputTimeMap;
        this.inputIdMap = inputIdMap;
        this.recordsCounter = recordsCounter;
        this.expectedCounter = expectedCounter;
    }

    public void start() throws JsonProcessingException, InterruptedException {

        ClickDataset clickDataset = new ClickDataset(this.eventsPerWindow);
        UpdateDataset updateDataset = new UpdateDataset();

        long counter = 0L;
        // Update the pages half way the window
        long nextUpdate = (Constants.WINDOW_SIZE.toMillis() / 2);

        try (KafkaProducer<byte [], byte []> kafkaProducer = new KafkaProducer<byte[], byte[]>(getKafkaProps(bootstrap, delay))) {


//        kafkaProducer.initTransactions();
//        kafkaProducer.beginTransaction();
            LOGGER.info("Producing records...");
            Instant benchmarkEndTime = Instant.now().plusSeconds(benchmarkLength);
            LOGGER.info(benchmarkEndTime.toString());

            while (benchmarkEndTime.isAfter(Instant.now())) {
                LOGGER.debug(Instant.now().toString());

                 ClickEvent clickEvent = clickDataset.next();
                if (clickEvent.getTimestamp().getTime() > nextUpdate) {
                    for (String page : Constants.PAGES){
                        UpdateEvent updateEvent = updateDataset.next(clickEvent.getTimestamp());
                        ProducerRecord<byte[], byte[]> producerRecord = new ProducerRecord<>("update", objectMapper.writeValueAsBytes(updateEvent.getPage()), objectMapper.writeValueAsBytes(updateEvent));
                        kafkaProducer.send(producerRecord, new UpdateCallback(updateEvent, inputIdMap, recordsCounter));
                    }
                    nextUpdate += Constants.WINDOW_SIZE.toMillis();
                }


                ProducerRecord<byte[], byte[]> record = new ProducerRecord<>(
                        "click",
                        this.objectMapper.writeValueAsBytes(clickEvent.getPage()),
                        this.objectMapper.writeValueAsBytes(clickEvent));

                kafkaProducer.send(record, new SendCallback(clickEvent, inputTimeMap, inputIdMap, recordsCounter, expectedCounter));
                counter++;


//                recordsCounter.labels(clickEvent.getPage()).inc();

                if (counter == this.delay) {
                    Thread.sleep(delayLength);
                    counter = 0;
                    kafkaProducer.flush();
//                this.kafkaProducer.commitTransaction();
//                kafkaProducer.beginTransaction();
                }
            }
        }
    }




    protected static Properties getKafkaProps(String bootstrap, long delay){
        Properties props = new Properties();

        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getCanonicalName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getCanonicalName());
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, (int)delay);
        props.put(ProducerConfig.LINGER_MS_CONFIG, 100);
        props.put(ProducerConfig.CLIENT_ID_CONFIG, "datagen"+ RandomUtils.nextInt());
//        props.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, "datagen");


        return props;
    }


}
