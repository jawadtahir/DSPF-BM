package de.tum.in.msrg.datagen.adapter;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

public class HDFSAdapter implements AutoCloseable, Adapter {
    private Configuration configuration;
    private FileSystem fs;
    private static final String DATA_DIR = "/datagen";
    private static final String OUTPUT_DIR = "/output";
    private final Path baseDir = new Path(DATA_DIR);

    private static final Logger LOGGER  = LogManager.getLogger(HDFSAdapter.class);

    public HDFSAdapter (Configuration conf, String uri) throws IOException, URISyntaxException {

        configuration = conf;
        URI hdfsUri = new URI(uri);
        fs = FileSystem.get(hdfsUri, configuration);
        clearDataDir(fs, this.baseDir);
        emptyDir(fs,new Path(OUTPUT_DIR));

//        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
//            try {
//                fs = FileSystem.get(hdfsUri, configuration);
//                clearDataDir(fs, this.baseDir);
//                clearDataDir(fs,new Path(OUTPUT_DIR));
//                fs.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }));
    }

    public void send (String stream, String key, String id, String event) {
        LOGGER.debug(String.format("Trying to write %s in stream %s with the data %s", id, stream, event));
        Path basePath = Path.mergePaths(this.baseDir, new Path(Path.SEPARATOR + stream));

        Path filePath = Path.mergePaths(basePath, new Path(Path.SEPARATOR + Path.CUR_DIR + id));
        try (FSDataOutputStream outputStream = fs.create(filePath)) {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));
            writer.write(event);
            writer.newLine();
            writer.flush();
            writer.close();

            LOGGER.debug("Hidden file created");
            Path newFilePath = Path.mergePaths(basePath, new Path(Path.SEPARATOR + id));
            fs.rename(filePath, newFilePath);
            LOGGER.debug("File created");
        } catch (IOException exception){
            LOGGER.error("Unable to create file");
            exception.printStackTrace();
        }
    }

    @Override
    public void flush() {

    }

    private static void clearDataDir (FileSystem fs, Path basePath) throws IOException {
        RemoteIterator<LocatedFileStatus> iterator = fs.listLocatedStatus(basePath);
        while (iterator.hasNext()){
            LocatedFileStatus fileStatus = iterator.next();
            if (fileStatus.isDirectory()){
                clearDataDir(fs, fileStatus.getPath());
            } else {
                fs.delete(fileStatus.getPath(), false);
            }
        }
    }

    private static void emptyDir (FileSystem fs, Path basePath) throws IOException {
        RemoteIterator<LocatedFileStatus> iterator = fs.listLocatedStatus(basePath);
        while (iterator.hasNext()){

            LocatedFileStatus fileStatus = iterator.next();
            fs.delete(fileStatus.getPath(), true);
        }
    }

    public static void main (String [] args) throws IOException, URISyntaxException {

        Configuration conf = new Configuration();
        conf.set("fs.defaultFS", "hdfs://172.24.33.94:9000");
        String hdfsUri = "hdfs://172.24.33.94:9000";





        HDFSAdapter hdfsAdapter = new HDFSAdapter(conf, hdfsUri);
        hdfsAdapter.send( "click", "help", "0", "random text");
    }

    @Override
    public void close() throws Exception {
        fs.close();
    }
}
