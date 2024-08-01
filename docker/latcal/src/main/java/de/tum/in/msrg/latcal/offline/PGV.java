package de.tum.in.msrg.latcal.offline;

import de.tum.in.msrg.datamodel.ClickEvent;
import de.tum.in.msrg.datamodel.ClickUpdateEvent;
import de.tum.in.msrg.datamodel.PageStatistics;
import de.tum.in.msrg.latcal.ClickEventDeserializer;
import de.tum.in.msrg.latcal.ClickUpdateEventDeserializer;
import de.tum.in.msrg.latcal.PageStatisticsDeserializer;
import de.tum.in.msrg.latcal.PageTSKey;
import org.apache.commons.cli.*;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.IsolationLevel;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.util.*;

public class PGV {

    private String bootstrap;
    private PGVResult result;
    private long numEventsPerWindow;


    private Properties kafkaProperties;
    private KafkaConsumer<String, ClickEvent> clickConsumer;
    private KafkaConsumer<String, PageStatistics> statsConsumer;
    private KafkaConsumer<String, ClickUpdateEvent> lateConsumer;
    private Map<PageTSKey, List<Long>> expectedOutput;
    private Map<PageTSKey, List<Long>> receivedOutput;


    private static final Logger LOGGER = LogManager.getLogger(PGV.class);

    public PGV (String bootstrap, Integer numEventsPerWindow){
        this.bootstrap = bootstrap;
        this.numEventsPerWindow = numEventsPerWindow;
        this.kafkaProperties = getKafkaProperties();
        this.kafkaProperties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, this.bootstrap);
        this.result = new PGVResult();

