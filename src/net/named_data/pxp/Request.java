package net.named_data.pxp;

import net.named_data.jndn.*;
import net.named_data.jndn.security.OnVerified;
import net.named_data.jndn.security.OnVerifyFailed;

import java.util.Date;

public abstract class Request extends Phrase
        implements OnData, OnVerified, OnVerifyFailed, OnTimeout, OnNetworkNack {

    protected long _pendingInterestId;
    protected boolean _isValidData;

    protected Request(){}

    public void onNetworkNack(Interest interest, NetworkNack networkNack) {
        System.out.println("NACK for interest " + interest.getName());
    }

    public void onTimeout(Interest interest) {
        System.out.println("TIMEOUT for interest" + interest.getName());
    }

    public void onData(Interest interest, Data data) {

    }

    public void onVerified(Data data) {
        _isValidData = true;
    }

    public void onVerifyFailed(Data data) {
        _isValidData = false;
    }

    /*protected void waitToRequest() throws InterruptedException {
        System.out.print(" in 3");
        Thread.sleep(1000);
        System.out.print(" 2");
        Thread.sleep(1000);
        System.out.println(" 1");
        Thread.sleep(1000);
    }*/
}
