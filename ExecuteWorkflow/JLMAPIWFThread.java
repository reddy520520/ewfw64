// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   ExecuteWorkflow.java

package ExecuteWorkflow;

import com.informatica.powercenter.sdk.lm.*;
import java.io.PrintStream;

class JLMAPIWFThread extends Thread
{

    public JLMAPIWFThread(IJLMWorkflow workflow)
    {
        wk = workflow;
    }

    public void run()
    {
        try
        {
            IJLMDriver ld = DriverFactory.getDriver();
            ld.createThreadLocale();
            wk.recover(wk);
            ld.destroyThreadLocale();
        }
        catch(JLMException wr)
        {
            if(wr.getErrorCode() == -292)
            {
                try
                {
                    wk.start(EJLMRequestMode.NORMAL, null);
                }
                catch(JLMException ws1)
                {
                    System.err.println(ws1.getMessage());
                    System.exit(99);
                }
            } else
            {
                System.err.println(wr.getMessage());
                System.exit(99);
            }
        }
    }

    private IJLMWorkflow wk;
}
