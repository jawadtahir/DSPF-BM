package de.tum.in.msrg.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.tum.in.msrg.datamodel.ClickEvent;
import de.tum.in.msrg.datamodel.ClickEventStatistics;
import de.tum.in.msrg.kafka.serdes.ClickEventStatsSerde;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.TestInputTopic;
import org.apache.kafka.streams.TestOutputTopic;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.TopologyTestDriver;
import org.junit.*;

import java.time.Duration;
import java.util.*;

public class ClickEventCountTest {

    private TopologyTestDriver driver;
    private TestInputTopic<String, String> inputTopic;
    private TestOutputTopic<String, ClickEventStatistics> outputTopic;
    private ObjectMapper objectMapper = new ObjectMapper();


    @Before
    public void setup(){
        Properties props = ClickEventCount.getProps();
        Topology topology = ClickEventCount.createTopology("input", "output");
        driver = new TopologyTestDriver(topology, props);

        inputTopic = driver.createInputTopic("input", new StringSerializer(), new StringSerializer());
        outputTopic = driver.createOutputTopic("output", new StringDeserializer(), new ClickEventStatsSerde());

    }

    @Ignore
    @Test
    public void testFunc(){
        ClickIterator iter = new ClickIterator();
        for (int i = 0; i <= 6000; i++){
            ClickEvent event = iter.next();
            try {
                inputTopic.pipeInput(event.getPage(), objectMapper.writeValueAsString(event));
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }

        Assert.assertEquals(13, outputTopic.readRecordsToList().size());
    }

    @After
    public void tearDown(){
        driver.close();
    }


    static class ClickIterator  {

        private Map<String, Long> nextTimestampPerKey;
        private int nextPageIndex;
        private static final List<String> pages = Arrays.asList("/help", "/index", "/shop", "/jobs", "/about", "/news");

        ClickIterator() {
            nextTimestampPerKey = new HashMap<>();
            nextPageIndex = 0;
        }

        ClickEvent next() {
            String page = nextPage();
            return new ClickEvent(nextTimestamp(page), page);
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
}
