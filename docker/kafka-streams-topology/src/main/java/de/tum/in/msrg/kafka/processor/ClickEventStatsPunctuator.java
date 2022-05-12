package de.tum.in.msrg.kafka.processor;

import de.tum.in.msrg.datamodel.ClickEvent;
import de.tum.in.msrg.datamodel.ClickEventStatistics;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.Windowed;
import org.apache.kafka.streams.processor.Punctuator;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.Record;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.WindowStore;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ClickEventStatsPunctuator implements Punctuator {

    private WindowStore<String, ClickEvent> stateStore;
    private ProcessorContext<String, ClickEventStatistics> context;
    public ClickEventStatsPunctuator(ProcessorContext<String, ClickEventStatistics> context, WindowStore<String, ClickEvent> stateStore){
        this.stateStore = stateStore;
        this.context = context;
    }
    @Override
    public void punctuate(long timestamp) {
//        this.stateStore
        Instant windowEnd = Instant.ofEpochMilli(timestamp);
        Instant windowStart = windowEnd.minus(1L, ChronoUnit.MINUTES);
        Map<String, ClickEventStatistics> statsMap = new HashMap<>();
        try (KeyValueIterator<Windowed<String>, ClickEvent> iter = this.stateStore.fetchAll(windowStart, windowEnd)) {
            while (iter.hasNext()){
                KeyValue<Windowed<String>, ClickEvent> kv = iter.next();
                ClickEvent event = kv.value;
                ClickEventStatistics keyStats = statsMap.getOrDefault(
                        event.getPage(),
                        new ClickEventStatistics(
                                Date.from(windowStart),
                                Date.from(windowEnd),
                                event.getCreationTimestamp(),
                                event.getPage(),
                                0L));
                keyStats.setCount(keyStats.getCount()+1);
                if (keyStats.getFirstMsgTS().after( event.getCreationTimestamp())){
                    keyStats.setFirstMsgTS(event.getCreationTimestamp());
                }
                statsMap.put(event.getPage(), keyStats);
            }
        }
        statsMap.forEach((s, clickEventStatistics) -> {
            this.context.forward(new Record<>(s, clickEventStatistics,timestamp ));
        });
    }
}
