package de.tum.in.msrg.datagen;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.tum.in.msrg.common.Constants;
import de.tum.in.msrg.common.PageTSKey;
import de.tum.in.msrg.datamodel.ClickEvent;
import de.tum.in.msrg.datamodel.UpdateEvent;
import io.prometheus.client.Counter;
import org.apache.commons.lang3.RandomUtils;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

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
    private final int streamsAmount;
    private final Map<PageTSKey, Date> inputTimeMap;
    private final Map<PageTSKey, Map<Long, Boolean>> inputIdMap;
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
            int streamsAmount,
            Map<PageTSKey, Date> inputTimeMap,
            Map<PageTSKey, Map<Long, Boolean>> inputIdMap,
            Counter recordsCounter,
            Counter expectedCounter) throws IOException {

        this.benchmarkLength = benchmarkLength;
        this.bootstrap = bootstrap;
        this.delay = delay;
        this.delayLength = delayLength;
        this.eventsPerWindow = eventsPerWindow;
        this.streamsAmount = streamsAmount;
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

//        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
//            LOGGER.info("Shutdown hook.");
//            try {
//                Thread.sleep(6000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            LOGGER.info("Shutdown hook completed.");
//        }));

        try (KafkaProducer<byte [], byte []> kafkaProducer = new KafkaProducer<byte[], byte[]>(getKafkaProps(bootstrap, delay))) {


//        kafkaProducer.initTransactions();
//        kafkaProducer.beginTransaction();
            LOGGER.info("Producing records...");
            Instant benchmarkEndTime = Instant.now().plusSeconds(benchmarkLength);
            LOGGER.info(benchmarkEndTime.toString());

            boolean isFinished = false;
            while (!isFinished) {
                isFinished = benchmarkEndTime.isBefore(Instant.now());

                 ClickEvent clickEvent = clickDataset.next();

                 if (streamsAmount != 1) {
                     if (clickEvent.getTimestamp().getTime() > nextUpdate) {
                         for (String page : Constants.PAGES) {
                             UpdateEvent updateEvent = updateDataset.next(clickEvent.getTimestamp());
                             ProducerRecord<byte[], byte[]> producerRecord = new ProducerRecord<>("update", objectMapper.writeValueAsBytes(updateEvent.getPage()), objectMapper.writeValueAsBytes(updateEvent));
                             Future<RecordMetadata> future = kafkaProducer.send(producerRecord, new UpdateCallback(updateEvent, inputIdMap, recordsCounter));
                             if (isFinished) {
                                 future.get();
                             }
                         }
                         nextUpdate += Constants.WINDOW_SIZE.toMillis();
                     }
                 }


                ProducerRecord<byte[], byte[]> record = new ProducerRecord<>(
                        "click",
                        this.objectMapper.writeValueAsBytes(clickEvent.getPage()),
                        this.objectMapper.writeValueAsBytes(clickEvent));

                Future<RecordMetadata> future = kafkaProducer.send(record, new SendCallback(clickEvent, inputTimeMap, inputIdMap, recordsCounter, expectedCounter));
                if (isFinished){
                    future.get();
                }
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
//            LOGGER.info("Finished data generation. Sleeping...");
//
//            kafkaProducer.flush();
//            Thread.sleep(6000);
//
//            LOGGER.info("Closing data gen...");

        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }




    protected static Properties getKafkaProps(String bootstrap, long delay){
        Properties props = new Properties();

        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getCanonicalName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getCanonicalName());
//        props.put(ProducerConfig.BATCH_SIZE_CONFIG, (int)100);
        props.put(ProducerConfig.LINGER_MS_CONFIG, 100);
        props.put(ProducerConfig.CLIENT_ID_CONFIG, "datagen"+ RandomUtils.nextInt());
//        props.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, "datagen");


        return props;
    }


}
