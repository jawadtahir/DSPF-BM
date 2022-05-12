package de.tum.in.msrg.kafka.processor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.tum.in.msrg.datamodel.ClickEvent;
import org.apache.kafka.streams.processor.api.Processor;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.ProcessorSupplier;
import org.apache.kafka.streams.processor.api.Record;

import java.util.Date;

public class ClickEventProcessorSupplier implements ProcessorSupplier<String, String, String, ClickEvent> {
    @Override
    public Processor<String, String, String, ClickEvent> get() {
        return new ClickEventProcessor();
    }


    class ClickEventProcessor implements Processor<String, String, String, ClickEvent>{
        private ProcessorContext<String, ClickEvent> context;
        private final ObjectMapper mapper = new ObjectMapper();

        @Override
        public void init(ProcessorContext<String, ClickEvent> context) {
            Processor.super.init(context);
            this.context = context;
        }

        @Override
        public void process(Record<String, String> record) {
            try {
                ClickEvent event = mapper.readValue(record.value(), ClickEvent.class);
                event.setCreationTimestamp(new Date(record.timestamp()));
                this.context.forward(record.withValue(event));
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void close() {
            Processor.super.close();
        }
    }
}
