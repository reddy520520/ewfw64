
package ExecuteWorkflow;

import com.informatica.powercenter.sdk.lm.*;

class JLMAPISessThread extends Thread
{

    public JLMAPISessThread(IJLMTask session)
    {
        Task = session;
    }

    public void run()
    {
        try
        {
            IJLMDriver ld = DriverFactory.getDriver();
            ld.createThreadLocale();
            Task.recover(Task);
            ld.destroyThreadLocale();
        }
        catch(JLMException wr)
        {
            wr.printStackTrace();
            System.exit(99);
        }
    }

    private IJLMTask Task;
}
