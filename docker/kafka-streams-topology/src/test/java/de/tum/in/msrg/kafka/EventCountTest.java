package de.tum.in.msrg.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.tum.in.msrg.datamodel.ClickEvent;
import de.tum.in.msrg.datamodel.PageStatistics;
import de.tum.in.msrg.datamodel.UpdateEvent;
import de.tum.in.msrg.kafka.serdes.PageStatisticsSerdes;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.TestInputTopic;
import org.apache.kafka.streams.TestOutputTopic;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.TopologyTestDriver;
import org.junit.*;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class EventCountTest {
    private TopologyTestDriver driver;
    private TestInputTopic<String, String> clickTopic;
    private TestInputTopic<String, String> updateTopic;
    private TestOutputTopic<String, PageStatistics> outputTopic;
    private ObjectMapper objectMapper = new ObjectMapper();

    @Before
    public void setup(){
        Properties props = EventCount.getProperties();
        Topology topology = EventCount.getTopology(null);
        driver = new TopologyTestDriver(topology, props);

        clickTopic = driver.createInputTopic("click", new StringSerializer(), new StringSerializer());
        updateTopic = driver.createInputTopic("update", new StringSerializer(), new StringSerializer());
        outputTopic = driver.createOutputTopic("output", new StringDeserializer(), new PageStatisticsSerdes());
    }

    @Ignore
    @Test
    public void testTopology(){
        ClickIterator iter = new ClickIterator();
        long nextUpdate = Duration.of(30, ChronoUnit.SECONDS).toMillis();
        for (int i = 0; i <= 6000; i++){
            ClickEvent event = iter.next();
            try {
                if (event.getTimestamp().getTime() > nextUpdate){
                    for (UpdateEvent updatePage: updatePages(event)){
                        updateTopic.pipeInput(updatePage.getPage(), objectMapper.writeValueAsString(updatePage));
                    }
                    nextUpdate += Duration.ofSeconds(60).toMillis();
                }
                clickTopic.pipeInput(event.getPage(), objectMapper.writeValueAsString(event));
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }
        Assert.assertEquals(12, outputTopic.readKeyValuesToList().size());
    }

    protected List<UpdateEvent> updatePages(ClickEvent clickEvent) throws JsonProcessingException {
        List<UpdateEvent> retVal = new ArrayList<>();
        for (String page : ClickIterator.pages){
            UpdateEvent updateEvent = new UpdateEvent(clickEvent.getId(), clickEvent.getTimestamp(), page, "");
            retVal.add(updateEvent);
        }
        return retVal;
    }

    static class ClickIterator  {

        private Map<String, Long> nextTimestampPerKey;
        private int nextPageIndex;
        public static final List<String> pages = Arrays.asList("/help", "/index", "/shop", "/jobs", "/about", "/news");
        private long id = Long.MIN_VALUE;

        ClickIterator() {
            nextTimestampPerKey = new HashMap<>();
            nextPageIndex = 0;
        }

        ClickEvent next() {
            String page = nextPage();
            id++;
            return new ClickEvent(id, nextTimestamp(page), page);
//            return null;
        }

        private Date nextTimestamp(String page) {
            long nextTimestamp = nextTimestampPerKey.getOrDefault(page, 1L);
//			nextTimestampPerKey.put(page, nextTimestamp + WINDOW_SIZE.toMilliseconds() / EVENTS_PER_WINDOW);
            nextTimestampPerKey.put(page, nextTimestamp + (Duration.ofSeconds(60).toMillis()  / 500 ) );
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


    @After
    public void tearDown(){this.driver.close();}
}
