package de.tum.in.msrg.flink;

import de.tum.in.msrg.datamodel.ClickEvent;
import de.tum.in.msrg.datamodel.ClickUpdateEvent;
import de.tum.in.msrg.datamodel.PageStatistics;
import de.tum.in.msrg.flink.connector.CustomKafkaSource;
import de.tum.in.msrg.flink.functions.CountProcessWindowFunction;
import de.tum.in.msrg.flink.serialization.*;
import org.apache.commons.cli.*;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.configuration.*;
import org.apache.flink.connector.base.DeliveryGuarantee;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.connector.kafka.source.KafkaSourceOptions;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.util.OutputTag;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.util.Properties;

/**
 * Hello world!
 *
 */
public class App 
{
    private static final Logger LOGGER = LogManager.getLogger(App.class);

    public static void main( String[] args ) throws Exception {

        DefaultParser parser = new DefaultParser();
        CommandLine cmdLine = parser.parse(getCliOptns(), args);

        String kafka = cmdLine.getOptionValue("kafka", "kafka1:9092");
        LOGGER.info(String.format("kafka: %s", kafka));
//        String pg = cmdLine.getOptionValue("pg", "exactly once");

        Configuration configuration = new Configuration();
//        configuration.set(TaskManagerOptions.NUM_TASK_SLOTS, 2);
//        configuration.set(CoreOptions.DEFAULT_PARALLELISM, 2);
//        configuration.set(RestOptions.PORT, 48081);

        StreamExecutionEnvironment environment = StreamExecutionEnvironment.getExecutionEnvironment();


        Properties srcProperties = new Properties();

        srcProperties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka);
        srcProperties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        srcProperties.put(ConsumerConfig.GROUP_ID_CONFIG, "flink-topology");


        Properties sinkProperties = new Properties();

        sinkProperties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka);
        sinkProperties.put(ProducerConfig.TRANSACTION_TIMEOUT_CONFIG, 3000);




        CustomKafkaSource<ClickEvent> clickSource = CustomKafkaSource.<ClickEvent>builder()
                .setTopics("click")
                .setBootstrapServers(kafka)
                .setStartingOffsets(OffsetsInitializer.earliest())
                .setProperties(srcProperties)
                .setProperty(KafkaSourceOptions.CLIENT_ID_PREFIX.key(), "flink-topology")
                .setValueOnlyDeserializer(new ClickEventDeserializer())
                .build();

//        CustomKafkaSource<UpdateEvent> updateSource = CustomKafkaSource.<UpdateEvent>builder()
//                .setTopics("update")
//                .setBootstrapServers(kafka)
//                .setProperties(srcProperties)
//                .setProperty(KafkaSourceOptions.CLIENT_ID_PREFIX.key(), "flink-topology")
//                .setValueOnlyDeserializer(new UpdateEventDeserializer())
//                .build();

        KafkaSink<PageStatistics> statsSink = KafkaSink.<PageStatistics>builder()
                .setBootstrapServers(kafka)
                .setKafkaProducerConfig(sinkProperties)
                .setDeliverGuarantee(DeliveryGuarantee.EXACTLY_ONCE)
                .setTransactionalIdPrefix("stat-sink").
                setRecordSerializer(KafkaRecordSerializationSchema.builder()
                        .setTopic("output")
                        .setValueSerializationSchema(new PageStatisticsValueSerializer())
                        .setKeySerializationSchema(new PageStatisticsKeyValueSerializer())
                        .build())
                .build();

        KafkaSink<ClickUpdateEvent> lateSink = KafkaSink.<ClickUpdateEvent>builder()
                .setBootstrapServers(kafka)
                .setKafkaProducerConfig(sinkProperties)
                .setDeliverGuarantee(DeliveryGuarantee.EXACTLY_ONCE)
                .setTransactionalIdPrefix("late-sink").
                setRecordSerializer(KafkaRecordSerializationSchema.builder()
                        .setTopic("lateOutput")
                        .setValueSerializationSchema(new ClickUpdateValueSerializer())
                        .setKeySerializationSchema(new ClickUpdateKeySerializer())
                        .build())
                .build();



        WatermarkStrategy<ClickEvent> clickEventWatermarkStrategy = WatermarkStrategy
                .<ClickEvent>forBoundedOutOfOrderness(Duration.ofMillis(200))
                .withIdleness(Duration.ofMillis(1000))
                .withTimestampAssigner((element, recordTimestamp) -> element.getTimestamp().getTime());

//        WatermarkStrategy<UpdateEvent> updateEventWatermarkStrategy = WatermarkStrategy
//                .<UpdateEvent>forBoundedOutOfOrderness(Duration.ofMillis(200))
//                .withIdleness(Duration.ofMillis(200))
//                .withTimestampAssigner((element, recordTimestamp) -> element.getTimestamp().getTime());



        DataStreamSource<ClickEvent> clickSrc = environment.fromSource(clickSource, clickEventWatermarkStrategy, "ClickEvent-src");
//        DataStreamSource<UpdateEvent> updateSrc = environment.fromSource(updateSource, updateEventWatermarkStrategy, "UpdateEvent-src");

//        DataStream<ClickUpdateEvent> joinedStream = clickSrc.join(updateSrc)
//                .where(ClickEvent::getPage).equalTo(UpdateEvent::getPage)
//                .window(TumblingEventTimeWindows.of(Time.seconds(60)))
//                .allowedLateness(Time.seconds(20))
//                .apply(new ClickUpdateJoinFunction());

        OutputTag<ClickUpdateEvent> outputTag = new OutputTag<ClickUpdateEvent>("lateEvent", TypeInformation.of(ClickUpdateEvent.class));
//        OutputTag<PageStatistics> outputTag1 = new OutputTag<PageStatistics>("lateEvent", TypeInformation.of(PageStatistics.class));

        SingleOutputStreamOperator<PageStatistics> statsStream = clickSrc.map(value -> new ClickUpdateEvent(value, null))
                .keyBy(ClickUpdateEvent::getPage)
                .window(TumblingEventTimeWindows.of(Time.seconds(60)))
//                .allowedLateness(Time.seconds(20))
                .sideOutputLateData(outputTag)
                .process(new CountProcessWindowFunction());

        statsStream.sinkTo(statsSink);
        statsStream.getSideOutput(outputTag).sinkTo(lateSink);

        environment.execute();


        System.out.println( "Hello World!" );
    }

    private static Options getCliOptns(){
        Options options = new Options();

        options.addOption(
                Option.builder("kafka")
                        .hasArg(true).argName("bootstrap")
                        .desc("Kafka bootstrap server")
                        .build());

//        options.addOption(
//                Option.builder("pg")
//                        .hasArg(true).argName("guarantee")
//                        .desc("Processing guarantee")
//                        .build());

        return options;
    }
}
