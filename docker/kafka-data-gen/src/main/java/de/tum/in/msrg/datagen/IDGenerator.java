package de.tum.in.msrg.datagen;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class IDGenerator {
    private static IDGenerator INSTANCE;

    private long id = 0;

    private final Logger logger = LogManager.getLogger(IDGenerator.class);

    private IDGenerator(){
        logger.info(String.format("Created generator. %s", this.toString()));
    }

    public synchronized long getId(){
        return ++id;
    }

    public synchronized static IDGenerator getInstance(){
        if (INSTANCE == null){
            INSTANCE = new IDGenerator();

        }
        return INSTANCE;
    }
}
