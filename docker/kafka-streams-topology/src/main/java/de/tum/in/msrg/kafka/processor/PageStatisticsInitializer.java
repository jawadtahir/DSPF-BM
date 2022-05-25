package de.tum.in.msrg.kafka.processor;

import de.tum.in.msrg.datamodel.PageStatistics;
import org.apache.kafka.streams.kstream.Initializer;

public class    PageStatisticsInitializer implements Initializer<PageStatistics> {
    @Override
    public PageStatistics apply() {
        return new PageStatistics();
    }
}
