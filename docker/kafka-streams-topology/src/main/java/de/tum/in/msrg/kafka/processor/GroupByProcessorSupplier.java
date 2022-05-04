package de.tum.in.msrg.kafka.processor;

import de.tum.in.msrg.datamodel.ClickEvent;
import de.tum.in.msrg.datamodel.ClickEventStatistics;
import org.apache.kafka.streams.processor.api.Processor;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.ProcessorSupplier;
import org.apache.kafka.streams.processor.api.Record;

public class GroupByProcessorSupplier implements Processor<String, ClickEvent, String, ClickEventStatistics> {
    private ProcessorContext<String, ClickEventStatistics> context;
    @Override
    public void init(ProcessorContext<String, ClickEventStatistics> context) {
        Processor.super.init(context);
        this.context = context;
        this.context.getStateStore("");

    }

    @Override
    public void process(Record<String, ClickEvent> record) {

    }

    @Override
    public void close() {
        Processor.super.close();
    }
}
