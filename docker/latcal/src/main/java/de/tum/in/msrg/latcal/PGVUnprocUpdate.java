package de.tum.in.msrg.latcal;

import io.prometheus.client.Gauge;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

public class PGVUnprocUpdate {
    private final Map<PageTSKey, Integer> unprocessedEventsCount;
    private final Gauge unprocGauge;

    private static final Logger LOGGER = LogManager.getLogger(PGVUnprocUpdate.class);

    public PGVUnprocUpdate(Map<PageTSKey, Integer> unprocessedEventsCount, Gauge unprocGauge){
        this.unprocessedEventsCount = unprocessedEventsCount;
        this.unprocGauge = unprocGauge;
    }

    protected void update () {
//        LOGGER.info("Executing update thread...");
        Map<String, Integer> unprocEventsCountPerKey = new HashMap<>();
        this.unprocessedEventsCount.forEach((pageTSKey, integer) -> {
            Integer prvCount = unprocEventsCountPerKey.getOrDefault(pageTSKey.getPage(), 0);
            prvCount += integer;
            if (prvCount != 0){
                LOGGER.info(String.format("****************\nFound unprocessed events for: %s\nCount: %d\n****************", pageTSKey,prvCount));
            }
            unprocEventsCountPerKey.put(pageTSKey.getPage(), prvCount);
        });


        unprocEventsCountPerKey.forEach((s, aLong) -> unprocGauge.labels(s).set(aLong));
    }
}
