package de.tum.in.msrg.utils;

import de.tum.in.msrg.common.PageTSKey;
import io.prometheus.client.Gauge;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public class VerifyE1 implements Runnable {

    Map<PageTSKey, List<Long>> inputIdMap;
    Map<PageTSKey, List<Long>> processedMap;
    Gauge unprocessedEventsGauge;
    Gauge unprocessedOutputsGauge;

    private static final Logger LOGGER = LogManager.getLogger(VerifyE1.class);

    public VerifyE1(
            Map<PageTSKey, List<Long>> inputIdMap,
            Map<PageTSKey, List<Long>> processedMap,
            Gauge unprocessedEventsGauge,
            Gauge unprocessedOutputsGauge) {
        this.inputIdMap = inputIdMap;
        this.processedMap = processedMap;
        this.unprocessedEventsGauge = unprocessedEventsGauge;
        this.unprocessedOutputsGauge = unprocessedOutputsGauge;
    }

    @Override
    public void run() {
        Map<String, Long> unprocessedEventsMap = new HashMap<>();
        Map<String, Long> unprocessedOutputsMap = new HashMap<>();
        for (Map.Entry<PageTSKey, List<Long>> entry : inputIdMap.entrySet()){

            List<Long> expectedIds = entry.getValue();
            List<Long> processedIds = processedMap.getOrDefault(entry.getKey(), Collections.synchronizedList(new ArrayList<>()));

            int expectedSize = expectedIds.size();
            int processedSize = processedIds.size();

            long unprocEventsPerKey = unprocessedEventsMap.getOrDefault(entry.getKey().getPage(), 0L);
            long unprocOutputsPerKey = unprocessedOutputsMap.getOrDefault(entry.getKey().getPage(), 0L);

            if (expectedSize == processedSize){
                continue;
            } else {
                LOGGER.debug(String.format("Expected size: %d\nProcessed size: %d\nKey: %s", expectedSize, processedSize, entry.getKey()));
                unprocOutputsPerKey += 1;
                unprocEventsPerKey += expectedSize - processedSize;
            }

            unprocessedEventsMap.put(entry.getKey().getPage(), unprocEventsPerKey);
            unprocessedOutputsMap.put(entry.getKey().getPage(), unprocOutputsPerKey);
        }
        unprocessedEventsMap.forEach((s, aLong) -> unprocessedEventsGauge.labels(s).set(aLong));
        unprocessedOutputsMap.forEach((s, aLong) -> unprocessedOutputsGauge.labels(s).set(aLong));
    }
}
