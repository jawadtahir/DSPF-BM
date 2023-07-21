package de.tum.in.msrg.datagen.adapter;

public interface Adapter {

    public void send (String stream, String key, String id, String event);

    public void flush ();
}
