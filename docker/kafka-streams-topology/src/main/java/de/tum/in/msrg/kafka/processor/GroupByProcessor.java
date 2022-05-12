package de.tum.in.msrg.kafka.processor;

import de.tum.in.msrg.datamodel.ClickEvent;
import de.tum.in.msrg.datamodel.ClickEventStatistics;
import org.apache.kafka.streams.processor.PunctuationType;
import org.apache.kafka.streams.processor.api.Processor;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.ProcessorSupplier;
import org.apache.kafka.streams.processor.api.Record;
import org.apache.kafka.streams.state.WindowStore;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalField;
import java.util.List;

public class GroupByProcessor implements Processor<String, ClickEvent, String, ClickEventStatistics> {
    private ProcessorContext<String, ClickEventStatistics> context;
    private WindowStore<String, ClickEvent> stateStore;
    @Override
    public void init(ProcessorContext<String, ClickEventStatistics> context) {
        Processor.super.init(context);
        this.context = context;
        this.stateStore = this.context.getStateStore("windowed-groupby-store");
        this.context.schedule(
                Duration.of(60, ChronoUnit.SECONDS),
                PunctuationType.STREAM_TIME,
                new ClickEventStatsPunctuator(this.context, this.stateStore) );
    }

    @Override
    public void process(Record<String, ClickEvent> record) {
//        Instant windowStartInstant = record.value().getTimestamp().toInstant().with(ChronoField.SECOND_OF_MINUTE, 0L);
        String newKey = record.value().getPage();
        this.stateStore.put(newKey, record.value(), record.value().getTimestamp().getTime());
        this.context.commit();
    }

    @Override
    public void close() {
        Processor.super.close();
    }
}
