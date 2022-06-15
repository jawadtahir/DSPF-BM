package de.tum.in.msrg.latcal;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class PGVFinalizer extends Thread{

    private final Map<Long, Boolean> idMap;
    private final AtomicLong processedCount;
    private final AtomicLong duplicateCount;
    private Long unprocessedCount;

    public PGVFinalizer(Map idMap, AtomicLong processedCount, AtomicLong duplicateCount){
        this. idMap = idMap;
        this.processedCount = processedCount;
        this.duplicateCount = duplicateCount;
    }

    @Override
    public void run() {
        System.out.println("Running shutdown hook");
        this.unprocessedCount = (long) this.idMap.size();
        System.out.printf("Processed events: %d%n", processedCount.get());
        System.out.printf("Duplicate events: %d%n", duplicateCount.get());
        System.out.printf("Unprocessed events: %d%n", this.unprocessedCount);

        File reportFolder = new File(Instant.now().toString());
        reportFolder.mkdirs();
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(reportFolder.getAbsolutePath()+"/pgvreport.txt"));
            writer.write(String.format("Processed: %d", processedCount.get()));
            writer.write(String.format("Duplicates: %d", duplicateCount.get()));
            writer.write(String.format("Unprocessed: %d", unprocessedCount));
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
        }

    }
}
