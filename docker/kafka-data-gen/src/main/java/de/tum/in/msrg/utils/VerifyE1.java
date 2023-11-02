package de.tum.in.msrg.utils;

import de.tum.in.msrg.common.PageTSKey;
import io.prometheus.client.Gauge;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class VerifyE1 implements Runnable {

    Map<PageTSKey, Map<Long, Boolean>> inputIdMap;
    Map<PageTSKey, Map<Long, Boolean>> processedMap;
    Gauge unprocessedEventsGauge;
    Gauge unprocessedOutputsGauge;

    private static final Logger LOGGER = LogManager.getLogger(VerifyE1.class);

    public VerifyE1(
            Map<PageTSKey, Map<Long, Boolean>> inputIdMap,
            Map<PageTSKey, Map<Long, Boolean>> processedMap,
            Gauge unprocessedEventsGauge,
            Gauge unprocessedOutputsGauge) {
        this.inputIdMap = inputIdMap;
        this.processedMap = processedMap;
        this.unprocessedEventsGauge = unprocessedEventsGauge;
        this.unprocessedOutputsGauge = unprocessedOutputsGauge;
    }



    @Override
    public void run() {

        LOGGER.debug("Starting E1 verifier");
        Long biggestTS = 0L;

        Map<PageTSKey, Long> unprocessedEventsMap = new ConcurrentHashMap<>();
        Map<String, Long> unprocessedEventsMap2 = new ConcurrentHashMap<>();
        for (Map.Entry<PageTSKey, Map<Long, Boolean>> entry : inputIdMap.entrySet()){
            LOGGER.debug(String.format("Processing %s key...", entry.getKey()));
            Map<Long, Boolean> expectedIds = entry.getValue();
            Map<Long, Boolean> processedIds = processedMap.getOrDefault(entry.getKey(), new ConcurrentHashMap<>());

            if (biggestTS < entry.getKey().getTS().getTime()){
                biggestTS = entry.getKey().getTS().getTime();
            }

            int expectedSize = expectedIds.size();
            int processedSize = processedIds.size();

            long unprocEventsPerKey = unprocessedEventsMap.getOrDefault(entry.getKey().getPage(), 0L);

            for (Long id : expectedIds.keySet()){
                if (!processedIds.containsKey(id)){
                    unprocEventsPerKey += 1;
                }
            }


            unprocessedEventsMap.put(entry.getKey(), unprocEventsPerKey);
        }
        LOGGER.debug(String.format("Biggest TS: %d", biggestTS));

        for (Map.Entry<PageTSKey, Long> entry : unprocessedEventsMap.entrySet()){
            LOGGER.debug(String.format("Processing %s key", entry.getKey()));
            if (entry.getKey().getTS().getTime() < biggestTS){
                Long unproc = unprocessedEventsMap2.getOrDefault(entry.getKey().getPage(), 0L);
                unproc += entry.getValue();
                unprocessedEventsMap2.put(entry.getKey().getPage(), unproc);

            }
        }

        LOGGER.debug("updating gauge...");
        unprocessedEventsMap2.forEach((s, aLong) -> unprocessedEventsGauge.labels(s).set(aLong));

        LOGGER.debug("Finished E1 verifier");
    }
}
