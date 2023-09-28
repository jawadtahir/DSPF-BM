package de.tum.in.msrg.latcal;

import de.tum.in.msrg.datamodel.ClickUpdateEvent;
import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PGVOutLate implements Runnable{

    private final Counter receivedCounter;
    private final Counter duplicateCounter;
    private final Counter processedCounter;
    private final Gauge unprocessedGauge;
    private final Properties kafkaProperties;
    private final Map<PageTSKey, Map<Long, Long>> eventsProcessed;
    private final Map<PageTSKey, List<Long>> eventsExpected;
    private final Map<PageTSKey, Integer> eventsUnprocCount;

    private static final Logger LOGGER = LogManager.getLogger(PGVOutLate.class);

    public PGVOutLate (
            Map<PageTSKey, Map<Long, Long>> eventsProcessed,
            Map<PageTSKey, List<Long>> eventsExpected,
            Map<PageTSKey, Integer> eventsUnprocCount,
            Properties kafkaProperties,
            Counter receivedCounter,
            Counter duplicateCounter,
            Counter processedCounter,
            Gauge unprocessedGauge){

        this.eventsProcessed = eventsProcessed;
        this.eventsExpected = eventsExpected;
        this.eventsUnprocCount = eventsUnprocCount;
        this.kafkaProperties = (Properties) kafkaProperties.clone();
        this.receivedCounter = receivedCounter;
        this.duplicateCounter = duplicateCounter;
        this.processedCounter = processedCounter;
        this.unprocessedGauge = unprocessedGauge;

    }

    @Override
    public void run() {
        this.kafkaProperties.put(ConsumerConfig.GROUP_ID_CONFIG, "pgvLateOutputReader");
        this.kafkaProperties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ClickUpdateEventDeserializer.class.getCanonicalName());
        Counter lateCounter = Counter.build("de_tum_in_msrg_pgv_late", "Dropped events due to late arrival").labelNames("key").register();
        try
                (KafkaConsumer<String, ClickUpdateEvent> consumer = new KafkaConsumer<String, ClickUpdateEvent>(this.kafkaProperties)){
            consumer.subscribe(Arrays.asList("lateOutput"));

            while(true){
                ConsumerRecords<String, ClickUpdateEvent> records = consumer.poll(Duration.ofMillis(100));
                for (ConsumerRecord<String, ClickUpdateEvent> lateRecord : records) {
                    PageTSKey windowKey = new PageTSKey(lateRecord.value().getPage(), lateRecord.value().getTimestamp());
                    Long eventId = lateRecord.value().getId();
                    lateCounter.labels(windowKey.getPage()).inc();
                    receivedCounter.labels(windowKey.getPage()).inc();
                    Map<Long, Long> prvPrcsd = this.eventsProcessed.getOrDefault(windowKey, new ConcurrentHashMap<>());
                    if (prvPrcsd.containsKey(eventId)) {
                        prvPrcsd.put(eventId, prvPrcsd.get(eventId)+1);
                        duplicateCounter.labels(windowKey.getPage()).inc();
                    } else {
                        prvPrcsd.put(eventId, 1L);
                        this.eventsProcessed.put(windowKey, prvPrcsd);
                        processedCounter.labels(windowKey.getPage()).inc();
                    }

                    List<Long> expectedEvents = eventsExpected.getOrDefault(windowKey, null);
                    if (expectedEvents != null){
                        int unprocCount =  expectedEvents.size() - prvPrcsd.size();
//                        if (unprocCount != 0) {
                            LOGGER.info(String.format("Got late event for window: %s. Unprocessed count: %d", windowKey, unprocCount));
//                        }
                        eventsUnprocCount.put(windowKey, unprocCount);
                        PGVUnprocUpdate pgvUnprocUpdate = new PGVUnprocUpdate(eventsUnprocCount, this.unprocessedGauge);
                        pgvUnprocUpdate.update();
                    }

                }
            }

        }


    }
}
