package net.named_data.pxp;

import java.util.Date;

/**
 *
 */
public abstract class Phrase {

    protected Date _beginTime = new Date();

    public void begin(){}

    public Date getBeginTime(){
        return _beginTime;
    }
}
