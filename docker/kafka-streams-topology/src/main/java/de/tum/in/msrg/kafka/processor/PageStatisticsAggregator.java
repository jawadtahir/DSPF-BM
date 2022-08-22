package de.tum.in.msrg.kafka.processor;

import de.tum.in.msrg.datamodel.ClickUpdateEvent;
import de.tum.in.msrg.datamodel.PageStatistics;
import org.apache.kafka.streams.kstream.Aggregator;

public class PageStatisticsAggregator implements Aggregator<String, ClickUpdateEvent, PageStatistics> {
    @Override
    public PageStatistics apply(String key, ClickUpdateEvent value, PageStatistics aggregate) {
        aggregate.setPage(value.getPage());
        aggregate.getIds().add(value.getId());
        aggregate.setCount(aggregate.getCount()+1);

        if (!value.getUpdateTimestamp().equals(aggregate.getLastUpdateTS())){
            aggregate.setLastUpdateTS(value.getUpdateTimestamp());
            aggregate.setUpdateCount(aggregate.getUpdateCount()+1);
        }

        return aggregate;
    }
}
