package de.tum.in.msrg.datagen;

import de.tum.in.msrg.common.Constants;
import de.tum.in.msrg.datamodel.UpdateEvent;

import java.util.Date;

public class UpdateDataset {
    private int nextPageIndex;
    private IDGenerator id;

    public UpdateDataset(){
        this.nextPageIndex = 0;
        id = IDGenerator.getInstance();
    }

    public UpdateEvent next(Date timestamp){
        if (nextPageIndex == Constants.PAGES.size()){
            nextPageIndex = 0;
        }
        return new UpdateEvent(id.getId(), timestamp, Constants.PAGES.get(nextPageIndex++), "");
    }
}
