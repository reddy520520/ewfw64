
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
