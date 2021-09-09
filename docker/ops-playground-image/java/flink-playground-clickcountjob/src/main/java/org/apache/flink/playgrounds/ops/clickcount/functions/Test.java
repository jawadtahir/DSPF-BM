package org.apache.flink.playgrounds.ops.clickcount.functions;

import org.apache.flink.playgrounds.ops.clickcount.ClickEventGenerator;
import org.apache.flink.playgrounds.ops.clickcount.records.ClickEvent;
import org.apache.flink.playgrounds.ops.clickcount.records.ClickEventSerializationSchema;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.apache.flink.playgrounds.ops.clickcount.ClickEventCount.WINDOW_SIZE;

public class Test {
    private static final List<String> pages = Arrays.asList("/help", "/index", "/shop", "/jobs", "/about", "/news");

    private static final int EVENTS_PER_WINDOW = 5000;

    static class ClickIterator  {

        private Map<String, Long> nextTimestampPerKey;
        private int nextPageIndex;

        ClickIterator() {
            nextTimestampPerKey = new HashMap<>();
            nextPageIndex = 0;
        }

        ClickEvent next() {
            String page = nextPage();
            return new ClickEvent(nextTimestamp(page), page);
        }

        private Date nextTimestamp(String page) {
            long nextTimestamp = nextTimestampPerKey.getOrDefault(page, 0L);
//			nextTimestampPerKey.put(page, nextTimestamp + WINDOW_SIZE.toMilliseconds() / EVENTS_PER_WINDOW);
            nextTimestampPerKey.put(page, nextTimestamp + (WINDOW_SIZE.toMilliseconds()  / EVENTS_PER_WINDOW ) );
            return new Date(nextTimestamp);
        }

        private String nextPage() {
            String nextPage = pages.get(nextPageIndex);
            if (nextPageIndex == pages.size() - 1) {
                nextPageIndex = 0;
            } else {
                nextPageIndex++;
            }
            return nextPage;
        }
    }


    public static void main(String[] args) throws InterruptedException {

        ClickIterator clickIterator = new ClickIterator();
        Date temp = new Date(0L);

        Long counter = 0L;
        long counter2 = 0L;

        while (true) {

            ClickEvent event = clickIterator.next();

            if (temp.toInstant().plus(60, ChronoUnit.SECONDS).isAfter(event.getTimestamp().toInstant()) ){
                counter2++;
            } else {

                temp = event.getTimestamp();
                System.out.println("Counter: "+counter2);
                System.out.println("TS: "+temp);
                counter2 = 0;
            }

//			Thread.sleep(DELAY);

            counter++;
            if (counter == 119) {
                Thread.sleep(1);
                counter = 0L;
            }
        }
    }
}
