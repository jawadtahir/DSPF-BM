package de.tum.in.msrg.datagen.adapter;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Properties;

public class KafkaAdapter implements Adapter{

    private KafkaProducer<String, String> kafkaProducer;
    private Properties properties;

    private static final Logger LOGGER = LogManager.getLogger(KafkaAdapter.class);

    public KafkaAdapter(Properties properties){
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                kafkaProducer.close();
            } catch (Exception e){
                e.printStackTrace();
            }
        }));

        this.properties = properties;
        kafkaProducer = new KafkaProducer<String, String>(properties);


    }

    @Override
    public void send(String stream, String key, String id, String event) {
        ProducerRecord<String, String> record = new ProducerRecord<>(stream, key, event);
        kafkaProducer.send(record);

    }

    @Override
    public void flush() {
        kafkaProducer.flush();
    }
}
