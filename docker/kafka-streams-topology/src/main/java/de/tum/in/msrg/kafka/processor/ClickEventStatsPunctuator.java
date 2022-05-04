package de.tum.in.msrg.kafka.processor;

import de.tum.in.msrg.datamodel.ClickEvent;
import de.tum.in.msrg.datamodel.ClickEventStatistics;
import org.apache.kafka.streams.processor.Punctuator;
import org.apache.kafka.streams.processor.StateStore;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.internals.RocksDBWindowStore;

public class ClickEventStatsPunctuator implements Punctuator {

    private KeyValueStore<String, ClickEvent> stateStore;
    private ProcessorContext<String, ClickEventStatistics> context;
    public ClickEventStatsPunctuator(ProcessorContext<String, ClickEventStatistics> context, KeyValueStore<String, ClickEvent> stateStore){
        this.stateStore = stateStore;
        this.context = context;
    }
    @Override
    public void punctuate(long timestamp) {
        this.stateStore.
    }
}
