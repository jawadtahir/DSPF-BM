package de.tum.in.msrg.utils;

import de.tum.in.msrg.common.Constants;
import de.tum.in.msrg.common.PageStatisticsDeserializer;
import de.tum.in.msrg.common.PageTSKey;
import de.tum.in.msrg.datamodel.PageStatistics;
import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class Verify implements Runnable {

    final String bootstrap;
    Map<PageTSKey, Date> inputTimeMap;
    Map<PageTSKey, List<Long>> processedMap;
    Counter processedCounter;
    Counter duplicateCounter;
    Counter receivedInputCounter;


    public Verify(
            String bootstrap,
            Map<PageTSKey, Date> inputTimeMap,
            Map<PageTSKey, List<Long>> processedMap,
            Counter processedCounter,
            Counter duplicateCounter,
            Counter receivedInputCounter) {

        this.bootstrap = bootstrap;
        this.inputTimeMap = inputTimeMap;
        this.processedMap = processedMap;
        this.processedCounter = processedCounter;
        this.duplicateCounter = duplicateCounter;
        this.receivedInputCounter = receivedInputCounter;
    }

    @Override
    public void run() {
        Gauge latencyGauge = Gauge.build("de_tum_in_msrg_latcal_latency", "End-to-end latency").labelNames("key").register();
        Counter receivedCounter = Counter.build("de_tum_in_msrg_pgv_received", "Total received events").labelNames("key").register();
        Counter correctOutputCounter = Counter.build("de_tum_in_msrg_pgv_correct_output", "Correct outputs").labelNames("key").register();
        Counter inCorrectOutputCounter = Counter.build("de_tum_in_msrg_pgv_incorrect_output", "incorrect outputs").labelNames("key").register();
        Counter inCorrectOutputHigherCounter = Counter.build("de_tum_in_msrg_pgv_incorrect_higher_output", "incorrect outputs, higher than expected").labelNames("key").register();
        Counter inCorrectOutputLowerCounter = Counter.build("de_tum_in_msrg_pgv_incorrect_lower_output", "incorrect outputs, lower than expected").labelNames("key").register();

        try (KafkaConsumer<String, PageStatistics> kafkaConsumer = new KafkaConsumer<String, PageStatistics>(getKafkaProperties())) {
            kafkaConsumer.subscribe(Arrays.asList(Constants.OUTPUT_TOPIC));
            while (true){
                ConsumerRecords<String, PageStatistics> consumerRecords = kafkaConsumer.poll(Duration.ofMillis(200));
                for (ConsumerRecord<String, PageStatistics> record : consumerRecords){
                    PageTSKey key = new PageTSKey(record.value().getPage(), record.value().getWindowStart());
                    PageTSKey next1key = new PageTSKey(record.value().getPage(), record.value().getWindowEnd());
                    PageTSKey next2key = new PageTSKey(record.value().getPage(), new Date(record.value().getWindowEnd().getTime() + 60_000) );

                    List<Long> processedIds = processedMap.getOrDefault(key, Collections.synchronizedList(new ArrayList<>()));

                    List<Long> receivedIds = record.value().getClickIds();
                    receivedIds.addAll(record.value().getUpdateIds());


                    receivedCounter.labels(key.getPage()).inc();

                    Date ingestionTime = inputTimeMap.get(next1key);
                    Date egressTime = new Date(record.timestamp());
                    long latency = egressTime.getTime() - ingestionTime.getTime();
                    latencyGauge.labels(next1key.getPage()).set(latency);



                    for (Long id : receivedIds){
                        receivedInputCounter.labels(key.getPage()).inc();
                        boolean found = processedIds.contains(id);
                        if (found){
                            duplicateCounter.labels(key.getPage()).inc();
                        } else {
                            processedIds.add(id);
                            processedCounter.labels(key.getPage()).inc();
                        }
                    }


                    if (receivedIds.size() == processedIds.size()){
                        correctOutputCounter.labels(key.getPage()).inc();
                    } else {
                        inCorrectOutputCounter.labels(key.getPage()).inc();
                        if (receivedIds.size() < processedIds.size()){
                            inCorrectOutputLowerCounter.labels(key.getPage()).inc();
                        } else {
                            inCorrectOutputHigherCounter.labels(key.getPage()).inc();
                        }
                    }

                    processedMap.put(key, processedIds);


                }
            }
        }


    }
    Properties getKafkaProperties(){
        Properties properties = new Properties();

        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getCanonicalName());
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, PageStatisticsDeserializer.class.getCanonicalName());
        properties.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, "outputVerifier");

        return properties;
    }
}
