package de.tum.in.msrg.flink.functions.hdfs;

import de.tum.in.msrg.datamodel.ClickEvent;
import de.tum.in.msrg.datamodel.UpdateEvent;
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

public class UpdateInputFormat extends SimpleStreamFormat<UpdateEvent> {
    @Override
    public Reader<UpdateEvent> createReader(Configuration config, FSDataInputStream stream) throws IOException {
        return new UpdateEventReader(new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8)));
    }

    @Override
    public TypeInformation<UpdateEvent> getProducedType() {
        return TypeInformation.of(UpdateEvent.class);
    }

    public static final class UpdateEventReader implements StreamFormat.Reader<UpdateEvent> {

        private final BufferedReader reader;
        private final ObjectMapper objectMapper;

        UpdateEventReader(final BufferedReader reader) {
            this.reader = reader;
            this.objectMapper = new ObjectMapper();
        }

        @Nullable
        @Override
        public UpdateEvent read() throws IOException {
            String body = reader.readLine();
            UpdateEvent updateEvent = null;
            if (body != null) {
                updateEvent = this.objectMapper.readValue(body, UpdateEvent.class);
            }
            return updateEvent;
        }

        @Override
        public void close() throws IOException {
            reader.close();
        }
    }
}
