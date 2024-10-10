package de.tum.in.msrg.datagen;

import de.tum.in.msrg.common.PageTSKey;
import de.tum.in.msrg.datamodel.UpdateEvent;
import io.prometheus.client.Counter;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.RecordMetadata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UpdateCallback implements Callback {

    private UpdateEvent updateEvent;
    private Map<PageTSKey, Map<Long, Boolean>> inputIdMap;
    private Counter generatedCounter;

    public UpdateCallback(UpdateEvent updateEvent, Map<PageTSKey, Map<Long, Boolean>> inputIdMap, Counter generatedCounter) {
        this.updateEvent = updateEvent;
        this.inputIdMap = inputIdMap;
        this.generatedCounter = generatedCounter;
    }

    @Override
    public void onCompletion(RecordMetadata metadata, Exception exception) {
        PageTSKey key = new PageTSKey(updateEvent.getPage(), updateEvent.getTimestamp());
        Long id = updateEvent.getId();

        Map<Long, Boolean> previousIds = inputIdMap.getOrDefault(key, new ConcurrentHashMap<>());

        previousIds.put(id, true);
        inputIdMap.put(key, previousIds);

        generatedCounter.labels(key.getPage()).inc();

    }
}
