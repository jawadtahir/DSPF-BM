package de.tum.in.msrg.flink;

import de.tum.in.msrg.datamodel.ClickEvent;
import de.tum.in.msrg.datamodel.ClickUpdateEvent;
import de.tum.in.msrg.datamodel.PageStatistics;
import de.tum.in.msrg.datamodel.UpdateEvent;
import de.tum.in.msrg.flink.connector.CustomKafkaSource;
import de.tum.in.msrg.flink.functions.ClickUpdateJoinFunction;
import de.tum.in.msrg.flink.functions.CountProcessWindowFunction;
import de.tum.in.msrg.flink.functions.hdfs.ClickInputFormat;
import de.tum.in.msrg.flink.functions.hdfs.PageStatPolicy;
import de.tum.in.msrg.flink.functions.hdfs.UpdateInputFormat;
import de.tum.in.msrg.flink.serialization.ClickEventDeserializer;
import de.tum.in.msrg.flink.serialization.PageStatisticsSerializer;
import de.tum.in.msrg.flink.serialization.UpdateEventDeserializer;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.serialization.SimpleStringEncoder;
import org.apache.flink.api.connector.sink2.Sink;
import org.apache.flink.api.connector.source.Source;
import org.apache.flink.configuration.ConfigConstants;
import org.apache.flink.configuration.ConfigOptions;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.CoreOptions;
import org.apache.flink.connector.base.DeliveryGuarantee;
import org.apache.flink.connector.file.sink.FileSink;
import org.apache.flink.connector.file.src.FileSource;
import org.apache.flink.connector.file.src.reader.TextLineInputFormat;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.KafkaSourceOptions;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.core.fs.Path;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.filesystem.bucketassigners.DateTimeBucketAssigner;
import org.apache.flink.streaming.api.functions.sink.filesystem.rollingpolicies.DefaultRollingPolicy;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.LocatedFileStatus;
import org.apache.hadoop.fs.RemoteIterator;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.protocol.types.Field;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Properties;

public class ClickCount {

    private static final String CLICK_DIR   = "/datagen/click/";
    private static final String UPDATE_DIR   = "/datagen/update/";
    private static final String OUTPUT_DIR   = "/output/";

    private static final String CLICK   = "click";
    private static final String UPDATE   = "update";
    private static final String OUTPUT   = "output";
    private static final String KAFKA   = "kafka";
    private static final String HDFS   = "hdfs";


    private static final Logger LOGGER = LogManager.getLogger(ClickCount.class);

    public static void main( String[] args ) throws Exception {

        DefaultParser parser = new DefaultParser();
        CommandLine cmdLine = parser.parse(getCliOptns(), args);

        String dataSource = cmdLine.getOptionValue("source");
        LOGGER.info(String.format("Data source is %s", dataSource));

        String bootstrapServer = cmdLine.getOptionValue("bootstrap");
        LOGGER.info(String.format("Bootstrap server is %s", bootstrapServer));

        Configuration configuration = new Configuration();
        configuration.set(CoreOptions.DEFAULT_FILESYSTEM_SCHEME, bootstrapServer);
        configuration.set(CoreOptions.DEFAULT_PARALLELISM, 1);


        StreamExecutionEnvironment environment = StreamExecutionEnvironment.getExecutionEnvironment(configuration);


        DataStreamSource<ClickEvent> clickSrc = (DataStreamSource<ClickEvent>) getStreamSource(environment, CLICK, dataSource, bootstrapServer);
        DataStreamSource<UpdateEvent> updateSrc = (DataStreamSource<UpdateEvent>) getStreamSource(environment, UPDATE, dataSource, bootstrapServer);

        DataStream<ClickUpdateEvent> joinedStream = clickSrc.join(updateSrc)
                .where(ClickEvent::getPage).equalTo(UpdateEvent::getPage)
                .window(TumblingEventTimeWindows.of(Time.seconds(60)))
                .apply(new ClickUpdateJoinFunction());

        DataStream<PageStatistics> statsStream = joinedStream
                .keyBy(ClickUpdateEvent::getPage)
                .window(TumblingEventTimeWindows.of(Time.seconds(60)))
                .process(new CountProcessWindowFunction());

        statsStream.sinkTo(getSink(dataSource, bootstrapServer));

        environment.execute("ClickCountJob");



    }

    private static Sink<PageStatistics> getSink(String dataSource, String bootstrapServer) {

        Sink<PageStatistics> sink = null;

        if (dataSource.equalsIgnoreCase(KAFKA)){

            sink = getKafkaSink(bootstrapServer);
        } else if (dataSource.equalsIgnoreCase(HDFS)) {

            sink = getHDFSSink(bootstrapServer);
        }

        return sink;

    }

