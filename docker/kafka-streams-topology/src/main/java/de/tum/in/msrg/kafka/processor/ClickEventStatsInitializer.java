package de.tum.in.msrg.kafka.processor;

import de.tum.in.msrg.datamodel.ClickEventStatistics;
import org.apache.kafka.streams.kstream.Initializer;

import java.time.Instant;
import java.util.Date;

public class ClickEventStatsInitializer implements Initializer<ClickEventStatistics> {
    @Override
    public ClickEventStatistics apply() {
        new ClickEventStatistics(Date.from(Instant.EPOCH),
                Date.from(Instant.EPOCH),
                "",
                0L);
        return null;
    }
}
