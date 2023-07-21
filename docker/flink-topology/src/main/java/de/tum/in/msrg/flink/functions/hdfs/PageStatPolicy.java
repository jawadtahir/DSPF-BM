package de.tum.in.msrg.flink.functions.hdfs;

import org.apache.flink.streaming.api.functions.sink.filesystem.PartFileInfo;
import org.apache.flink.streaming.api.functions.sink.filesystem.RollingPolicy;

import java.io.IOException;

public class PageStatPolicy<ID, BucketID> implements RollingPolicy<ID, BucketID> {
    @Override
    public boolean shouldRollOnCheckpoint(PartFileInfo<BucketID> partFileState) throws IOException {
        return true;
    }

    @Override
    public boolean shouldRollOnEvent(PartFileInfo<BucketID> partFileState, ID element) throws IOException {
        return true;
    }

    @Override
    public boolean shouldRollOnProcessingTime(PartFileInfo<BucketID> partFileState, long currentTime) throws IOException {
        return true;
    }
}
