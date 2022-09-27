package de.tum.in.msrg.flink;

import de.tum.in.msrg.datamodel.ClickEvent;
import de.tum.in.msrg.datamodel.ClickUpdateEvent;
import de.tum.in.msrg.datamodel.PageStatistics;
import de.tum.in.msrg.datamodel.UpdateEvent;
import de.tum.in.msrg.flink.connector.CustomKafkaSource;
import de.tum.in.msrg.flink.functions.ClickUpdateJoinFunction;
import de.tum.in.msrg.flink.functions.CountProcessWindowFunction;
import de.tum.in.msrg.flink.serialization.ClickEventDeserializer;
import de.tum.in.msrg.flink.serialization.PageStatisticsSerializer;
import de.tum.in.msrg.flink.serialization.UpdateEventDeserializer;
import org.apache.commons.cli.*;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.connector.base.DeliveryGuarantee;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.connector.kafka.source.KafkaSourceOptions;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;

import java.time.Duration;
import java.util.Properties;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args ) throws Exception {

        DefaultParser parser = new DefaultParser();
        CommandLine cmdLine = parser.parse(getCliOptns(), args);

        String kafka = cmdLine.getOptionValue("kafka", "kafka1:9092");
//        String pg = cmdLine.getOptionValue("pg", "exactly once");

        StreamExecutionEnvironment environment = StreamExecutionEnvironment.getExecutionEnvironment();


        Properties srcProperties = new Properties();

        srcProperties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka);
        srcProperties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        srcProperties.put(ConsumerConfig.GROUP_ID_CONFIG, "flink-topology");


        Properties sinkProperties = new Properties();

        sinkProperties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka);
        sinkProperties.put(ProducerConfig.TRANSACTION_TIMEOUT_CONFIG, 3000);




//        CustomKafkaSource<ClickEvent> clickSource = CustomKafkaSource.<ClickEvent>builder()
        KafkaSource<ClickEvent> clickSource = KafkaSource.<ClickEvent>builder()
                .setTopics("click")
                .setBootstrapServers(kafka)
                .setStartingOffsets(OffsetsInitializer.earliest())
                .setProperties(srcProperties)
                .setProperty(KafkaSourceOptions.CLIENT_ID_PREFIX.key(), "flink-topology")
                .setValueOnlyDeserializer(new ClickEventDeserializer())
                .build();

        KafkaSource<UpdateEvent> updateSource = KafkaSource.<UpdateEvent>builder()
                .setTopics("update")
                .setBootstrapServers(kafka)
                .setProperties(srcProperties)
                .setProperty(KafkaSourceOptions.CLIENT_ID_PREFIX.key(), "flink-topology")
                .setValueOnlyDeserializer(new UpdateEventDeserializer())
                .build();

        KafkaSink<PageStatistics> statsSink = KafkaSink.<PageStatistics>builder()
                .setBootstrapServers(kafka)
                .setKafkaProducerConfig(sinkProperties)
                .setDeliverGuarantee(DeliveryGuarantee.EXACTLY_ONCE)
                .setTransactionalIdPrefix("kafka-sink").
                setRecordSerializer(KafkaRecordSerializationSchema.builder()
                        .setTopic("output")
                        .setValueSerializationSchema(new PageStatisticsSerializer())
                        .build())
                .build();

        WatermarkStrategy<ClickEvent> clickEventWatermarkStrategy = WatermarkStrategy
                .<ClickEvent>forBoundedOutOfOrderness(Duration.ofMillis(200))
                .withIdleness(Duration.ofMillis(200))
                .withTimestampAssigner((element, recordTimestamp) -> element.getTimestamp().getTime());

        WatermarkStrategy<UpdateEvent> updateEventWatermarkStrategy = WatermarkStrategy
                .<UpdateEvent>forBoundedOutOfOrderness(Duration.ofMillis(200))
                .withIdleness(Duration.ofMillis(200))
                .withTimestampAssigner((element, recordTimestamp) -> element.getTimestamp().getTime());



        environment.enableCheckpointing(3000, CheckpointingMode.AT_LEAST_ONCE);
        DataStreamSource<ClickEvent> clickSrc = environment.fromSource(clickSource, clickEventWatermarkStrategy, "ClickEvent-src");
        DataStreamSource<UpdateEvent> updateSrc = environment.fromSource(updateSource, updateEventWatermarkStrategy, "UpdateEvent-src");

        DataStream<ClickUpdateEvent> joinedStream = clickSrc.join(updateSrc)
                .where(ClickEvent::getPage).equalTo(UpdateEvent::getPage)
                .window(TumblingEventTimeWindows.of(Time.seconds(60)))
                .apply(new ClickUpdateJoinFunction());

        DataStream<PageStatistics> statsStream = joinedStream
                .keyBy(ClickUpdateEvent::getPage)
                .window(TumblingEventTimeWindows.of(Time.seconds(60)))
                .process(new CountProcessWindowFunction());

        statsStream.sinkTo(statsSink);

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
