package de.tum.in.msrg.datagen;

import de.tum.in.msrg.common.Constants;
import de.tum.in.msrg.datamodel.ClickEvent;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ClickDataset {


    private final Map<String, Long> nextTimestampPerKey;
    private int nextPageIndex;
    private IDGenerator id;
    protected int eventPerWindow;

    ClickDataset(int eventPerWindow) {
        nextTimestampPerKey = new HashMap<>();
        nextPageIndex = 0;
        this.eventPerWindow = eventPerWindow;
        id = IDGenerator.getInstance();
    }


    ClickEvent next() {
        String page = nextPage();
        Date nextTimestamp = nextTimestamp(page);
        return new ClickEvent(id.getId(), nextTimestamp, page);
    }

    private Date nextTimestamp(String page) {
        long nextTimestamp = nextTimestampPerKey.getOrDefault(page, 1L);
//			nextTimestampPerKey.put(page, nextTimestamp + WINDOW_SIZE.toMilliseconds() / EVENTS_PER_WINDOW);
        nextTimestampPerKey.put(page, nextTimestamp + (Constants.WINDOW_SIZE.toMillis()  / this.eventPerWindow ) );
        return new Date(nextTimestamp);
    }

    private String nextPage() {
        String nextPage = Constants.PAGES.get(nextPageIndex);
        if (nextPageIndex == Constants.PAGES.size() - 1) {
            nextPageIndex = 0;
        } else {
            nextPageIndex++;
        }
        return nextPage;
    }
}

