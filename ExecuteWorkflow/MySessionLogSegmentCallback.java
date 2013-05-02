// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   ExecuteWorkflow.java

package ExecuteWorkflow;

import com.informatica.powercenter.sdk.lm.IJLMLogSegment;
import com.informatica.powercenter.sdk.lm.IJLMLogSegmentCallback;

class MySessionLogSegmentCallback
    implements IJLMLogSegmentCallback
{

    MySessionLogSegmentCallback()
    {
    }

    public void receiveLogSegment(IJLMLogSegment logSegment)
    {
        logSeg = logSegment;
        displayLogSegment();
    }

    public void displayLogSegment()
    {
        bufferByte = logSeg.getMBCSBuffer();
        String byteArray = new String(bufferByte);
        System.out.println(byteArray);
    }

    private IJLMLogSegment logSeg;
    private int bufferSize;
    private byte bufferByte[];
}
