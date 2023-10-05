package de.tum.in.msrg.datagen;

import de.tum.in.msrg.common.PageTSKey;
import de.tum.in.msrg.datamodel.ClickEvent;
import io.prometheus.client.Counter;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.RecordMetadata;

import java.util.*;

public class SendCallback implements Callback {

    private ClickEvent clickEvent;
    private Map<PageTSKey, Date> inputTimeMap;
    private Map<PageTSKey, List<Long>> inputIdMap;
    private Counter generatedCounter;
    private Counter expectedOutputs;

    public SendCallback(
            ClickEvent clickEvent,
            Map<PageTSKey, Date> inputTimeMap,
            Map<PageTSKey, List<Long>> inputIdMap,
            Counter generatedCounter,
            Counter expectedOutputs) {
        this.clickEvent = clickEvent;
        this.inputTimeMap = inputTimeMap;
        this.inputIdMap = inputIdMap;
        this.generatedCounter = generatedCounter;
        this.expectedOutputs = expectedOutputs;
    }

    @Override
    public void onCompletion(RecordMetadata metadata, Exception exception) {

        PageTSKey key = new PageTSKey(clickEvent.getPage(), clickEvent.getTimestamp());
        long id = clickEvent.getId();



        Date ingestionTime = new Date(metadata.timestamp());

        if (!inputTimeMap.containsKey(key)){
            inputTimeMap.put(key, ingestionTime);
            expectedOutputs.labels(key.getPage()).inc();
        }

        List<Long> previousIds = inputIdMap.getOrDefault(key, Collections.synchronizedList(new ArrayList<>()));

        previousIds.add(id);
        inputIdMap.put(key, previousIds);

        generatedCounter.labels(key.getPage()).inc();
    }
}