    private static Sink getHDFSSink(String bootstrapServer) {
        Sink retVal;

        retVal = FileSink.forRowFormat(new Path(bootstrapServer+Path.SEPARATOR+OUTPUT_DIR), new SimpleStringEncoder<>("UTF-8"))
                .withBucketAssigner(new DateTimeBucketAssigner<>("yyyy-MM-dd"))
                .withRollingPolicy(new PageStatPolicy<>())
                .build();

        return retVal;
    }

    private static Sink<PageStatistics> getKafkaSink(String bootstrapServer) {

        Properties sinkProperties = new Properties();

        sinkProperties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer);
        sinkProperties.put(ProducerConfig.TRANSACTION_TIMEOUT_CONFIG, 3000);

        return KafkaSink.<PageStatistics>builder()
                .setBootstrapServers(bootstrapServer)
                .setKafkaProducerConfig(sinkProperties)
                .setDeliverGuarantee(DeliveryGuarantee.EXACTLY_ONCE)
                .setTransactionalIdPrefix("kafka-sink").
                setRecordSerializer(KafkaRecordSerializationSchema.builder()
                        .setTopic("output")
                        .setValueSerializationSchema(new PageStatisticsSerializer())
                        .build())
                .build();
    }

    /**
     * get appropriate {@link DataStreamSource} based on {@code stream} and {@code dataSource}
     * @param environment
     * @param stream
     * @param dataSource
     * @param bootstrapServer
     * @return
     */
    private static DataStreamSource getStreamSource (StreamExecutionEnvironment environment,
                                                    String stream,
                                                    String dataSource,
                                                    String bootstrapServer) throws URISyntaxException, IOException {
        DataStreamSource retVal = null;
        // Get source click source
        Source source = getSources(stream, dataSource, bootstrapServer);

        if (stream.equalsIgnoreCase(CLICK)){

            // Create watermark strategy for click stream
            WatermarkStrategy<ClickEvent> clickEventWatermarkStrategy = WatermarkStrategy
                    .<ClickEvent>forBoundedOutOfOrderness(Duration.ofMillis(1))
                    .withIdleness(Duration.ofMillis(1))
                    .withTimestampAssigner((element, recordTimestamp) -> element.getTimestamp().getTime());

            retVal = environment.fromSource(source, clickEventWatermarkStrategy, "ClickEvent-src");
        } else if (stream.equalsIgnoreCase(UPDATE)){
            // Create watermark strategy for update stream
            WatermarkStrategy<UpdateEvent> updateEventWatermarkStrategy = WatermarkStrategy
                    .<UpdateEvent>forBoundedOutOfOrderness(Duration.ofMillis(1))
                    .withIdleness(Duration.ofMillis(1))
                    .withTimestampAssigner((element, recordTimestamp) -> element.getTimestamp().getTime());

            retVal = environment.fromSource(source, updateEventWatermarkStrategy, "UpdateEvent-src");
        }


        return retVal;
    }

    private static Source getSources(String stream, String dataSource, String bootstrapServer) throws URISyntaxException, IOException {
        Source retVal = null;

        if (stream.equalsIgnoreCase(CLICK)){
            if (dataSource.equalsIgnoreCase(KAFKA)){

                retVal = getKafkaSource(stream, bootstrapServer);

            } else if (dataSource.equalsIgnoreCase(HDFS)){

                retVal = getHDFSSource(stream, bootstrapServer);
            }
        } else if (stream.equalsIgnoreCase(UPDATE)){
            if (dataSource.equalsIgnoreCase(KAFKA)){

                retVal = getKafkaSource(stream, bootstrapServer);

            } else if (dataSource.equalsIgnoreCase(HDFS)) {

                retVal = getHDFSSource(stream, bootstrapServer);
            }

        }
        return retVal;
    }

    private static CustomKafkaSource getKafkaSource(String stream, String bootstrapServer){
        CustomKafkaSource retVal = null;

        Properties srcProperties = new Properties();

        srcProperties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer);
        srcProperties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        srcProperties.put(ConsumerConfig.GROUP_ID_CONFIG, "flink-topology");

        if (stream.equalsIgnoreCase(CLICK)){
            // create kafka file source
            retVal = CustomKafkaSource.<ClickEvent>builder()
                    .setTopics(stream)
                    .setBootstrapServers(bootstrapServer)
                    .setStartingOffsets(OffsetsInitializer.earliest())
                    .setProperties(srcProperties)
                    .setProperty(KafkaSourceOptions.CLIENT_ID_PREFIX.key(), "flink-topology")
                    .setValueOnlyDeserializer(new ClickEventDeserializer())
                    .build();
        } else  if (stream.equalsIgnoreCase(UPDATE)){
            // create kafka file source
            retVal = CustomKafkaSource.<UpdateEvent>builder()
                    .setTopics(stream)
                    .setBootstrapServers(bootstrapServer)
                    .setStartingOffsets(OffsetsInitializer.earliest())
                    .setProperties(srcProperties)
                    .setProperty(KafkaSourceOptions.CLIENT_ID_PREFIX.key(), "flink-topology")
                    .setValueOnlyDeserializer(new UpdateEventDeserializer())
                    .build();
        }




        return retVal;
    }

    private static FileSource getHDFSSource(String stream, String bootstrapServer) throws URISyntaxException, IOException {
        FileSource retVal = null;

        org.apache.hadoop.conf.Configuration configuration = new org.apache.hadoop.conf.Configuration();
        configuration.set("fs.defaultFS", bootstrapServer);

        try (FileSystem fs = FileSystem.get(new URI(bootstrapServer), configuration)){
            clearDataDir(fs, new org.apache.hadoop.fs.Path(CLICK_DIR));
            clearDataDir(fs, new org.apache.hadoop.fs.Path(UPDATE_DIR));
            clearDir(fs, new org.apache.hadoop.fs.Path(OUTPUT_DIR));
        }


        if (stream.equalsIgnoreCase(CLICK)){
            retVal = FileSource.forRecordStreamFormat(
                            new ClickInputFormat(),
                            new Path(bootstrapServer+Path.SEPARATOR+CLICK_DIR))
                    .monitorContinuously(Duration.of(10, ChronoUnit.SECONDS))
                    .build();
        } else {
            retVal = FileSource
                    .forRecordStreamFormat(
                            new UpdateInputFormat(),
                            new Path(bootstrapServer+Path.SEPARATOR+UPDATE_DIR))
                    .monitorContinuously(Duration.of(1000, ChronoUnit.MILLIS))
                    .build();
        }
        return retVal;
    }

    private static void clearDir(FileSystem fs, org.apache.hadoop.fs.Path basePath) throws IOException {
        RemoteIterator<LocatedFileStatus> iter = fs.listLocatedStatus(basePath);
        while (iter.hasNext()){
            LocatedFileStatus fileStatus = iter.next();
            fs.delete(fileStatus.getPath(), true);
        }
    }

    private static void clearDataDir (FileSystem fs, org.apache.hadoop.fs.Path basePath) throws IOException {
        RemoteIterator<LocatedFileStatus> iterator = fs.listLocatedStatus(basePath);
        while (iterator.hasNext()){
            LocatedFileStatus fileStatus = iterator.next();
            if (fileStatus.isDirectory()){
                clearDataDir(fs, fileStatus.getPath());
            } else {
                fs.delete(fileStatus.getPath(), false);
            }
        }
    }

    private static WatermarkStrategy getWatermarkStrategy(String stream){
        WatermarkStrategy retVal = null;
        if (stream.equalsIgnoreCase(CLICK)){
            retVal = WatermarkStrategy
                    .<ClickEvent>forBoundedOutOfOrderness(Duration.ofMillis(1))
                    .withIdleness(Duration.ofMillis(1))
                    .withTimestampAssigner((element, recordTimestamp) -> element.getTimestamp().getTime());
        } else if (stream.equalsIgnoreCase(UPDATE)) {

            retVal = WatermarkStrategy
                    .<UpdateEvent>forBoundedOutOfOrderness(Duration.ofMillis(1))
                    .withIdleness(Duration.ofMillis(1))
                    .withTimestampAssigner((element, recordTimestamp) -> element.getTimestamp().getTime());
        }

        return retVal;
    }


    private static Options getCliOptns(){
        Options options = new Options();

        options.addOption(
                Option.builder("source")
                        .hasArg(true).argName("data source")
                        .desc("data source, one of [hdfs, kafka]")
                        .required()
                        .build());

        options.addOption(
                Option.builder("bootstrap")
                        .hasArg(true).argName("bootstrap server")
                        .desc("Data source bootstrap server")
                        .required()
                        .build()
        );

//        options.addOption(
//                Option.builder("pg")
//                        .hasArg(true).argName("guarantee")
//                        .desc("Processing guarantee")
//                        .build());

        return options;
    }
}