        expectedOutput = new HashMap<>();
        receivedOutput = new HashMap<>();
    }

    public static void main (String[] args) throws ParseException {

        DefaultParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(getCLIOptns(), args);

        String bootstrap = cmd.getOptionValue("kafka", "kafka1:9092");
        LOGGER.info(String.format("Kafka bootstrap server: %s", bootstrap));

        Integer eventsPerWindow = Integer.parseInt(cmd.getOptionValue("events", "5000"));
        LOGGER.info(String.format("Events per window: %d", eventsPerWindow));

        PGV pgv = new PGV(bootstrap, eventsPerWindow);
        pgv.verfify();
        System.out.println(pgv.result.toString());


    }

    protected void verfify(){

        readClickStream();
        readStatsStream();
        readLateStream();
        calculateUnprocessedEvents();
        writeResults();

    }

    protected void configureKafkaConsumers(Properties kafkaProperties){
        Properties clickProperties = (Properties) kafkaProperties.clone();
        clickProperties.put(ConsumerConfig.GROUP_ID_CONFIG, "pgvClickReader");
        clickProperties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ClickUpdateEventDeserializer.class.getCanonicalName());
        clickConsumer = new KafkaConsumer<String, ClickEvent>(clickProperties);
        clickConsumer.subscribe(Arrays.asList("click"));

    }

    protected void readClickStream(){

        Properties clickProperties = (Properties) this.kafkaProperties.clone();
        clickProperties.put(ConsumerConfig.GROUP_ID_CONFIG, "pgvClickReader");
        clickProperties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ClickEventDeserializer.class.getCanonicalName());
        clickConsumer = new KafkaConsumer<String, ClickEvent>(clickProperties);

        List<PartitionInfo> partitionInfos =   clickConsumer.partitionsFor("click");
        for (PartitionInfo partitionInfo : partitionInfos){
            TopicPartition topicPartition = new TopicPartition(partitionInfo.topic(), partitionInfo.partition());

            Collection<TopicPartition> topicPartitions = Arrays.asList(topicPartition);
            Map<TopicPartition, Long> topicPartitionEndOffset = clickConsumer.endOffsets(topicPartitions);
            Map<TopicPartition, Long> topicPartitionStartOffset = clickConsumer.beginningOffsets(topicPartitions);
            clickConsumer.assign(topicPartitions);
//            clickConsumer.seekToBeginning(topicPartitions);
            while (true){
                ConsumerRecords<String, ClickEvent> records = clickConsumer.poll(Duration.ofMillis(100));
                for (ConsumerRecord<String, ClickEvent> record : records.records(topicPartition)) {
                    this.result.incReadInputEvents();
                    PageTSKey key = new PageTSKey(record.value().getPage(), record.value().getTimestamp());
                    Long eventId = record.value().getId();
                    List<Long> previousIds = expectedOutput.getOrDefault(key, null);
                    if (previousIds == null){
                        previousIds = new ArrayList<Long>();
                        this.result.incExpectedOutputs();
                    }
                    previousIds.add(eventId);
                    expectedOutput.put(key, previousIds);
                }
                clickConsumer.commitSync();
                if (clickConsumer.position(topicPartition) == topicPartitionEndOffset.get(topicPartition)){
                    LOGGER.info(String.format("Reached head: %d", clickConsumer.position(topicPartition)));
                    clickConsumer.seekToBeginning(topicPartitions);
                    break;
                }
            }
        }
        LOGGER.info(this.result.toString());

    }

    protected void readStatsStream(){
        Properties statsProperties = (Properties) this.kafkaProperties.clone();
        statsProperties.put(ConsumerConfig.GROUP_ID_CONFIG, "pgvStatsReader");
        statsProperties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, PageStatisticsDeserializer.class.getCanonicalName());
        statsConsumer = new KafkaConsumer<String, PageStatistics>(statsProperties);

        List<PartitionInfo> partitionInfos = statsConsumer.partitionsFor("output");
        for (PartitionInfo partitionInfo: partitionInfos){
            TopicPartition topicPartition = new TopicPartition(partitionInfo.topic(), partitionInfo.partition());
            Collection<TopicPartition> topicPartitions = Arrays.asList(topicPartition);
            Map<TopicPartition, Long> topicPartitionEndOffset = statsConsumer.endOffsets(topicPartitions);
            statsConsumer.assign(topicPartitions);
            while (true){
                ConsumerRecords<String, PageStatistics> records = statsConsumer.poll(Duration.ofMillis(100));
                LOGGER.info(String.format("Current position: %d", statsConsumer.position(topicPartition)));
                for (ConsumerRecord<String, PageStatistics> record: records.records(topicPartition)){
                    this.result.incReadOutputs();
                    if (record.value().getClickIds().size() == this.numEventsPerWindow){
                        this.result.incCorrectOutputs();
                    } else if (record.value().getClickIds().size() < this.numEventsPerWindow) {
                        this.result.incLowerIncorrectOutputs();
                    } else {
                        this.result.incHigherIncorrectOutputs();
                    }

                    PageTSKey key = new PageTSKey(record.value().getPage(), record.value().getWindowStart());
                    List<Long> prevProcessedEvents = receivedOutput.getOrDefault(key, new ArrayList<Long>());
                    List<Long> processedEvents = record.value().getClickIds();

                    for (Long eventId : processedEvents) {
                        this.result.incReadOutputInputs();
                        if (prevProcessedEvents.contains(eventId)){
                            this.result.incDuplicateEvents();
                        } else {
                            this.result.incProcessedEvents();
                            prevProcessedEvents.add(eventId);
                        }
                    }
                    receivedOutput.put(key, prevProcessedEvents);
                }
                statsConsumer.commitSync();
                if (statsConsumer.position(topicPartition) == topicPartitionEndOffset.get(topicPartition)){
                    statsConsumer.seekToBeginning(topicPartitions);
                    break;
                }
            }
        }
        LOGGER.info(this.result.toString());
    }

    protected void readLateStream(){
        Properties lateProperties = (Properties) this.kafkaProperties.clone();
        lateProperties.put(ConsumerConfig.GROUP_ID_CONFIG, "pgvLateReader");
        lateProperties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ClickUpdateEventDeserializer.class.getCanonicalName());
        lateConsumer = new KafkaConsumer<String, ClickUpdateEvent>(lateProperties);

        List<PartitionInfo> partitionInfos = lateConsumer.partitionsFor("lateOutput");
        for (PartitionInfo partitionInfo : partitionInfos){
            TopicPartition topicPartition = new TopicPartition(partitionInfo.topic(), partitionInfo.partition());
            Collection<TopicPartition> topicPartitions = Arrays.asList(topicPartition);
            Map<TopicPartition, Long> topicPartitionEndOffset = lateConsumer.endOffsets(topicPartitions);
            lateConsumer.assign(topicPartitions);
            while (true){
                ConsumerRecords<String, ClickUpdateEvent> records = lateConsumer.poll(Duration.ofMillis(100));
                for (ConsumerRecord<String, ClickUpdateEvent>record:records.records(topicPartition)){
                    PageTSKey key = new PageTSKey(record.value().getPage(), record.value().getClickTimestamp());
                    Long eventId = record.value().getClickId();
                    this.result.incReadOutputInputs();
                    List<Long> previousIds = receivedOutput.getOrDefault(key, new ArrayList<Long>());
                    if (previousIds.contains(eventId)){
                        this.result.incDuplicateEvents();
                    } else {
                        this.result.incProcessedEvents();
                        previousIds.add(eventId);
                    }
                    receivedOutput.put(key, previousIds);
                }
                lateConsumer.commitSync();
                if (lateConsumer.position(topicPartition) == topicPartitionEndOffset.get(topicPartition)){
                    lateConsumer.seekToBeginning(topicPartitions);
                    break;
                }
            }
        }
        LOGGER.info(this.result.toString());

    }

    protected void calculateUnprocessedEvents(){

        expectedOutput.forEach((pageTSKey, longs) -> {
            long expectedSize = longs.size();
            if (expectedSize == this.numEventsPerWindow) {
                List<Long> receivedIds = receivedOutput.getOrDefault(pageTSKey, new ArrayList<Long>());
                Long unprocCount = expectedSize - receivedIds.size();
                if (unprocCount != 0){
                    LOGGER.info(String.format("Unprocessed events found. Key: %s", pageTSKey));
                }
                this.result.setUnproceesedEvents(this.result.getUnproceesedEvents()+unprocCount);
            }
        });
        LOGGER.info(this.result.toString());
    }

    protected void writeResults(){
        LOGGER.info(this.result.toString());
    }

    protected static Options getCLIOptns(){
        Options cliOptions = new Options();

        Option kafkaOptn = Option.builder("kafka")
                .hasArg(true)
                .argName("bootstrap")
                .desc("Bootstrap kafka server")
                .build();

        Option epwOptn = Option.builder("events")
                .hasArg(true)
                .argName("perWindow")
                .desc("Events per window")
                .build();


        cliOptions.addOption(kafkaOptn);
        cliOptions.addOption(epwOptn);

        return  cliOptions;
    }

    protected Properties getKafkaProperties(){
        Properties properties = new Properties();

        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getCanonicalName());
        properties.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");

        return properties;
    }


}
