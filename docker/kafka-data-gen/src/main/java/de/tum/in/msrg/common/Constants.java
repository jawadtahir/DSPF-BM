package de.tum.in.msrg.common;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

public class Constants {

    public static final List<String> PAGES = Arrays.asList("/help", "/index", "/shop", "/jobs", "/about", "/news");
    public static final Duration WINDOW_SIZE = Duration.of(60, ChronoUnit.SECONDS );
    public static final String CLICK_TOPIC = "click";
    public static final String UPDATE_TOPIC = "update";
    public static final String OUTPUT_TOPIC = "output";
    public static final String LATE_OUTPUT_TOPIC = "lateOutput";
}
