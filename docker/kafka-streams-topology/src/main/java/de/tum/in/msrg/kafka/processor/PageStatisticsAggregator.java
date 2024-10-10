package de.tum.in.msrg.kafka.processor;

import de.tum.in.msrg.datamodel.ClickUpdateEvent;
import de.tum.in.msrg.datamodel.PageStatistics;
import org.apache.kafka.streams.kstream.Aggregator;

public class PageStatisticsAggregator implements Aggregator<byte[], ClickUpdateEvent, PageStatistics> {
    @Override
    public PageStatistics apply(byte[] key, ClickUpdateEvent value, PageStatistics aggregate) {
        if (value != null) {
            aggregate.setPage(value.getPage());
            if (!aggregate.getClickIds().contains(value.getClickId())) {
                aggregate.getClickIds().add(value.getClickId());
            }
            if (!aggregate.getUpdateIds().contains(value.getUpdateId()) && value.getUpdateId() != 0) {
                aggregate.getUpdateIds().add(value.getUpdateId());
            }
        }

        return aggregate;
    }
}
