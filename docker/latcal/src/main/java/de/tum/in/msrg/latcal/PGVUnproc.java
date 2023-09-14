package de.tum.in.msrg.latcal;

import io.prometheus.client.Gauge;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public class PGVUnproc implements Runnable {

    private final Map<PageTSKey, List<Long>> expectedOutputs;
    private final Map<PageTSKey, List<Long>> processedOutputs;
    private final int numEventsPerWindow;

    private static final Logger LOGGER = LogManager.getLogger(PGVUnproc.class);

    public PGVUnproc(Map<PageTSKey, List<Long>> expectedOutputs, Map<PageTSKey, List<Long>> processedOutputs, int numEventsPerWindow) {
        this.expectedOutputs = expectedOutputs;
        this.processedOutputs = processedOutputs;
        this.numEventsPerWindow = numEventsPerWindow;
    }

    @Override
    public void run() {
        Gauge unprocessedGauge = Gauge.build("de_tum_in_msrg_pgv_unprocessed", "Unprocessed events").labelNames("key").register();
        while (true){
            LOGGER.debug("Calculating unprocessed events...");
//                    Map<String, Long> keyWindowCountMap = new HashMap<String, Long>();
            Map<String, Long> keyUnprocWindowCountMap = new HashMap<String, Long>();
            Set<Map.Entry<PageTSKey, List<Long>>> entries = expectedOutputs.entrySet();
//                 System.out.printf("The size of the map is %d%n \r\n", entries.size());
            for (Map.Entry<PageTSKey, List<Long>> entry : entries){
                List<Long> expectedEvents = new ArrayList<>(entry.getValue());
                List<Long> processedEvents = processedOutputs.get(entry.getKey());
                Long unprocCountPerKey = keyUnprocWindowCountMap.getOrDefault(entry.getKey().getPage(), 0L);

                // Ensure we are only counting complete windows
                if (expectedEvents.size() == this.numEventsPerWindow && processedEvents != null){
                    expectedEvents.removeAll(processedEvents);
                    unprocCountPerKey += expectedEvents.size();
                }
                keyUnprocWindowCountMap.put(entry.getKey().getPage(), unprocCountPerKey);

            }

            keyUnprocWindowCountMap.forEach((s, aLong) -> unprocessedGauge.labels(s).set(aLong));
        }

    }
}
