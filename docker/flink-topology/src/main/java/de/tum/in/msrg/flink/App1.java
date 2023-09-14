package de.tum.in.msrg.flink;

import de.tum.in.msrg.datamodel.ClickEvent;
import de.tum.in.msrg.datamodel.ClickUpdateEvent;
import de.tum.in.msrg.datamodel.PageStatistics;
import de.tum.in.msrg.datamodel.UpdateEvent;
import de.tum.in.msrg.flink.functions.ClickUpdateJoinFunction;
import de.tum.in.msrg.flink.functions.CountProcessWindowFunction;
import de.tum.in.msrg.flink.serialization.*;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.connector.base.DeliveryGuarantee;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.util.OutputTag;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Properties;

/**
 * Hello world!
 *
 */
public class App1
{
    private static final Logger LOGGER = LogManager.getLogger(App1.class);

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
                .build();

        KafkaSource<UpdateEvent> updateSource = KafkaSource.<UpdateEvent>builder()
                .setTopics("update")
                .setBootstrapServers(kafka)
                .setGroupId("updateReader")
                .setStartingOffsets(OffsetsInitializer.committedOffsets(OffsetResetStrategy.EARLIEST))
                .setValueOnlyDeserializer(new UpdateEventDeserializer())
                .build();


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
                .<ClickEvent>forMonotonousTimestamps()
                .withTimestampAssigner((element, recordTimestamp) -> element.getTimestamp().getTime());

        WatermarkStrategy<UpdateEvent> updateEventWatermarkStrategy = WatermarkStrategy
                .<UpdateEvent>forMonotonousTimestamps()
                .withTimestampAssigner((element, recordTimestamp) -> element.getTimestamp().getTime());



        DataStreamSource<ClickEvent> clickSrc = environment.fromSource(clickSource, clickEventWatermarkStrategy, "ClickEvent-src");
        DataStreamSource<UpdateEvent> updateSrc = environment.fromSource(updateSource, updateEventWatermarkStrategy, "UpdateEvent-src");

        OutputTag<ClickUpdateEvent> outputTag = new OutputTag<ClickUpdateEvent>("lateEvent", TypeInformation.of(ClickUpdateEvent.class));

        DataStream<ClickUpdateEvent> joinedStream = clickSrc.join(updateSrc)
                .where(ClickEvent::getPage).equalTo(UpdateEvent::getPage)
                .window(TumblingEventTimeWindows.of(Time.seconds(60)))
//                .allowedLateness(Time.seconds(20))
                .apply(new ClickUpdateJoinFunction());



        SingleOutputStreamOperator<PageStatistics> statsStream = joinedStream
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
