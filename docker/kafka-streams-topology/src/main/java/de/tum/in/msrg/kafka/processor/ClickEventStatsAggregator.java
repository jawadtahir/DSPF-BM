package de.tum.in.msrg.kafka.processor;

import de.tum.in.msrg.datamodel.ClickEvent;
import de.tum.in.msrg.datamodel.ClickEventStatistics;
import org.apache.kafka.streams.kstream.Aggregator;
import org.apache.kafka.streams.kstream.Reducer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ClickEventStatsAggregator implements Aggregator<String, ClickEvent, ClickEventStatistics> {

    private static final Logger LOGGER = LogManager.getLogger(ClickEventStatsAggregator.class);

    @Override
    public ClickEventStatistics apply(String key, ClickEvent value, ClickEventStatistics aggregate) {
        ClickEventStatistics stats = aggregate;
        aggregate.setCount(aggregate.getCount()+1);
        if (aggregate.getPage().equals("")){
            aggregate.setPage(value.getPage());
        }

        LOGGER.debug(stats.toString());
        return stats;
    }
}
