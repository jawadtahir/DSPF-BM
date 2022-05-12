package de.tum.in.msrg.kafka.processor;

import de.tum.in.msrg.datamodel.ClickEvent;
import de.tum.in.msrg.datamodel.ClickEventStatistics;
import org.apache.kafka.streams.kstream.Aggregator;
import org.apache.kafka.streams.kstream.Reducer;

public class ClickEventStatsAggregator implements Aggregator<String, ClickEvent, ClickEventStatistics> {
    @Override
    public ClickEventStatistics apply(String key, ClickEvent value, ClickEventStatistics aggregate) {
        ClickEventStatistics stats = aggregate;
        aggregate.setCount(aggregate.getCount()+1);
        if (aggregate.getPage().equals("")){
            aggregate.setPage(value.getPage());
        }
        return stats;
    }
}
