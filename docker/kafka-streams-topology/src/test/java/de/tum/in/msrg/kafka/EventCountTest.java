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

import java.util.Properties;

public class EventCountTest {
    private TopologyTestDriver driver;
    private TestInputTopic<String, String> inputTopic;
    private TestOutputTopic<String, ClickEventStatistics> outputTopic;
    private ObjectMapper objectMapper = new ObjectMapper();

    @Before
    public void setup(){
        Properties props = EventCount.getProperties();
        Topology topology = EventCount.getTopology("input", "output", null);
        driver = new TopologyTestDriver(topology, props);

        inputTopic = driver.createInputTopic("input", new StringSerializer(), new StringSerializer());
        outputTopic = driver.createOutputTopic("output", new StringDeserializer(), new ClickEventStatsSerde());
    }

    @Ignore
    @Test
    public void testTopology(){
        ClickEventCountTest.ClickIterator iter = new ClickEventCountTest.ClickIterator();
        for (int i = 0; i <= 6000; i++){
            ClickEvent event = iter.next();
            try {
                inputTopic.pipeInput(event.getPage(), objectMapper.writeValueAsString(event));
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }
        Assert.assertEquals(12, outputTopic.readKeyValuesToList().size());
    }

    @After
    public void tearDown(){this.driver.close();}
}
