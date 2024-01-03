package de.tum.in.msrg.storm.window;

import org.apache.storm.tuple.Tuple;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class OneMinuteWindow {

    final private long winStart;
    final private long winEnd;
    final private List<Tuple> tuples;

    public static final long WINDOW_LENGTH_MS = 60_000;

    public OneMinuteWindow(long winStart){
        this.winStart = winStart;
        this.winEnd = winStart + WINDOW_LENGTH_MS;
        tuples = new ArrayList<>();
    }

    public long getWinStart() {
        return winStart;
    }

    public long getWinEnd() {
        return winEnd;
    }

    public List<Tuple> getTuples() {
        return tuples;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("OneMinuteWindowIDs{");
        sb.append("winStart=").append(winStart);
        sb.append(", winEnd=").append(winEnd);
        sb.append(", tuples=").append(Arrays.deepToString(tuples.toArray()));
        sb.append('}');
        return sb.toString();
    }
}
