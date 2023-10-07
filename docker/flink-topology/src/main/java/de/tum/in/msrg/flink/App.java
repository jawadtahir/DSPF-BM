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
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.connector.kafka.source.KafkaSourceOptions;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.util.OutputTag;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;
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

        StreamExecutionEnvironment environment = StreamExecutionEnvironment.getExecutionEnvironment();


        Properties sinkProperties = new Properties();

        sinkProperties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka);
        sinkProperties.put(ProducerConfig.TRANSACTION_TIMEOUT_CONFIG, 30000);


        KafkaSource<ClickEvent> clickSource = KafkaSource.<ClickEvent>builder()
                .setTopics("click")
                .setBootstrapServers(kafka)
                .setGroupId("clickReader")
                .setStartingOffsets(OffsetsInitializer.committedOffsets(OffsetResetStrategy.EARLIEST))
                .setValueOnlyDeserializer(new ClickEventDeserializer())
//                .setProperty(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed")
                .build();

//        KafkaSource<UpdateEvent> updateSource = KafkaSource.<UpdateEvent>builder()
//                .setTopics("update")
//                .setBootstrapServers(kafka)
//                .setGroupId("updateReader")
//                .setStartingOffsets(OffsetsInitializer.committedOffsets(OffsetResetStrategy.EARLIEST))
//                .setValueOnlyDeserializer(new UpdateEventDeserializer())
//                .setProperty(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed")
//                .build();


        KafkaSink<PageStatistics> statsSink = KafkaSink.<PageStatistics>builder()
                .setBootstrapServers(kafka)
                .setKafkaProducerConfig(sinkProperties)
                .setDeliveryGuarantee(DeliveryGuarantee.EXACTLY_ONCE)
                .setTransactionalIdPrefix("statSink").
                setRecordSerializer(KafkaRecordSerializationSchema.builder()
                        .setTopic("output")
                        .setValueSerializationSchema(new PageStatisticsValueSerializer())
                        .setKeySerializationSchema(new PageStatisticsKeyValueSerializer())
                        .build())
                .build();

        KafkaSink<ClickUpdateEvent> lateSink = KafkaSink.<ClickUpdateEvent>builder()
                .setBootstrapServers(kafka)
                .setKafkaProducerConfig(sinkProperties)
                .setDeliveryGuarantee(DeliveryGuarantee.EXACTLY_ONCE)
                .setTransactionalIdPrefix("lateSink").
                setRecordSerializer(KafkaRecordSerializationSchema.builder()
                        .setTopic("lateOutput")
                        .setValueSerializationSchema(new ClickUpdateValueSerializer())
                        .setKeySerializationSchema(new ClickUpdateKeySerializer())
                        .build())
                .build();



        WatermarkStrategy<ClickEvent> clickEventWatermarkStrategy = WatermarkStrategy
                .<ClickEvent>forBoundedOutOfOrderness(Duration.ofMillis(200))
//                .withWatermarkAlignment("clickAligner", Duration.ofMillis(200))
                .withTimestampAssigner((element, recordTimestamp) -> element.getTimestamp().getTime());

//        WatermarkStrategy<UpdateEvent> updateEventWatermarkStrategy = WatermarkStrategy
//                .<UpdateEvent>forBoundedOutOfOrderness(Duration.ofMillis(200))
////                .withWatermarkAlignment("updateAligner", Duration.ofMillis(200))
//                .withTimestampAssigner((element, recordTimestamp) -> element.getTimestamp().getTime());



        DataStreamSource<ClickEvent> clickSrc = environment.fromSource(clickSource, clickEventWatermarkStrategy, "ClickEvent-src");
//        DataStreamSource<UpdateEvent> updateSrc = environment.fromSource(updateSource, updateEventWatermarkStrategy, "UpdateEvent-src");

        OutputTag<ClickUpdateEvent> outputTag = new OutputTag<ClickUpdateEvent>("lateEvent", TypeInformation.of(ClickUpdateEvent.class));

        SingleOutputStreamOperator<PageStatistics> statsStream = clickSrc.map(value -> new ClickUpdateEvent(value, null))
                .keyBy(ClickUpdateEvent::getPage)
                .window(TumblingEventTimeWindows.of(Time.seconds(60)))
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
