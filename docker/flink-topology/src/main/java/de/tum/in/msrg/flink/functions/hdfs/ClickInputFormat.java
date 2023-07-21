package de.tum.in.msrg.flink.functions.hdfs;

import de.tum.in.msrg.datamodel.ClickEvent;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.connector.file.src.reader.SimpleStreamFormat;
import org.apache.flink.connector.file.src.reader.StreamFormat;
import org.apache.flink.core.fs.FSDataInputStream;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class ClickInputFormat extends SimpleStreamFormat<ClickEvent> {
    @Override
    public Reader<ClickEvent> createReader(Configuration config, FSDataInputStream stream) throws IOException {
        return new ClickEventReader(new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8)));
    }

    @Override
    public TypeInformation<ClickEvent> getProducedType() {
        return TypeInformation.of(ClickEvent.class);
    }

    public static final class ClickEventReader implements StreamFormat.Reader<ClickEvent> {

        private final BufferedReader reader;
        private final ObjectMapper objectMapper;

        ClickEventReader(final BufferedReader reader) {
            this.reader = reader;
            this.objectMapper = new ObjectMapper();
        }

        @Nullable
        @Override
        public ClickEvent read() throws IOException {
            String body = reader.readLine();
            ClickEvent clickEvent = null;
            if (body != null){
                clickEvent = this.objectMapper.readValue(body, ClickEvent.class);
            }
            return clickEvent;
        }

        @Override
        public void close() throws IOException {
            reader.close();
        }
    }
}
