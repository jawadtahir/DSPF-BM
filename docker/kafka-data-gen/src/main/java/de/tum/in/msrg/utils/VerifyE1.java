package de.tum.in.msrg.utils;

import de.tum.in.msrg.common.PageTSKey;
import io.prometheus.client.Gauge;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class VerifyE1 implements Runnable {

    Map<PageTSKey, List<Long>> inputIdMap;
    Map<PageTSKey, Map<Long, Long>> processedMap;
    int eventsPerWindow;
    AtomicLong largestWatermark;
    Gauge unprocessedGauge;

    private static final Logger LOGGER = LogManager.getLogger(VerifyE1.class);

    public VerifyE1(Map<PageTSKey, List<Long>> inputIdMap, Map<PageTSKey, Map<Long, Long>> processedMap, int eventsPerWindow, AtomicLong largestWatermark, Gauge unprocessedGauge) {
        this.inputIdMap = inputIdMap;
        this.processedMap = processedMap;
        this.eventsPerWindow = eventsPerWindow;
        this.largestWatermark = largestWatermark;
        this.unprocessedGauge = unprocessedGauge;
    }

    @Override
    public void run() {
        Map<String, Long> unprocessedMap = new HashMap<>();
        for (Map.Entry<PageTSKey, List<Long>> entry : inputIdMap.entrySet()){
            long watermark = entry.getKey().getTS().getTime();
            long largestWM = largestWatermark.getOpaque();
            List<Long> expectedIds = entry.getValue();
            Map<Long, Long> processedIds = processedMap.get(entry.getKey());

            int expectedSize = 0;
            int processedSize = 0;

            expectedSize = expectedIds.size();
            if (processedIds != null){
                processedSize = processedIds.keySet().size();
            }

            long unprocPerKey = unprocessedMap.getOrDefault(entry.getKey().getPage(), 0L);
            if (entry.getValue().size() == eventsPerWindow) {

                unprocPerKey += expectedSize - processedSize;
            }
//            }else {
//                LOGGER.info(String.format("ASSERT!! \nCurr WM: %d\nLarge WM: %d", watermark, largestWM));
//            }
            unprocessedMap.put(entry.getKey().getPage(), unprocPerKey);
        }
        unprocessedMap.forEach((s, aLong) -> unprocessedGauge.labels(s).set(aLong));
    }
}
