// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   ExecuteWorkflow.java

package ExecuteWorkflow;

import com.informatica.powercenter.sdk.lm.*;
import org.apache.commons.cli.*;
import org.apache.log4j.PropertyConfigurator;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;

// Referenced classes of package ExecuteWorkflow:
//            JLMAPISessThread, JLMAPIWFThread, MySessionLogSegmentCallback

public class ExecuteWorkflow extends Thread
{

    public ExecuteWorkflow()
    {
        recip = null;
    }

    public static String now()
    {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        return sdf.format(cal.getTime());
    }

    public static String hostname()
    {
        try
        {
            InetAddress addr = InetAddress.getLocalHost();
            hostname = addr.getHostName();
        }
        catch(UnknownHostException e) { }
        return hostname;
    }

    public static File logfilename()
    {
        try
        {
            tmplogfile = File.createTempFile("temp_", ".log", new File(tmpdir));
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
        tmplogfile.deleteOnExit();
        return tmplogfile;
    }

    public static File tmpparmfilename()
    {
        try
        {
            TmpParameterFile = File.createTempFile((new StringBuilder()).append(FolderName).append("_").append(WorkflowName).append("_").append(now()).toString(), ".tmp", new File(tmpdir));
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
        TmpParameterFile.deleteOnExit();
        return TmpParameterFile;
    }

    public void closefile(File tmplogfile, String inputstr)
        throws JLMException, IOException
    {
        PrintStream ops = System.out;
        FileOutputStream outStr = new FileOutputStream(tmplogfile, true);
        PrintStream newps = new PrintStream(outStr);
        System.setOut(newps);
        System.out.println(inputstr);
        outStr.close();
        newps.close();
        System.setOut(ops);
    }

    private String[] commaToArray(String aString)
    {
        String splittArray[] = null;
        if(aString != null || !aString.equalsIgnoreCase(""))
            splittArray = aString.split(",");
        return splittArray;
    }

    private static void printUsage(Options options)
    {
        HelpFormatter helpFormatter = new HelpFormatter();
        helpFormatter.setWidth(250);
        helpFormatter.printHelp("[-h Help] [-f foldername] [-w workflowname] [-r start|stop|recover|abort|force] [-s session] [-sf fromsession] [-p01-p10 Parameters] [-e email] [-pf parameterfile] [-pr propfilename]", "\nexecuteworkflow\n\n", options, "", false);
    }

    public void execute()
        throws Exception
    {
        PropertyConfigurator.configure("log4j.properties");
        try
        {
            intializeProps(propfilename);
            InitConnection(FolderName, WorkflowName);
            try
            {
                getparms(ParameterFile, parm01, parm02, parm03, parm04, parm05, parm06, parm07, parm08, parm09, parm10);
            }
            catch(Exception pw)
            {
                pw.printStackTrace();
                System.err.println("Error with prompts being passed to program.");
                System.err.println("Check that there are are (\") double quotes surrounding parameters ie (\"$$Parmater1=value1\")");
                disconnect();
                System.exit(WKUNKNOWN);
            }
            if(emaillist.equalsIgnoreCase(""))
            {
                recip = commaToArray(EMAIL);
            } else
            {
                String newemailstr = (new StringBuilder()).append(EMAIL).append(",").append(emaillist).toString();
                recip = commaToArray(newemailstr);
            }
            logfilename();
            try
            {
                if(!sessionname.equalsIgnoreCase(""))
                {
                    jobname = (new StringBuilder()).append("Informatica[").append(FolderName).append(".").append(WorkflowName).append(".")
                            .append(sessionname).append("]").toString();
                    executesession(WorkflowName, sessionname);
                } else
                {
                    jobname = (new StringBuilder()).append("Informatica[").append(FolderName).append(".").append(WorkflowName)
                            .append("]").toString();
                    executewf(WorkflowName, sessionfromname);
                }
            }
            catch(JLMException w)
            {
                w.printStackTrace();
                System.err.println("Unknown error executing workflow or session. Please contact Informatica Administrator");
                disconnect();
                System.exit(WKUNKNOWN);
            }
            disconnect();
        }
        catch(IOException e)
        {
            e.printStackTrace();
            disconnect();
            System.exit(WKUNKNOWN);
        }
        catch(JLMException f)
        {
            f.printStackTrace();
            disconnect();
            System.exit(WKUNKNOWN);
        }
        catch(InterruptedException i)
        {
            disconnect();
            System.exit(WKUNKNOWN);
        }
        catch(Exception r)
        {
            disconnect();
            System.exit(WKUNKNOWN);
        }
    }

    public void intializeProps(String propfilename)
        throws IOException
    {
        Properties props = new Properties();
        if(!propfilename.equalsIgnoreCase(""))
            props.load(new FileInputStream(propfilename));
        else
            props.load(new FileInputStream("../infa.properties"));
        DISERVICENAME = props.getProperty("DISERVICENAME").trim();
        DOMAINNAME = props.getProperty("DOMAINNAME").trim();
        USERNAME = props.getProperty("USERNAME").trim();
        PASSWORD = props.getProperty("PASSWORD").trim();
        REPOSITORYNAME = props.getProperty("REPOSITORYNAME").trim();
        EMAIL = props.getProperty("EMAIL").trim();
        PMLOGDIR = props.getProperty("PMLOGDIR").trim();
        SMTPEMAILFROM = props.getProperty("SMTPEMAILFROM").trim();
        SMTPEMAILHOST = props.getProperty("SMTPEMAILHOST").trim();
        SMTPEMAILPORT = props.getProperty("SMTPEMAILPORT").trim();
        SMTPEMAILUSER = props.getProperty("SMTPEMAILUSER").trim();
    }

    public void InitConnection(String FolderName, String WorkflowName)
        throws JLMException
    {
        ld = DriverFactory.getDriver("JLMDriver10");
        ld.initialize((new StringBuilder()).append("lib").append(fs).append("locale").toString());
        System.out.println((new StringBuilder()).append("Connecting to ").append(DISERVICENAME).append(" from ").append(hostname()).append(" as user ").append(USERNAME).toString());
        try
        {
            connection = ld.getConnectionEx(DOMAINNAME, DISERVICENAME, 180, null);
        }
        catch(Exception cex)
        {
            System.err.println("Error connecting to Integration service");
            cex.printStackTrace();
            System.exit(WKUNKNOWN);
        }
        try
        {
            connection.login("", REPOSITORYNAME, USERNAME, PASSWORD, null);
        }
        catch(Exception l)
        {
            System.err.println("Error logging in to Integration service");
            l.printStackTrace();
            System.exit(WKUNKNOWN);
        }
        if(SRVDET)
        {
            srvdetails = connection.getServerDetails(EJLMMonitorServerMode.ALL, null);
            IJLMWorkflowDetails rwf[] = srvdetails.getWorkflows();
            IJLMTaskDetails rtask[] = srvdetails.getTasks();
            System.out.println("***********Server Details********");
            System.out.println((new StringBuilder()).append("Server Status      : ").append(srvdetails.getStatus().toString()).toString());
            System.out.println((new StringBuilder()).append("Number of Workflows: ").append(rwf.length).toString());
            System.out.println((new StringBuilder()).append("Number of Sessions : ").append(rtask.length).toString());
            if(rwf.length > 0)
            {
                System.out.println("");
                System.out.println("List of current running workflows:");
                String dtlformatstr = "%1$-20s%2$-40s%3$-20s%4$-20s%5$-20s\n";
                System.out.format(dtlformatstr, new Object[] {
                    "Folder", "Workflow", "Status", "User", "Start Time"
                });
                for(int i = 0; i < rwf.length; i++)
                {
                    EJLMWorkflowStatus runwkStatus = rwf[i].getWorkflowRunStatus();
                    String userName = rwf[i].getUserName();
                    String workflowName = rwf[i].getWorkflowName();
                    String folderName = rwf[i].getFolderName();
                    String starttime = (new StringBuilder()).append(rwf[i].getStartTime().getMonth()).append("/").append(rwf[i].getStartTime().getDate()).append("/").append(rwf[i].getStartTime().getYear()).append(" ").append(rwf[i].getStartTime().getHours()).append(":").append(rwf[i].getStartTime().getMinutes()).append(":").append(rwf[i].getStartTime().getSeconds()).toString();
                    System.out.format(dtlformatstr, new Object[] {
                        folderName, workflowName, runwkStatus.toString(), userName, starttime
                    });
                }

            }
            System.out.println("");
        }
        wk = connection.getWorkflow(FolderName, WorkflowName, null);
    }

    public void executesession(String WorkflowName, String sessionname)
        throws JLMException, IOException, SQLException
    {
        System.out.println((new StringBuilder()).append("Getting task ").append(sessionname).append(" from workflow ").append(WorkflowName).toString());
        try
        {
            task = wk.getTask(null, sessionname);
            try
            {
                gettaskparms(ParameterFile, parm01, parm02, parm03, parm04, parm05, parm06, parm07, parm08, parm09, parm10);
            }
            catch(Exception pw)
            {
                pw.printStackTrace();
                System.err.println("Error with prompts being passed to program.");
                System.err.println("Check that there are are (\") double quotes surrounding parameters ie (\"$$Parmater1=value1\")");
                System.exit(WKUNKNOWN);
            }
            if(task != null)
            {
                task.setTaskInstPath(sessionname);
                if(restart.equalsIgnoreCase("START"))
                {
                    task.setTaskReason("Starting");
                    if(!wfinstancename.equalsIgnoreCase(""))
                    {
                        task.setWorkflowRunInstanceName(wfinstancename);
                        System.out.println((new StringBuilder()).append("Starting task ").append(sessionname).append(" in workflow ").append(WorkflowName).append(" with workflow instance name [").append(wfinstancename).append("]").toString());
                    } else
                    {
                        System.out.println((new StringBuilder()).append("Starting task ").append(sessionname).append(" in workflow ").append(WorkflowName).toString());
                    }
                    task.start(EJLMRequestMode.NORMAL, null);
                    task.waitTillComplete(null);
                    task.recover(null);
                } else
                if(restart.equalsIgnoreCase("RECOVER"))
                {
                    System.out.println((new StringBuilder()).append("Recovering task ").append(sessionname).append(" in workflow ").append(WorkflowName).toString());
                    task.setTaskReason("Recovering");
                    JLMAPISessThread thread2 = new JLMAPISessThread(task);
                    thread2.start();
                    task.waitTillComplete(null);
                } else
                if(restart.equalsIgnoreCase("FORCE"))
                {
                    System.out.println((new StringBuilder()).append("Recovering task ").append(sessionname).append(" in workflow ").append(WorkflowName).toString());
                    task.setTaskReason("Recovering");
                    JLMAPISessThread thread2 = new JLMAPISessThread(task);
                    thread2.start();
                    task.waitTillComplete(null);
                } else
                if(restart.equalsIgnoreCase("ABORT"))
                {
                    System.out.println((new StringBuilder()).append("Aborting task ").append(sessionname).append(" in workflow ").append(WorkflowName).toString());
                    wk.stop(true, null);
                } else
                if(restart.equalsIgnoreCase("STOP"))
                {
                    System.out.println((new StringBuilder()).append("Aborting task ").append(sessionname).append(" in workflow ").append(WorkflowName).toString());
                    wk.stop(false, null);
                } else
                {
                    System.out.println((new StringBuilder()).append("Invalid option starting session ").append(WorkflowName).append(" use start, recover, stop or abort").toString());
                    System.exit(WKUNKNOWN);
                }
            } else
            {
                System.out.println((new StringBuilder()).append("Error:  Session ").append(sessionname).append(" in workflow ").append(WorkflowName).append(" can not be found").toString());
            }
            taskDetails = task.getTaskDetails(null);
            String taskStatus = taskDetails.getRunStatus().toString();
            System.out.println((new StringBuilder()).append("Session ").append(sessionname).append(" in workflow ").append(WorkflowName).append(" is: ").append(taskStatus.toString()).toString());
            getwfresults();
            printsessstatus();
            System.exit(WKSUCEED);
        }
        catch(JLMException s)
        {
            getwfresults();
            savewfresults(tmplogfile);
            printsessstatus();
            savesessstatus(tmplogfile);
            closefile(tmplogfile, "</table>");
            wkDetails = wk.getWorkflowDetails(null);
            wkStatus = wkDetails.getWorkflowRunStatus();
            String StrStatus = wkStatus.toString();
            if(StrStatus.equalsIgnoreCase("SUSPENDED"))
            {
                postMail("SUSPENDED");
                System.exit(WKSUSPEND);
            } else
            if(StrStatus.equalsIgnoreCase("FAILED"))
            {
                postMail("FAILED");
                System.exit(WKFAIL);
            } else
            if(StrStatus.equalsIgnoreCase("ABORTED"))
            {
                postMail("ABORTED");
                System.exit(WKABORT);
            } else
            if(StrStatus.equalsIgnoreCase("TERMINATED"))
            {
                postMail("TERMINATED");
                System.exit(WKTERM);
            } else
            if(StrStatus.equalsIgnoreCase("STOPPED"))
            {
                postMail("STOPPED");
                System.exit(WKSTOP);
            } else
            {
                postMail("FAILED");
                System.exit(WKUNKNOWN);
            }
        }
    }

    public void executewf(String WorkflowName, String sessionfromname)
        throws JLMException, IOException, InterruptedException, SQLException
    {
        System.out.println("***********Starting Execution********");
        if(restart.equalsIgnoreCase("START"))
        {
            if(!sessionfromname.equalsIgnoreCase("noinput"))
            {
                wk.setWorkflowReason("Starting");
                if(!wfinstancename.equalsIgnoreCase(""))
                {
                    wk.setInstanceName(wfinstancename);
                    System.out.println((new StringBuilder()).append("Starting Workflow ").append(WorkflowName).append(" from task ").
                            append(sessionfromname).append(" with workflow instance name [").append(wfinstancename).append("]").toString());
                } else
                {
                    System.out.println((new StringBuilder()).append("Starting Workflow ").append(WorkflowName).
                            append(" from task ").append(sessionfromname).toString());
                }
                try
                {
                    wk.start(EJLMRequestMode.NORMAL, sessionfromname, null);
                }
                catch(JLMException ss)
                {
                    if(ss.getErrorCode() == -220)
                    {
                        System.out.println((new StringBuilder()).append("Workflow ").append(WorkflowName).append(" is in a suspended status, use the recover option").toString());
                        disconnect();
                        System.exit(WKSUSPEND);
                    } else
                    {
                        ss.printStackTrace();
                    }
                }
            } else
            {
                wk.setWorkflowReason("Starting");
                if(!wfinstancename.equalsIgnoreCase(""))
                {
                    wk.setInstanceName(wfinstancename);
                    System.out.println((new StringBuilder()).append("Starting Workflow ").append(WorkflowName).append(" with workflow instance name [").append(wfinstancename).append("]").toString());
                } else
                {
                    System.out.println((new StringBuilder()).append("Starting Workflow ").append(WorkflowName).toString());
                }
                try
                {
                    wk.start(EJLMRequestMode.NORMAL, null);
                }
                catch(JLMException ws)
                {
                    if(ws.getErrorCode() == -220)
                    {
                        System.out.println((new StringBuilder()).append("Workflow ").append(WorkflowName).append(" is in a suspended status, use the recover option").toString());
                        disconnect();
                        System.exit(WKSUSPEND);
                    } else
                    {
                        ws.printStackTrace();
                    }
                }
            }
        } else
        if(restart.equalsIgnoreCase("RECOVER"))
        {
            System.out.println((new StringBuilder()).append("Recovering Workflow ").append(WorkflowName).toString());
            wk.setWorkflowReason("Recovering");
            JLMAPIWFThread thread1 = new JLMAPIWFThread(wk);
            thread1.start();
        } else
        if(restart.equalsIgnoreCase("FORCE"))
        {
            if(!sessionfromname.equalsIgnoreCase("noinput"))
            {
                System.out.println((new StringBuilder()).append("Starting Workflow ").append(WorkflowName).append(" from task ").append(sessionfromname).toString());
                wk.setWorkflowReason("Starting");
                try
                {
                    wk.start(EJLMRequestMode.NORMAL, sessionfromname, null);
                }
                catch(JLMException ss)
                {
                    if(ss.getErrorCode() == -220)
                    {
                        System.out.println((new StringBuilder()).append("Found Workflow ").append(WorkflowName).append(" in a suspended status, trying to recover workflow").toString());
                        System.out.println((new StringBuilder()).append("Recovering Workflow ").append(WorkflowName).toString());
                        wk.setWorkflowReason("Recovering");
                        JLMAPIWFThread thread1 = new JLMAPIWFThread(wk);
                        thread1.start();
                    }
                }
            } else
            {
                System.out.println((new StringBuilder()).append("Starting Workflow ").append(WorkflowName).toString());
                wk.setWorkflowReason("Starting");
                try
                {
                    wk.start(EJLMRequestMode.NORMAL, null);
                }
                catch(JLMException ws)
                {
                    if(ws.getErrorCode() == -220)
                    {
                        System.out.println((new StringBuilder()).append("Found Workflow ").append(WorkflowName).append(" in a suspended status, trying to recover workflow").toString());
                        System.out.println((new StringBuilder()).append("Recovering Workflow ").append(WorkflowName).toString());
                        wk.setWorkflowReason("Recovering");
                        JLMAPIWFThread thread1 = new JLMAPIWFThread(wk);
                        thread1.start();
                    }
                }
            }
        } else
        if(restart.equalsIgnoreCase("ABORT"))
        {
            System.out.println((new StringBuilder()).append("Aborting workflow ").append(WorkflowName).toString());
            wk.stop(true, null);
        } else
        if(restart.equalsIgnoreCase("STOP"))
        {
            System.out.println((new StringBuilder()).append("Stopping workflow ").append(WorkflowName).toString());
            wk.stop(false, null);
        } else
        {
            System.out.println((new StringBuilder()).append("Invalid option starting workflow ").append(WorkflowName).append(" use start or recover").toString());
            disconnect();
            System.exit(WKUNKNOWN);
        }
        wkDetails = wk.getWorkflowDetails(null);
        wkStatus = wkDetails.getWorkflowRunStatus();
        Thread.sleep(4000L);
        System.out.println((new StringBuilder()).append("Workflow ").append(WorkflowName).append(" status is: ").append(wkStatus.toString()).toString());
        int stopworkflow = 0;
        do
        {
            if(stopworkflow != 0)
                break;
            wkDetails = wk.getWorkflowDetails(null);
            wkStatus = wkDetails.getWorkflowRunStatus();
            String WkStat = wkStatus.toString();
            if(WkStat.equalsIgnoreCase("RUNNING"))
                stopworkflow = 0;
            if(WkStat.equalsIgnoreCase("ABORTED"))
                if(restart.equalsIgnoreCase("ABORT"))
                {
                    printtaskstatus();
                    postMail("ABORTED");
                    disconnect();
                    System.exit(WKSUCEED);
                } else
                {
                    printtaskstatus();
                    postMail("ABORTED");
                    disconnect();
                    System.exit(WKABORT);
                }
            if(WkStat.equalsIgnoreCase("ABORTING"))
                stopworkflow = 0;
            if(WkStat.equalsIgnoreCase("FAILED"))
            {
                printtaskstatus();
                postMail("FAILED");
                disconnect();
                System.exit(WKFAIL);
            }
            if(WkStat.equalsIgnoreCase("SCHEDULED"))
            {
                printtaskstatus();
                disconnect();
                System.exit(WKUNKNOWN);
            }
            if(WkStat.equalsIgnoreCase("STOPPED"))
                if(restart.equalsIgnoreCase("STOP"))
                {
                    printtaskstatus();
                    postMail("STOPPED");
                    disconnect();
                    System.exit(WKSUCEED);
                } else
                {
                    printtaskstatus();
                    postMail("STOPPED");
                    disconnect();
                    System.exit(WKSTOP);
                }
            if(WkStat.equalsIgnoreCase("STOPPING"))
                stopworkflow = 0;
            if(WkStat.equalsIgnoreCase("SUCCEEDED"))
            {
                printtaskstatus();
                disconnect();
                System.exit(WKSUCEED);
            }
            if(WkStat.equalsIgnoreCase("SUSPENDED"))
            {
                printtaskstatus();
                stopworkflow = 1;
                postMail("SUSPENDED");
                System.exit(WKSUSPEND);
            }
            if(WkStat.equalsIgnoreCase("SUSPENDING"))
                stopworkflow = 0;
            if(WkStat.equalsIgnoreCase("TERMINATED"))
            {
                printtaskstatus();
                postMail("TERMINATED");
                disconnect();
                System.exit(WKTERM);
            }
            if(WkStat.equalsIgnoreCase("UNKNOWN"))
            {
                printtaskstatus();
                disconnect();
                System.exit(WKUNKNOWN);
            }
            if(WkStat.equalsIgnoreCase("UNSCHEDULED"))
            {
                printtaskstatus();
                disconnect();
                System.exit(WKUNKNOWN);
            }
            if(WkStat.equalsIgnoreCase("WAITING"))
                stopworkflow = 0;
        } while(true);
    }

    public void savesessstatus(File tmplogfile)
        throws JLMException, IOException
    {
        PrintStream ops = System.out;
        FileOutputStream outStr = new FileOutputStream(tmplogfile, true);
        PrintStream newps = new PrintStream(outStr);
        System.setOut(newps);
        IJLMTaskDetails taskDet = task.getTaskDetails(sessionname);
        if(taskDet.isSessionType())
        {
            IJLMSession session = task.getSession(task);
            sessionDetails = session.getSessionDetails(task);
            IJLMTableStat stats[] = sessionDetails.getTableStatistics();
            System.out.println((new StringBuilder()).append("<br><br><b>Session [").append(sessionname).append("]</b><br>").toString());
            if(sessionDetails.getTaskStatus().toString().equalsIgnoreCase("SUCCEEDED"));
            System.out.println("<table border=\"1\"  width=\"40%\">");
            System.out.println((new StringBuilder()).append("<tr><td width=\"40%\">Session log location</td><td width=\"60%\">").append(sessionDetails.getLogFilePath()).append("</td></tr>").toString());
            System.out.println((new StringBuilder()).append("<tr><td width=\"40%\">Session Status</td><td width=\"60%\">").append(sessionDetails.getTaskStatus().toString().toUpperCase()).append("</td></tr>").toString());
            System.out.println((new StringBuilder()).append("<tr><td width=\"40%\">Src Success Rows</td><td width=\"60%\">").append(sessionDetails.getNumSrcSuccessRows()).append("</td></tr>").toString());
            System.out.println((new StringBuilder()).append("<tr><td width=\"40%\">Src Failed Rows</td><td width=\"60%\">").append(sessionDetails.getNumSrcFailedRows()).append("</td></tr>").toString());
            System.out.println((new StringBuilder()).append("<tr><td width=\"40%\">Tgt Success Rows</td><td width=\"60%\">").append(sessionDetails.getNumTgtSuccessRows()).append("</td></tr>").toString());
            System.out.println((new StringBuilder()).append("<tr><td width=\"40%\">Tgt Failed Rows</td><td width=\"60%\">").append(sessionDetails.getNumTgtFailedRows()).append("</td></tr>").toString());
            System.out.println((new StringBuilder()).append("<tr><td width=\"40%\">Session Error Message</td><td width=\"60%\">").append(taskDet.getRunErrorMsg()).append("</td></tr>").toString());
            System.out.println("</table>");
            System.out.println("<table border=\"1\"  width=\"100%\">");
            System.out.println("<tr><td><b>Transformation Name</b></td>");
            System.out.println("<td><b>Applied Rows</b></td>");
            System.out.println("<td><b>Affected Rows</b></td>");
            System.out.println("<td><b>Rejected Rows</b></td>");
            System.out.println("<td><b>Throughput(Rows/Sec)</b></td>");
            System.out.println("<td><b>Start Time</b></td>");
            System.out.println("<td><b>End Time</b></td>");
            System.out.println("<td><b>Last Error Code</b></td>");
            System.out.println("<td><b>Last Error Message</b></td></tr>");
            IJLMTableStat arr$[] = stats;
            int len$ = arr$.length;
            for(int i$ = 0; i$ < len$; i$++)
            {
                IJLMTableStat t = arr$[i$];
                EJLMWidgetType wtype = t.getWidgetType();
                if(wtype == EJLMWidgetType.DATASRCQUAL)
                    System.out.println((new StringBuilder()).append("<tr><td>").append(t.getWidgetName()).append("</td><td>").append(t.getAppliedRowsNum()).append("</td><td>").append(t.getAffectedRowsNum()).append("</td><td>").append(t.getRejectedRowsNum()).append("</td><td>").append(t.getThroughput()).append("</td><td>").append(t.getStartTime().getMonth()).append("/").append(t.getStartTime().getDate()).append("/").append(t.getStartTime().getYear()).append(" ").append(t.getStartTime().getHours()).append(":").append(t.getStartTime().getMinutes()).append(":").append(t.getStartTime().getSeconds()).append("</td><td>").append(t.getEndTime().getMonth()).append("/").append(t.getEndTime().getDate()).append("/").append(t.getEndTime().getYear()).append(" ").append(t.getEndTime().getHours()).append(":").append(t.getEndTime().getMinutes()).append(":").append(t.getEndTime().getSeconds()).append("</td><td>").append(t.getLastErrorCode()).append("</td><td>").append(t.getLastErrorMsg()).append("</td></tr>").toString());
                if(wtype == EJLMWidgetType.SDKDSQ)
                    System.out.println((new StringBuilder()).append("<tr><td>").append(t.getWidgetName()).append("</td><td>").append(t.getAppliedRowsNum()).append("</td><td>").append(t.getAffectedRowsNum()).append("</td><td>").append(t.getRejectedRowsNum()).append("</td><td>").append(t.getThroughput()).append("</td><td>").append(t.getStartTime().getMonth()).append("/").append(t.getStartTime().getDate()).append("/").append(t.getStartTime().getYear()).append(" ").append(t.getStartTime().getHours()).append(":").append(t.getStartTime().getMinutes()).append(":").append(t.getStartTime().getSeconds()).append("</td><td>").append(t.getEndTime().getMonth()).append("/").append(t.getEndTime().getDate()).append("/").append(t.getEndTime().getYear()).append(" ").append(t.getEndTime().getHours()).append(":").append(t.getEndTime().getMinutes()).append(":").append(t.getEndTime().getSeconds()).append("</td><td>").append(t.getLastErrorCode()).append("</td><td>").append(t.getLastErrorMsg()).append("</td></tr>").toString());
                if(wtype == EJLMWidgetType.SOURCE)
                    System.out.println((new StringBuilder()).append("<tr><td>").append(t.getWidgetName()).append("</td><td>").append(t.getAppliedRowsNum()).append("</td><td>").append(t.getAffectedRowsNum()).append("</td><td>").append(t.getRejectedRowsNum()).append("</td><td>").append(t.getThroughput()).append("</td><td>").append(t.getStartTime().getMonth()).append("/").append(t.getStartTime().getDate()).append("/").append(t.getStartTime().getYear()).append(" ").append(t.getStartTime().getHours()).append(":").append(t.getStartTime().getMinutes()).append(":").append(t.getStartTime().getSeconds()).append("</td><td>").append(t.getEndTime().getMonth()).append("/").append(t.getEndTime().getDate()).append("/").append(t.getEndTime().getYear()).append(" ").append(t.getEndTime().getHours()).append(":").append(t.getEndTime().getMinutes()).append(":").append(t.getEndTime().getSeconds()).append("</td><td>").append(t.getLastErrorCode()).append("</td><td>").append(t.getLastErrorMsg()).append("</td></tr>").toString());
                if(wtype == EJLMWidgetType.XMLDSQ)
                    System.out.println((new StringBuilder()).append("<tr><td>").append(t.getWidgetName()).append("</td><td>").append(t.getAppliedRowsNum()).append("</td><td>").append(t.getAffectedRowsNum()).append("</td><td>").append(t.getRejectedRowsNum()).append("</td><td>").append(t.getThroughput()).append("</td><td>").append(t.getStartTime().getMonth()).append("/").append(t.getStartTime().getDate()).append("/").append(t.getStartTime().getYear()).append(" ").append(t.getStartTime().getHours()).append(":").append(t.getStartTime().getMinutes()).append(":").append(t.getStartTime().getSeconds()).append("</td><td>").append(t.getEndTime().getMonth()).append("/").append(t.getEndTime().getDate()).append("/").append(t.getEndTime().getYear()).append(" ").append(t.getEndTime().getHours()).append(":").append(t.getEndTime().getMinutes()).append(":").append(t.getEndTime().getSeconds()).append("</td><td>").append(t.getLastErrorCode()).append("</td><td>").append(t.getLastErrorMsg()).append("</td></tr>").toString());
                if(wtype == EJLMWidgetType.ERPDSQ)
                    System.out.println((new StringBuilder()).append("<tr><td>").append(t.getWidgetName()).append("</td><td>").append(t.getAppliedRowsNum()).append("</td><td>").append(t.getAffectedRowsNum()).append("</td><td>").append(t.getRejectedRowsNum()).append("</td><td>").append(t.getThroughput()).append("</td><td>").append(t.getStartTime().getMonth()).append("/").append(t.getStartTime().getDate()).append("/").append(t.getStartTime().getYear()).append(" ").append(t.getStartTime().getHours()).append(":").append(t.getStartTime().getMinutes()).append(":").append(t.getStartTime().getSeconds()).append("</td><td>").append(t.getEndTime().getMonth()).append("/").append(t.getEndTime().getDate()).append("/").append(t.getEndTime().getYear()).append(" ").append(t.getEndTime().getHours()).append(":").append(t.getEndTime().getMinutes()).append(":").append(t.getEndTime().getSeconds()).append("</td><td>").append(t.getLastErrorCode()).append("</td><td>").append(t.getLastErrorMsg()).append("</td></tr>").toString());
                if(wtype == EJLMWidgetType.MQDSQ)
                    System.out.println((new StringBuilder()).append("<tr><td>").append(t.getWidgetName()).append("</td><td>").append(t.getAppliedRowsNum()).append("</td><td>").append(t.getAffectedRowsNum()).append("</td><td>").append(t.getRejectedRowsNum()).append("</td><td>").append(t.getThroughput()).append("</td><td>").append(t.getStartTime().getMonth()).append("/").append(t.getStartTime().getDate()).append("/").append(t.getStartTime().getYear()).append(" ").append(t.getStartTime().getHours()).append(":").append(t.getStartTime().getMinutes()).append(":").append(t.getStartTime().getSeconds()).append("</td><td>").append(t.getEndTime().getMonth()).append("/").append(t.getEndTime().getDate()).append("/").append(t.getEndTime().getYear()).append(" ").append(t.getEndTime().getHours()).append(":").append(t.getEndTime().getMinutes()).append(":").append(t.getEndTime().getSeconds()).append("</td><td>").append(t.getLastErrorCode()).append("</td><td>").append(t.getLastErrorMsg()).append("</td></tr>").toString());
                if(wtype == EJLMWidgetType.MAPPLET)
                    System.out.println((new StringBuilder()).append("<tr><td>").append(t.getWidgetName()).append("</td><td>").append(t.getAppliedRowsNum()).append("</td><td>").append(t.getAffectedRowsNum()).append("</td><td>").append(t.getRejectedRowsNum()).append("</td><td>").append(t.getThroughput()).append("</td><td>").append(t.getStartTime().getMonth()).append("/").append(t.getStartTime().getDate()).append("/").append(t.getStartTime().getYear()).append(" ").append(t.getStartTime().getHours()).append(":").append(t.getStartTime().getMinutes()).append(":").append(t.getStartTime().getSeconds()).append("</td><td>").append(t.getEndTime().getMonth()).append("/").append(t.getEndTime().getDate()).append("/").append(t.getEndTime().getYear()).append(" ").append(t.getEndTime().getHours()).append(":").append(t.getEndTime().getMinutes()).append(":").append(t.getEndTime().getSeconds()).append("</td><td>").append(t.getLastErrorCode()).append("</td><td>").append(t.getLastErrorMsg()).append("</td></tr>").toString());
                if(wtype == EJLMWidgetType.DATADRIVEN)
                    System.out.println((new StringBuilder()).append("<tr><td>").append(t.getWidgetName()).append("</td><td>").append(t.getAppliedRowsNum()).append("</td><td>").append(t.getAffectedRowsNum()).append("</td><td>").append(t.getRejectedRowsNum()).append("</td><td>").append(t.getThroughput()).append("</td><td>").append(t.getStartTime().getMonth()).append("/").append(t.getStartTime().getDate()).append("/").append(t.getStartTime().getYear()).append(" ").append(t.getStartTime().getHours()).append(":").append(t.getStartTime().getMinutes()).append(":").append(t.getStartTime().getSeconds()).append("</td><td>").append(t.getEndTime().getMonth()).append("/").append(t.getEndTime().getDate()).append("/").append(t.getEndTime().getYear()).append(" ").append(t.getEndTime().getHours()).append(":").append(t.getEndTime().getMinutes()).append(":").append(t.getEndTime().getSeconds()).append("</td><td>").append(t.getLastErrorCode()).append("</td><td>").append(t.getLastErrorMsg()).append("</td></tr>").toString());
                if(wtype == EJLMWidgetType.OUTPUT)
                    System.out.println((new StringBuilder()).append("<tr><td>").append(t.getWidgetName()).append("</td><td>").append(t.getAppliedRowsNum()).append("</td><td>").append(t.getAffectedRowsNum()).append("</td><td>").append(t.getRejectedRowsNum()).append("</td><td>").append(t.getThroughput()).append("</td><td>").append(t.getStartTime().getMonth()).append("/").append(t.getStartTime().getDate()).append("/").append(t.getStartTime().getYear()).append(" ").append(t.getStartTime().getHours()).append(":").append(t.getStartTime().getMinutes()).append(":").append(t.getStartTime().getSeconds()).append("</td><td>").append(t.getEndTime().getMonth()).append("/").append(t.getEndTime().getDate()).append("/").append(t.getEndTime().getYear()).append(" ").append(t.getEndTime().getHours()).append(":").append(t.getEndTime().getMinutes()).append(":").append(t.getEndTime().getSeconds()).append("</td><td>").append(t.getLastErrorCode()).append("</td><td>").append(t.getLastErrorMsg()).append("</td></tr>").toString());
                if(wtype == EJLMWidgetType.TARGET)
                    System.out.println((new StringBuilder()).append("<tr><td>").append(t.getWidgetName()).append("</td><td>").append(t.getAppliedRowsNum()).append("</td><td>").append(t.getAffectedRowsNum()).append("</td><td>").append(t.getRejectedRowsNum()).append("</td><td>").append(t.getThroughput()).append("</td><td>").append(t.getStartTime().getMonth()).append("/").append(t.getStartTime().getDate()).append("/").append(t.getStartTime().getYear()).append(" ").append(t.getStartTime().getHours()).append(":").append(t.getStartTime().getMinutes()).append(":").append(t.getStartTime().getSeconds()).append("</td><td>").append(t.getEndTime().getMonth()).append("/").append(t.getEndTime().getDate()).append("/").append(t.getEndTime().getYear()).append(" ").append(t.getEndTime().getHours()).append(":").append(t.getEndTime().getMinutes()).append(":").append(t.getEndTime().getSeconds()).append("</td><td>").append(t.getLastErrorCode()).append("</td><td>").append(t.getLastErrorMsg()).append("</td></tr>").toString());
            }

        }
        outStr.close();
        newps.close();
        System.setOut(ops);
    }

    public void printsessstatus()
        throws JLMException
    {
        IJLMTaskDetails taskDet = task.getTaskDetails(sessionname);
        if(taskDet.isSessionType())
        {
            IJLMSession session = task.getSession(task);
            sessionDetails = session.getSessionDetails(task);
            IJLMTableStat stats[] = sessionDetails.getTableStatistics();
            System.out.println("");
            if(sessionDetails.getTaskStatus().toString().equalsIgnoreCase("SUCCEEDED"));
            System.out.println("=======================================================================================");
            System.out.println("");
            System.out.println((new StringBuilder()).append("Session ").append(sessionname).toString());
            System.out.println("--------------------------------------------------------");
            System.out.println((new StringBuilder()).append("Session log location     :             ").append(sessionDetails.getLogFilePath()).toString());
            System.out.println((new StringBuilder()).append("Session Status           :             ").append(sessionDetails.getTaskStatus().toString().toUpperCase()).toString());
            System.out.println((new StringBuilder()).append("Src Success Rows         :             ").append(sessionDetails.getNumSrcSuccessRows()).toString());
            System.out.println((new StringBuilder()).append("Src Failed Rows          :             ").append(sessionDetails.getNumSrcFailedRows()).toString());
            System.out.println((new StringBuilder()).append("Tgt Success Rows         :             ").append(sessionDetails.getNumTgtSuccessRows()).toString());
            System.out.println((new StringBuilder()).append("Tgt Failed Rows          :             ").append(sessionDetails.getNumTgtFailedRows()).toString());
            System.out.println((new StringBuilder()).append("Session Error Message    :             ").append(taskDet.getRunErrorMsg()).toString());
        }
    }

    private void getwfresults()
        throws JLMException
    {
        wkDetails = wk.getWorkflowDetails(null);
        wkStatus = wkDetails.getWorkflowRunStatus();
        wkStatusRes = wkDetails.getRunErrorCode();
        System.out.println("");
        System.out.println((new StringBuilder()).append("Workflow [").append(wkDetails.getWorkflowName()).append("] details").toString());
        System.out.println("--------------------------------------------------------");
        System.out.println((new StringBuilder()).append("Integration Service Name :             ").append(DISERVICENAME).toString());
        System.out.println((new StringBuilder()).append("Start Time:              :             ").append(wkDetails.getStartTime().getMonth()).append("/").append(wkDetails.getStartTime().getDate()).append("/").append(wkDetails.getStartTime().getYear()).append(" ").append(wkDetails.getStartTime().getHours()).append(":").append(wkDetails.getStartTime().getMinutes()).append(":").append(wkDetails.getStartTime().getSeconds()).toString());
        System.out.println((new StringBuilder()).append("End Time:                :             ").append(wkDetails.getEndTime().getMonth()).append("/").append(wkDetails.getEndTime().getDate()).append("/").append(wkDetails.getEndTime().getYear()).append(" ").append(wkDetails.getEndTime().getHours()).append(":").append(wkDetails.getEndTime().getMinutes()).append(":").append(wkDetails.getEndTime().getSeconds()).toString());
        System.out.println((new StringBuilder()).append("Status                   :             ").append(wkStatus.toString().toUpperCase()).toString());
        System.out.println("--------------------------------------------------------");
    }

    private void savewfresults(File tmplogfile)
        throws JLMException, IOException
    {
        PrintStream ops = System.out;
        FileOutputStream outStr = new FileOutputStream(tmplogfile, true);
        PrintStream newps = new PrintStream(outStr);
        System.setOut(newps);
        wkDetails = wk.getWorkflowDetails(null);
        wkStatus = wkDetails.getWorkflowRunStatus();
        wkStatusRes = wkDetails.getRunErrorCode();
        System.out.println("<html><head></head><body>");
        System.out.println((new StringBuilder()).append("<b>Workflow [").append(wkDetails.getWorkflowName()).append("]</b>").toString());
        System.out.println("<table border=\"1\" width=\"40%\">");
        System.out.println((new StringBuilder()).append("<tr><td>Integration Service Name</td><td>").append(DISERVICENAME).append("</td></tr>").toString());
        System.out.println((new StringBuilder()).append("<tr><td>Start Time</td><td>").append(wkDetails.getStartTime().getMonth()).append("/").append(wkDetails.getStartTime().getDate()).append("/").append(wkDetails.getStartTime().getYear()).append(" ").append(wkDetails.getStartTime().getHours()).append(":").append(wkDetails.getStartTime().getMinutes()).append(":").append(wkDetails.getStartTime().getSeconds()).append("</td></tr>").toString());
        System.out.println((new StringBuilder()).append("<tr><td>End Time</td><td>").append(wkDetails.getEndTime().getMonth()).append("/").append(wkDetails.getEndTime().getDate()).append("/").append(wkDetails.getEndTime().getYear()).append(" ").append(wkDetails.getEndTime().getHours()).append(":").append(wkDetails.getEndTime().getMinutes()).append(":").append(wkDetails.getEndTime().getSeconds()).append("</td></tr>").toString());
        System.out.println((new StringBuilder()).append("<tr><td>Status</td><td>").append(wkStatus.toString().toUpperCase()).append("</td></tr>").toString());
        System.out.println("</table>");
        outStr.close();
        newps.close();
        System.setOut(ops);
    }

    private void disconnect()
    {
        try
        {
            connection.close(null);
            ld.deinitialize();
        }
        catch(JLMException je) { }
    }

    private void getparms(String ParameterFile, String parm01, String parm02, String parm03, String parm04, String parm05, String parm06, 
            String parm07, String parm08, String parm09, String parm10)
    {
        List parmlist = new LinkedList();
        parmlist = new ArrayList();
        parmlist.add(parm01);
        parmlist.add(parm02);
        parmlist.add(parm03);
        parmlist.add(parm04);
        parmlist.add(parm05);
        parmlist.add(parm06);
        parmlist.add(parm07);
        parmlist.add(parm08);
        parmlist.add(parm09);
        parmlist.add(parm10);
        for(int i = 0; i < parmlist.size(); i++)
            if(!parmlist.get(i).toString().equalsIgnoreCase("none"))
                CntParam++;

        if(!ParameterFile.equalsIgnoreCase("none") && CntParam == 0)
        {
            System.out.println("***********Parameter Details********");
            System.out.println((new StringBuilder()).append("Using parameter file ").append(ParameterFile).toString());
            wk.setParamFile(ParameterFile);
            System.out.println("");
        } else
        if(!ParameterFile.equalsIgnoreCase("none") && CntParam > 0)
        {
            String line = null;
            try
            {
                tmpparmfilename();
                BufferedReader parmin = new BufferedReader(new FileReader(ParameterFile));
                BufferedWriter parmout = new BufferedWriter(new FileWriter(TmpParameterFile));
                while((line = parmin.readLine()) != null) 
                    parmout.write((new StringBuilder()).append(line).append("\n").toString());
                parmin.close();
                System.out.println("***********Parameter Details********");
                for(int i = 0; i <= CntParam; i++)
                    if(!parmlist.get(i).toString().equalsIgnoreCase("none"))
                    {
                        String parmstring = parmlist.get(i).toString();
                        parmstring = parmstring.replace("/$/$", "$$");
                        parmout.write((new StringBuilder()).append("\n").append(parmstring).toString());
                        System.out.println((new StringBuilder()).append("Adding parameter ").append(parmstring).append(" to parameter file ").append(TmpParameterFile).toString());
                    }

                parmout.close();
                System.out.println("");
            }
            catch(IOException e) { }
            wk.setParamFile(TmpParameterFile.toString());
        } else
        if(ParameterFile.equalsIgnoreCase("none") && CntParam > 0)
        {
            int cntnewparm = 1;
            System.out.println("***********Parameter Details********");
            for(int i = 0; i <= CntParam && !parmlist.get(i).toString().equalsIgnoreCase("none"); i++)
            {
                String parmstring = parmlist.get(i).toString();
                parmstring = parmstring.replace("/$/$", "$$");
                String Parm[] = parmstring.split("=");
                wk.addParameter((new StringBuilder()).append(FolderName).append(".WF:").append(WorkflowName).toString(), Parm[0], Parm[1]);
                System.out.println((new StringBuilder()).append("Added parameter ").append(cntnewparm).append(" - ").append(Parm[0]).append("=").append(Parm[1]).toString());
                cntnewparm++;
            }

            System.out.println("");
        }
    }

    private void gettaskparms(String ParameterFile, String parm01, String parm02, String parm03, String parm04, String parm05, String parm06, 
            String parm07, String parm08, String parm09, String parm10)
    {
        List parmlist = new LinkedList();
        parmlist = new ArrayList();
        parmlist.add(parm01);
        parmlist.add(parm02);
        parmlist.add(parm03);
        parmlist.add(parm04);
        parmlist.add(parm05);
        parmlist.add(parm06);
        parmlist.add(parm07);
        parmlist.add(parm08);
        parmlist.add(parm09);
        parmlist.add(parm10);
        for(int i = 0; i < parmlist.size(); i++)
            if(!parmlist.get(i).toString().equalsIgnoreCase("none"))
                CntParam++;

        if(!ParameterFile.equalsIgnoreCase("none") && CntParam == 0)
            task.setParamFile(ParameterFile);
        else
        if(!ParameterFile.equalsIgnoreCase("none") && CntParam > 0)
        {
            String line = null;
            try
            {
                tmpparmfilename();
                BufferedReader parmin = new BufferedReader(new FileReader(ParameterFile));
                BufferedWriter parmout = new BufferedWriter(new FileWriter(TmpParameterFile));
                while((line = parmin.readLine()) != null) 
                    parmout.write((new StringBuilder()).append(line).append("\n").toString());
                parmin.close();
                for(int i = 0; i <= CntParam; i++)
                    if(!parmlist.get(i).toString().equalsIgnoreCase("none"))
                    {
                        String parmstring = parmlist.get(i).toString();
                        parmstring = parmstring.replace("/$/$", "$$");
                        parmout.write((new StringBuilder()).append("\n").append(parmstring).toString());
                    }

                parmout.close();
            }
            catch(IOException e) { }
            task.setParamFile(TmpParameterFile.toString());
        } else
        if(ParameterFile.equalsIgnoreCase("none") && CntParam > 0)
        {
            int cntnewparm = 1;
            for(int i = 0; i <= CntParam && !parmlist.get(i).toString().equalsIgnoreCase("none"); i++)
            {
                String parmstring = parmlist.get(i).toString();
                parmstring = parmstring.replace("/$/$", "$$");
                String Parm[] = parmstring.split("=");
                task.addParameter((new StringBuilder()).append(FolderName).append(".WF:").append(WorkflowName).toString(), Parm[0], Parm[1]);
                cntnewparm++;
            }

        }
    }

    public void savelogs(IJLMWorkflowTasks objstatus, File tmplogfile)
        throws JLMException, IOException
    {
        PrintStream ops = System.out;
        FileOutputStream outStr = new FileOutputStream(tmplogfile, true);
        PrintStream newps = new PrintStream(outStr);
        System.setOut(newps);
        IJLMTaskTopLevelParent taskpar[] = objstatus.getTopLevelParents();
        IJLMTaskTopLevelParent arr$[] = taskpar;
        int len$ = arr$.length;
        for(int i$ = 0; i$ < len$; i$++)
        {
            IJLMTaskTopLevelParent p = arr$[i$];
            String taskname = p.getTaskInstanceName();
            try
            {
                task = wk.getTask(null, taskname);
            }
            catch(JLMException te)
            {
                if(te.getErrorCode() == -212)
                {
                    IJLMTaskDetails taskDet = task.getTaskDetails(objstatus);
                    EJLMTaskType tt = taskDet.getTaskType();
                    String tasktype = tt.toString();
                    if(tasktype.equalsIgnoreCase("worklet_task"))
                    {
                        taskname = (new StringBuilder()).append(p.getTopLevelParentName()).append(".").append(p.getTaskInstanceName()).toString();
                        task.setTaskInstPath((new StringBuilder()).append(p.getTopLevelParentName()).append(".").append(p.getTaskInstanceName()).toString());
                        task = wk.getTask(null, taskname);
                    }
                }
            }
            IJLMTaskDetails taskDet = task.getTaskDetails(objstatus);
            if(taskDet.isSessionType())
            {
                IJLMSession session = task.getSession(task);
                IJLMSessionDetails sessionDetails = session.getSessionDetails(task);
                IJLMTableStat stats[] = sessionDetails.getTableStatistics();
                if(!sessionDetails.getTaskStatus().toString().equalsIgnoreCase("SUCCEEDED"))
                {
                    System.out.println((new StringBuilder()).append("<br><br><b>Session [").append(taskname).append("]</b><br>").toString());
                    System.out.println("<table border=\"1\"  width=\"60%\">");
                    System.out.println((new StringBuilder()).append("<tr><td width=\"40%\">Session log location</td><td width=\"60%\">").append(sessionDetails.getLogFilePath()).append("</td></tr>").toString());
                    System.out.println((new StringBuilder()).append("<tr><td width=\"40%\">Session Status</td><td width=\"60%\">").append(sessionDetails.getTaskStatus().toString().toUpperCase()).append("</td></tr>").toString());
                    System.out.println((new StringBuilder()).append("<tr><td width=\"40%\">Src Success Rows</td><td width=\"60%\">").append(sessionDetails.getNumSrcSuccessRows()).append("</td></tr>").toString());
                    System.out.println((new StringBuilder()).append("<tr><td width=\"40%\">Src Failed Rows</td><td width=\"60%\">").append(sessionDetails.getNumSrcFailedRows()).append("</td></tr>").toString());
                    System.out.println((new StringBuilder()).append("<tr><td width=\"40%\">Tgt Success Rows</td><td width=\"60%\">").append(sessionDetails.getNumTgtSuccessRows()).append("</td></tr>").toString());
                    System.out.println((new StringBuilder()).append("<tr><td width=\"40%\">Tgt Failed Rows</td><td width=\"60%\">").append(sessionDetails.getNumTgtFailedRows()).append("</td></tr>").toString());
                    System.out.println((new StringBuilder()).append("<tr><td width=\"40%\">Session Error Message</td><td width=\"60%\">").append(taskDet.getRunErrorMsg()).append("</td></tr>").toString());
                    System.out.println("</table>");
                    System.out.println("<table border=\"1\"  width=\"100%\">");
                    System.out.println("<tr><td><b>Transformation Name</b></td>");
                    System.out.println("<td><b>Applied Rows</b></td>");
                    System.out.println("<td><b>Affected Rows</b></td>");
                    System.out.println("<td><b>Rejected Rows</b></td>");
                    System.out.println("<td><b>Throughput(Rows/Sec)</b></td>");
                    System.out.println("<td><b>Start Time</b></td>");
                    System.out.println("<td><b>End Time</b></td>");
                    System.out.println("<td><b>Last Error Code</b></td>");
                    System.out.println("<td><b>Last Error Message</b></td></tr>");
                    IJLMTableStat arr2$[] = stats;
                    int len2$ = arr$.length;
                    for(int i2$ = 0; i$ < len$; i$++)
                    {
                        IJLMTableStat t = arr2$[i$];
                        EJLMWidgetType wtype = t.getWidgetType();
                        if (wtype == EJLMWidgetType.DATASRCQUAL)
                            System.out.println((new StringBuilder()).append("<tr><td>").append(t.getWidgetName()).append("</td><td>").append(t.getAppliedRowsNum()).append("</td><td>").append(t.getAffectedRowsNum()).append("</td><td>").append(t.getRejectedRowsNum()).append("</td><td>").append(t.getThroughput()).append("</td><td>").append(t.getStartTime().getMonth()).append("/").append(t.getStartTime().getDate()).append("/").append(t.getStartTime().getYear()).append(" ").append(t.getStartTime().getHours()).append(":").append(t.getStartTime().getMinutes()).append(":").append(t.getStartTime().getSeconds()).append("</td><td>").append(t.getEndTime().getMonth()).append("/").append(t.getEndTime().getDate()).append("/").append(t.getEndTime().getYear()).append(" ").append(t.getEndTime().getHours()).append(":").append(t.getEndTime().getMinutes()).append(":").append(t.getEndTime().getSeconds()).append("</td><td>").append(t.getLastErrorCode()).append("</td><td>").append(t.getLastErrorMsg()).append("</td></tr>").toString());
                        if (wtype == EJLMWidgetType.SDKDSQ)
                            System.out.println((new StringBuilder()).append("<tr><td>").append(t.getWidgetName()).append("</td><td>").append(t.getAppliedRowsNum()).append("</td><td>").append(t.getAffectedRowsNum()).append("</td><td>").append(t.getRejectedRowsNum()).append("</td><td>").append(t.getThroughput()).append("</td><td>").append(t.getStartTime().getMonth()).append("/").append(t.getStartTime().getDate()).append("/").append(t.getStartTime().getYear()).append(" ").append(t.getStartTime().getHours()).append(":").append(t.getStartTime().getMinutes()).append(":").append(t.getStartTime().getSeconds()).append("</td><td>").append(t.getEndTime().getMonth()).append("/").append(t.getEndTime().getDate()).append("/").append(t.getEndTime().getYear()).append(" ").append(t.getEndTime().getHours()).append(":").append(t.getEndTime().getMinutes()).append(":").append(t.getEndTime().getSeconds()).append("</td><td>").append(t.getLastErrorCode()).append("</td><td>").append(t.getLastErrorMsg()).append("</td></tr>").toString());
                        if (wtype == EJLMWidgetType.SOURCE)
                            System.out.println((new StringBuilder()).append("<tr><td>").append(t.getWidgetName()).append("</td><td>").append(t.getAppliedRowsNum()).append("</td><td>").append(t.getAffectedRowsNum()).append("</td><td>").append(t.getRejectedRowsNum()).append("</td><td>").append(t.getThroughput()).append("</td><td>").append(t.getStartTime().getMonth()).append("/").append(t.getStartTime().getDate()).append("/").append(t.getStartTime().getYear()).append(" ").append(t.getStartTime().getHours()).append(":").append(t.getStartTime().getMinutes()).append(":").append(t.getStartTime().getSeconds()).append("</td><td>").append(t.getEndTime().getMonth()).append("/").append(t.getEndTime().getDate()).append("/").append(t.getEndTime().getYear()).append(" ").append(t.getEndTime().getHours()).append(":").append(t.getEndTime().getMinutes()).append(":").append(t.getEndTime().getSeconds()).append("</td><td>").append(t.getLastErrorCode()).append("</td><td>").append(t.getLastErrorMsg()).append("</td></tr>").toString());
                        if (wtype == EJLMWidgetType.XMLDSQ)
                            System.out.println((new StringBuilder()).append("<tr><td>").append(t.getWidgetName()).append("</td><td>").append(t.getAppliedRowsNum()).append("</td><td>").append(t.getAffectedRowsNum()).append("</td><td>").append(t.getRejectedRowsNum()).append("</td><td>").append(t.getThroughput()).append("</td><td>").append(t.getStartTime().getMonth()).append("/").append(t.getStartTime().getDate()).append("/").append(t.getStartTime().getYear()).append(" ").append(t.getStartTime().getHours()).append(":").append(t.getStartTime().getMinutes()).append(":").append(t.getStartTime().getSeconds()).append("</td><td>").append(t.getEndTime().getMonth()).append("/").append(t.getEndTime().getDate()).append("/").append(t.getEndTime().getYear()).append(" ").append(t.getEndTime().getHours()).append(":").append(t.getEndTime().getMinutes()).append(":").append(t.getEndTime().getSeconds()).append("</td><td>").append(t.getLastErrorCode()).append("</td><td>").append(t.getLastErrorMsg()).append("</td></tr>").toString());
                        if (wtype == EJLMWidgetType.ERPDSQ)
                            System.out.println((new StringBuilder()).append("<tr><td>").append(t.getWidgetName()).append("</td><td>").append(t.getAppliedRowsNum()).append("</td><td>").append(t.getAffectedRowsNum()).append("</td><td>").append(t.getRejectedRowsNum()).append("</td><td>").append(t.getThroughput()).append("</td><td>").append(t.getStartTime().getMonth()).append("/").append(t.getStartTime().getDate()).append("/").append(t.getStartTime().getYear()).append(" ").append(t.getStartTime().getHours()).append(":").append(t.getStartTime().getMinutes()).append(":").append(t.getStartTime().getSeconds()).append("</td><td>").append(t.getEndTime().getMonth()).append("/").append(t.getEndTime().getDate()).append("/").append(t.getEndTime().getYear()).append(" ").append(t.getEndTime().getHours()).append(":").append(t.getEndTime().getMinutes()).append(":").append(t.getEndTime().getSeconds()).append("</td><td>").append(t.getLastErrorCode()).append("</td><td>").append(t.getLastErrorMsg()).append("</td></tr>").toString());
                        if (wtype == EJLMWidgetType.MQDSQ)
                            System.out.println((new StringBuilder()).append("<tr><td>").append(t.getWidgetName()).append("</td><td>").append(t.getAppliedRowsNum()).append("</td><td>").append(t.getAffectedRowsNum()).append("</td><td>").append(t.getRejectedRowsNum()).append("</td><td>").append(t.getThroughput()).append("</td><td>").append(t.getStartTime().getMonth()).append("/").append(t.getStartTime().getDate()).append("/").append(t.getStartTime().getYear()).append(" ").append(t.getStartTime().getHours()).append(":").append(t.getStartTime().getMinutes()).append(":").append(t.getStartTime().getSeconds()).append("</td><td>").append(t.getEndTime().getMonth()).append("/").append(t.getEndTime().getDate()).append("/").append(t.getEndTime().getYear()).append(" ").append(t.getEndTime().getHours()).append(":").append(t.getEndTime().getMinutes()).append(":").append(t.getEndTime().getSeconds()).append("</td><td>").append(t.getLastErrorCode()).append("</td><td>").append(t.getLastErrorMsg()).append("</td></tr>").toString());
                        if (wtype == EJLMWidgetType.MAPPLET)
                            System.out.println((new StringBuilder()).append("<tr><td>").append(t.getWidgetName()).append("</td><td>").append(t.getAppliedRowsNum()).append("</td><td>").append(t.getAffectedRowsNum()).append("</td><td>").append(t.getRejectedRowsNum()).append("</td><td>").append(t.getThroughput()).append("</td><td>").append(t.getStartTime().getMonth()).append("/").append(t.getStartTime().getDate()).append("/").append(t.getStartTime().getYear()).append(" ").append(t.getStartTime().getHours()).append(":").append(t.getStartTime().getMinutes()).append(":").append(t.getStartTime().getSeconds()).append("</td><td>").append(t.getEndTime().getMonth()).append("/").append(t.getEndTime().getDate()).append("/").append(t.getEndTime().getYear()).append(" ").append(t.getEndTime().getHours()).append(":").append(t.getEndTime().getMinutes()).append(":").append(t.getEndTime().getSeconds()).append("</td><td>").append(t.getLastErrorCode()).append("</td><td>").append(t.getLastErrorMsg()).append("</td></tr>").toString());
                        if (wtype == EJLMWidgetType.DATADRIVEN)
                            System.out.println((new StringBuilder()).append("<tr><td>").append(t.getWidgetName()).append("</td><td>").append(t.getAppliedRowsNum()).append("</td><td>").append(t.getAffectedRowsNum()).append("</td><td>").append(t.getRejectedRowsNum()).append("</td><td>").append(t.getThroughput()).append("</td><td>").append(t.getStartTime().getMonth()).append("/").append(t.getStartTime().getDate()).append("/").append(t.getStartTime().getYear()).append(" ").append(t.getStartTime().getHours()).append(":").append(t.getStartTime().getMinutes()).append(":").append(t.getStartTime().getSeconds()).append("</td><td>").append(t.getEndTime().getMonth()).append("/").append(t.getEndTime().getDate()).append("/").append(t.getEndTime().getYear()).append(" ").append(t.getEndTime().getHours()).append(":").append(t.getEndTime().getMinutes()).append(":").append(t.getEndTime().getSeconds()).append("</td><td>").append(t.getLastErrorCode()).append("</td><td>").append(t.getLastErrorMsg()).append("</td></tr>").toString());
                        if (wtype == EJLMWidgetType.OUTPUT)
                            System.out.println((new StringBuilder()).append("<tr><td>").append(t.getWidgetName()).append("</td><td>").append(t.getAppliedRowsNum()).append("</td><td>").append(t.getAffectedRowsNum()).append("</td><td>").append(t.getRejectedRowsNum()).append("</td><td>").append(t.getThroughput()).append("</td><td>").append(t.getStartTime().getMonth()).append("/").append(t.getStartTime().getDate()).append("/").append(t.getStartTime().getYear()).append(" ").append(t.getStartTime().getHours()).append(":").append(t.getStartTime().getMinutes()).append(":").append(t.getStartTime().getSeconds()).append("</td><td>").append(t.getEndTime().getMonth()).append("/").append(t.getEndTime().getDate()).append("/").append(t.getEndTime().getYear()).append(" ").append(t.getEndTime().getHours()).append(":").append(t.getEndTime().getMinutes()).append(":").append(t.getEndTime().getSeconds()).append("</td><td>").append(t.getLastErrorCode()).append("</td><td>").append(t.getLastErrorMsg()).append("</td></tr>").toString());
                        if (wtype == EJLMWidgetType.TARGET)
                            System.out.println((new StringBuilder()).append("<tr><td>").append(t.getWidgetName()).append("</td><td>").append(t.getAppliedRowsNum()).append("</td><td>").append(t.getAffectedRowsNum()).append("</td><td>").append(t.getRejectedRowsNum()).append("</td><td>").append(t.getThroughput()).append("</td><td>").append(t.getStartTime().getMonth()).append("/").append(t.getStartTime().getDate()).append("/").append(t.getStartTime().getYear()).append(" ").append(t.getStartTime().getHours()).append(":").append(t.getStartTime().getMinutes()).append(":").append(t.getStartTime().getSeconds()).append("</td><td>").append(t.getEndTime().getMonth()).append("/").append(t.getEndTime().getDate()).append("/").append(t.getEndTime().getYear()).append(" ").append(t.getEndTime().getHours()).append(":").append(t.getEndTime().getMinutes()).append(":").append(t.getEndTime().getSeconds()).append("</td><td>").append(t.getLastErrorCode()).append("</td><td>").append(t.getLastErrorMsg()).append("</td></tr>").toString());
                    }

                }
            }
            System.out.println("</table>");
        }

        outStr.close();
        newps.close();
        System.setOut(ops);
    }

    public void printtaskstatus()
        throws JLMException, IOException
    {
        getwfresults();
        savewfresults(tmplogfile);
        IJLMWorkflowTasks succeedTasks = wk.getWorkflowTasksByStatus(EJLMTaskStatus.SUCCEEDED, null);
        if(succeedTasks.getTopLevelParents() != null)
        {
            getfailsesslog(succeedTasks);
            printlogs(succeedTasks);
            savelogs(succeedTasks, tmplogfile);
            closefile(tmplogfile, "</table>");
        }
        IJLMWorkflowTasks waitingTasks = wk.getWorkflowTasksByStatus(EJLMTaskStatus.WAITING, null);
        if(waitingTasks.getTopLevelParents() != null)
        {
            getfailsesslog(waitingTasks);
            printlogs(waitingTasks);
            savelogs(waitingTasks, tmplogfile);
            closefile(tmplogfile, "</table>");
        }
        IJLMWorkflowTasks runningTasks = wk.getWorkflowTasksByStatus(EJLMTaskStatus.RUNNING, null);
        if(runningTasks.getTopLevelParents() != null)
        {
            getfailsesslog(runningTasks);
            printlogs(runningTasks);
            savelogs(runningTasks, tmplogfile);
            closefile(tmplogfile, "</table>");
        }
        IJLMWorkflowTasks abortTasks = wk.getWorkflowTasksByStatus(EJLMTaskStatus.ABORTING, null);
        if(abortTasks.getTopLevelParents() != null)
        {
            getfailsesslog(abortTasks);
            printlogs(abortTasks);
            savelogs(abortTasks, tmplogfile);
            closefile(tmplogfile, "</table>");
        }
        IJLMWorkflowTasks disableTasks = wk.getWorkflowTasksByStatus(EJLMTaskStatus.DISABLED, null);
        if(disableTasks.getTopLevelParents() != null)
        {
            getfailsesslog(disableTasks);
            printlogs(disableTasks);
            savelogs(disableTasks, tmplogfile);
            closefile(tmplogfile, "</table>");
        }
        IJLMWorkflowTasks failTasks = wk.getWorkflowTasksByStatus(EJLMTaskStatus.FAILED, null);
        if(failTasks.getTopLevelParents() != null)
        {
            getfailsesslog(failTasks);
            printlogs(failTasks);
            savelogs(failTasks, tmplogfile);
            closefile(tmplogfile, "</table>");
        }
        IJLMWorkflowTasks stoppedTasks = wk.getWorkflowTasksByStatus(EJLMTaskStatus.STOPPED, null);
        if(stoppedTasks.getTopLevelParents() != null)
        {
            getfailsesslog(stoppedTasks);
            printlogs(stoppedTasks);
            savelogs(stoppedTasks, tmplogfile);
            closefile(tmplogfile, "</table>");
        }
        IJLMWorkflowTasks stoppingTasks = wk.getWorkflowTasksByStatus(EJLMTaskStatus.STOPPING, null);
        if(stoppingTasks.getTopLevelParents() != null)
        {
            getfailsesslog(stoppingTasks);
            printlogs(stoppingTasks);
            savelogs(stoppingTasks, tmplogfile);
            closefile(tmplogfile, "</table>");
        }
        IJLMWorkflowTasks suspendedTasks = wk.getWorkflowTasksByStatus(EJLMTaskStatus.SUSPENDED, null);
        if(suspendedTasks.getTopLevelParents() != null)
        {
            getfailsesslog(suspendedTasks);
            printlogs(suspendedTasks);
            savelogs(suspendedTasks, tmplogfile);
            closefile(tmplogfile, "</table>");
        }
        IJLMWorkflowTasks suspendingTasks = wk.getWorkflowTasksByStatus(EJLMTaskStatus.SUSPENDING, null);
        if(suspendingTasks.getTopLevelParents() != null)
        {
            printlogs(suspendingTasks);
            savelogs(suspendingTasks, tmplogfile);
            closefile(tmplogfile, "</table>");
            getfailsesslog(suspendingTasks);
        }
        IJLMWorkflowTasks termTasks = wk.getWorkflowTasksByStatus(EJLMTaskStatus.TERMINATED, null);
        if(termTasks.getTopLevelParents() != null)
        {
            getfailsesslog(termTasks);
            printlogs(termTasks);
            savelogs(termTasks, tmplogfile);
            closefile(tmplogfile, "</table>");
        }
        IJLMWorkflowTasks unkownTasks = wk.getWorkflowTasksByStatus(EJLMTaskStatus.UNKNOWN, null);
        if(unkownTasks.getTopLevelParents() != null)
        {
            getfailsesslog(unkownTasks);
            printlogs(unkownTasks);
            savelogs(unkownTasks, tmplogfile);
            closefile(tmplogfile, "</table>");
        }
        closefile(tmplogfile, "</body></html>");
    }

    public void printlogs(IJLMWorkflowTasks objstatus)
        throws JLMException, IOException
    {
        IJLMTaskTopLevelParent taskpar[] = objstatus.getTopLevelParents();
        IJLMTaskTopLevelParent arr$[] = taskpar;
        int len$ = arr$.length;
        for(int i$ = 0; i$ < len$; i$++)
        {
            IJLMTaskTopLevelParent p = arr$[i$];
            String taskname = p.getTaskInstanceName();
            try
            {
                task = wk.getTask(null, taskname);
            }
            catch(JLMException te)
            {
                if(te.getErrorCode() == -212)
                {
                    IJLMTaskDetails taskDet = task.getTaskDetails(objstatus);
                    EJLMTaskType tt = taskDet.getTaskType();
                    String tasktype = tt.toString();
                    if(tasktype.equalsIgnoreCase("worklet_task"))
                    {
                        taskname = (new StringBuilder()).append(p.getTopLevelParentName()).append(".").append(p.getTaskInstanceName()).toString();
                        task.setTaskInstPath((new StringBuilder()).append(p.getTopLevelParentName()).append(".").append(p.getTaskInstanceName()).toString());
                        task = wk.getTask(null, taskname);
                    }
                }
            }
            IJLMTaskDetails taskDet = task.getTaskDetails(objstatus);
            if(!taskDet.isSessionType())
                continue;
            IJLMSession session = task.getSession(task);
            IJLMSessionDetails sessionDetails = session.getSessionDetails(task);
            IJLMTableStat stats[] = sessionDetails.getTableStatistics();
            if(!sessionDetails.getTaskStatus().toString().equalsIgnoreCase("SUCCEEDED"))
            {
                System.out.println("");
                System.out.println((new StringBuilder()).append("Session [").append(taskname).append("] details").toString());
                System.out.println("--------------------------------------------------------");
                sessionDetails.getLogFilePath();
                System.out.println((new StringBuilder()).append("Session log location     :             ").append(sessionDetails.getLogFilePath()).toString());
                System.out.println((new StringBuilder()).append("Session Status           :             ").append(sessionDetails.getTaskStatus().toString().toUpperCase()).toString());
                System.out.println((new StringBuilder()).append("Src Success Rows         :             ").append(sessionDetails.getNumSrcSuccessRows()).toString());
                System.out.println((new StringBuilder()).append("Src Failed Rows          :             ").append(sessionDetails.getNumSrcFailedRows()).toString());
                System.out.println((new StringBuilder()).append("Tgt Success Rows         :             ").append(sessionDetails.getNumTgtSuccessRows()).toString());
                System.out.println((new StringBuilder()).append("Tgt Failed Rows          :             ").append(sessionDetails.getNumTgtFailedRows()).toString());
                System.out.println((new StringBuilder()).append("Session Error Message    :             ").append(taskDet.getRunErrorMsg()).toString());
                System.out.println("--------------------------------------------------------");
                System.out.println("");
            }
        }

    }

    public void getfailsesslog(IJLMWorkflowTasks objstatus)
        throws JLMException, IOException
    {
        PrintStream ops = System.out;
        IJLMTaskTopLevelParent taskpar[] = objstatus.getTopLevelParents();
        IJLMTaskTopLevelParent arr$[] = taskpar;
        int len$ = arr$.length;
        for(int i$ = 0; i$ < len$; i$++)
        {
            IJLMTaskTopLevelParent p = arr$[i$];
            String taskname = p.getTaskInstanceName();
            try
            {
                task = wk.getTask(null, taskname);
            }
            catch(JLMException te)
            {
                if(te.getErrorCode() == -212)
                {
                    IJLMTaskDetails taskDet = task.getTaskDetails(objstatus);
                    EJLMTaskType tt = taskDet.getTaskType();
                    String tasktype = tt.toString();
                    if(tasktype.equalsIgnoreCase("worklet_task"))
                    {
                        taskname = (new StringBuilder()).append(p.getTopLevelParentName()).append(".").append(p.getTaskInstanceName()).toString();
                        task.setTaskInstPath((new StringBuilder()).append(p.getTopLevelParentName()).append(".").append(p.getTaskInstanceName()).toString());
                        task = wk.getTask(null, taskname);
                    }
                }
            }
            IJLMTaskDetails taskDet = task.getTaskDetails(objstatus);
            if(!taskDet.isSessionType())
                continue;
            IJLMSession session = task.getSession(task);
            IJLMSessionDetails sessionDetails = session.getSessionDetails(task);
            if(sessionDetails.getTaskStatus().toString().equalsIgnoreCase("SUCCEEDED"))
                continue;
            MySessionLogSegmentCallback logSegmentCallback = new MySessionLogSegmentCallback();
            if(session == null)
                continue;
            File logsessionfile = new File((new StringBuilder()).append(PMLOGDIR).append(taskname).append(".log").toString());
            if(logsessionfile.exists())
                logsessionfile.delete();
            logsessionfile.createNewFile();
            FileOutputStream outStr = new FileOutputStream(logsessionfile, true);
            PrintStream newps1 = new PrintStream(outStr);
            System.setOut(newps1);
            try
            {
                session.startSessionLogFetch(logSegmentCallback, null, null);
            }
            catch(JLMException ex)
            {
                ex.printStackTrace();
            }
            outStr.close();
            newps1.close();
            System.setOut(ops);
        }

    }

    public void postMail(String Status)
    {
        boolean debug;
        debug = false;
        if(SMTPEMAILHOST.length() == 0)
            return;
        try
        {
            Properties props = new Properties();
            props.put("mail.smtp.host", SMTPEMAILHOST);
            props.put("mail.smtp.port", SMTPEMAILPORT);
            props.put("mail.smtp.user", SMTPEMAILUSER);
            Session mailsession = Session.getDefaultInstance(props, null);
            mailsession.setDebug(debug);
            Message msg = new MimeMessage(mailsession);
            InternetAddress addressFrom = new InternetAddress(SMTPEMAILFROM);
            msg.setFrom(addressFrom);
            InternetAddress addressTo[] = new InternetAddress[recip.length];
            for(int i = 0; i < recip.length; i++)
                addressTo[i] = new InternetAddress(recip[i]);

            msg.setRecipients(javax.mail.Message.RecipientType.TO, addressTo);
            msg.addHeader("X-Mailer", "sendhtml");
            msg.setSubject((new StringBuilder()).append("Workflow [").append(WorkflowName).append("] has ").append(Status).append(" in folder [").append(FolderName).append("] on ").append(hostname()).toString());
            StringBuffer contents = new StringBuffer();
            BufferedReader reader = null;
            reader = new BufferedReader(new FileReader(tmplogfile));
            for(String message = null; (message = reader.readLine()) != null;)
                contents.append(message).append(System.getProperty("line.separator"));

            reader.close();
            msg.setContent(contents.toString(), "text/html");
            Transport.send(msg);
        }
        catch(MessagingException m)
        {
            m.printStackTrace();
        }
        catch(FileNotFoundException e)
        {
            e.printStackTrace();
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
        return;
    }

    public static void main(String args[])
    {
        CommandLineParser parser = new BasicParser();
        Options options = new Options();

        options.addOption("h", "help", false, "Print this usage information");
        options.addOption(OptionBuilder.isRequired(true).withDescription("Folder name where workflow is located")
                .hasArg(true).withArgName("foldername").withLongOpt("foldername").create("f"));
        options.addOption(OptionBuilder.isRequired(true).hasArg(true).withDescription("Workflow name to be executed")
                .withArgName("workflowname").withLongOpt("workflowname").create("w"));
        options.addOption(OptionBuilder.isRequired(false).hasArg(true)
                .withDescription("Start a single session in named workflow").withArgName("session").withLongOpt("session").create("s"));
        options.addOption(OptionBuilder.isRequired(false).hasArg(true)
                .withDescription("Start from a session in named workflow").withArgName("fromsession").withLongOpt("fromsession").create("sf"));
        options.addOption(OptionBuilder.isRequired(true).hasArg(true)
                .withDescription("start|recover|stop|abort workflow").withArgName("restart").withLongOpt("restart").create("r"));
        options.addOption(OptionBuilder.isRequired(false).hasArg(true)
                .withDescription("Full path and name to properties file").withArgName("propfilename").withLongOpt("propfilename").create("prop"));
        options.addOption(OptionBuilder.isRequired(false).hasArg(true)
                .withDescription("Email List to send error log").withArgName("email").withLongOpt("email").create("e"));
        options.addOption(OptionBuilder.isRequired(false).hasArg(true)
                .withDescription("Workflow run instance name").withArgName("wfinstancename").withLongOpt("wfinstancename").create("ri"));
        options.addOption(OptionBuilder.isRequired(false).hasArg(true)
                .withDescription("Parameter File Name").withArgName("parameterfile").withLongOpt("parameterfile").create("pf"));
        options.addOption(OptionBuilder.isRequired(false).hasArg(true)
                .withDescription("Parameter 1").withArgName("parm01").withLongOpt("parm01").create("p01"));
        options.addOption(OptionBuilder.isRequired(false).hasArg(true)
                .withDescription("Parameter 2").withArgName("parm02").withLongOpt("parm02").create("p02"));
        options.addOption(OptionBuilder.isRequired(false).hasArg(true)
                .withDescription("Parameter 3").withArgName("parm03").withLongOpt("parm03").create("p03"));
        options.addOption(OptionBuilder.isRequired(false).hasArg(true)
                .withDescription("Parameter 4").withArgName("parm04").withLongOpt("parm04").create("p04"));
        options.addOption(OptionBuilder.isRequired(false).hasArg(true)
                .withDescription("Parameter 5").withArgName("parm05").withLongOpt("parm05").create("p05"));
        options.addOption(OptionBuilder.isRequired(false).hasArg(true)
                .withDescription("Parameter 6").withArgName("parm06").withLongOpt("parm06").create("p06"));
        options.addOption(OptionBuilder.isRequired(false).hasArg(true)
                .withDescription("Parameter 7").withArgName("parm07").withLongOpt("parm07").create("p07"));
        options.addOption(OptionBuilder.isRequired(false).hasArg(true)
                .withDescription("Parameter 8").withArgName("parm08").withLongOpt("parm08").create("p08"));
        options.addOption(OptionBuilder.isRequired(false).hasArg(true)
                .withDescription("Parameter 9").withArgName("parm09").withLongOpt("parm09").create("p09"));
        options.addOption(OptionBuilder.isRequired(false).hasArg(true)
                .withDescription("Parameter 10").withArgName("parm10").withLongOpt("parm10").create("p10"));
        options.addOption("d", false, "Optional. List server details in console log");

        try
        {
            CommandLine commandLine = parser.parse(options, args);
            if(commandLine.hasOption("h"))
            {
                printUsage(options);
                System.exit(0);
            }
            if(commandLine.hasOption("f"))
                FolderName = commandLine.getOptionValue("f");
            else
                FolderName = "";
            if(commandLine.hasOption("w"))
                WorkflowName = commandLine.getOptionValue("w");
            else
                WorkflowName = "";
            if(commandLine.hasOption("s"))
                sessionname = commandLine.getOptionValue("s");
            else
                sessionname = "";
            if(commandLine.hasOption("sf"))
                sessionfromname = commandLine.getOptionValue("sf");
            else
                sessionfromname = "noinput";
            if(commandLine.hasOption("r"))
                restart = commandLine.getOptionValue("r");
            else
                restart = "";
            if(commandLine.hasOption("e"))
                emaillist = commandLine.getOptionValue("e");
            else
                emaillist = "";
            if(commandLine.hasOption("ri"))
                wfinstancename = commandLine.getOptionValue("ri");
            else
                wfinstancename = "";
            if(commandLine.hasOption("prop"))
                propfilename = commandLine.getOptionValue("prop");
            else
                propfilename = "";
            if(commandLine.hasOption("pf"))
                ParameterFile = commandLine.getOptionValue("pf");
            else
                ParameterFile = "none";
            if(commandLine.hasOption("parm01"))
                parm01 = commandLine.getOptionValue("parm01");
            else
                parm01 = "none";
            if(commandLine.hasOption("parm02"))
                parm02 = commandLine.getOptionValue("parm02");
            else
                parm02 = "none";
            if(commandLine.hasOption("parm03"))
                parm03 = commandLine.getOptionValue("parm03");
            else
                parm03 = "none";
            if(commandLine.hasOption("parm04"))
                parm04 = commandLine.getOptionValue("parm04");
            else
                parm04 = "none";
            if(commandLine.hasOption("parm05"))
                parm05 = commandLine.getOptionValue("parm05");
            else
                parm05 = "none";
            if(commandLine.hasOption("parm06"))
                parm06 = commandLine.getOptionValue("parm06");
            else
                parm06 = "none";
            if(commandLine.hasOption("parm07"))
                parm07 = commandLine.getOptionValue("parm07");
            else
                parm07 = "none";
            if(commandLine.hasOption("parm08"))
                parm08 = commandLine.getOptionValue("parm08");
            else
                parm08 = "none";
            if(commandLine.hasOption("parm09"))
                parm09 = commandLine.getOptionValue("parm09");
            else
                parm09 = "none";
            if(commandLine.hasOption("parm10"))
                parm10 = commandLine.getOptionValue("parm10");
            else
                parm10 = "none";
            if(commandLine.hasOption("d"))
                SRVDET = true;
            else
                SRVDET = false;
        }
        catch(MissingOptionException e)
        {
            System.err.println((new StringBuilder()).append("\nMissing Options \n").append(e.getMissingOptions()).toString());
            printUsage(options);
            System.exit(WKUNKNOWN);
        }
        catch(MissingArgumentException a)
        {
            System.err.println(a.getOption());
        }
        catch(ParseException e)
        {
            e.printStackTrace();
        }
        ExecuteWorkflow ExecuteWorkflow = new ExecuteWorkflow();
        try
        {
            ExecuteWorkflow.execute();
        }
        catch(Exception s)
        {
            s.printStackTrace();
        }
    }

    private static int wkStatusRes = 99;
    private static String DOMAINNAME = null;
    private static String DISERVICENAME = null;
    private static String REPOSITORYNAME = null;
    private static String USERNAME = null;
    private static String PASSWORD = null;
    private static String EMAIL = null;
    private static String PMLOGDIR = null;
    private static String SMTPEMAILFROM = null;
    private static String SMTPEMAILHOST = null;
    private static String SMTPEMAILPORT = null;
    private static String SMTPEMAILUSER = null;
    private static String FolderName = null;
    private static String WorkflowName = null;
    private static String wfinstancename = "";
    private static String ParameterFile = "none";
    private static int CntParam = 0;
    private static String taskstatus = null;
    private static String propfilename = null;
    private static String sessionname = null;
    private static String sessionfromname = "noinput";
    private static Boolean runasrdw = Boolean.valueOf(false);
    private static String restart = null;
    private static String parm01 = "none";
    private static String parm02 = "none";
    private static String parm03 = "none";
    private static String parm04 = "none";
    private static String parm05 = "none";
    private static String parm06 = "none";
    private static String parm07 = "none";
    private static String parm08 = "none";
    private static String parm09 = "none";
    private static String parm10 = "none";
    private static String tmpdir = System.getProperty("java.io.tmpdir");
    private static String fs = System.getProperty("file.separator");
    public static final String DATE_FORMAT_NOW = "yyyyMMddHHmmss";
    public static String SessErrlogName = null;
    public static String hostname = null;
    public static File tmplogfile = null;
    public static File TmpParameterFile = null;
    public static String emaillist = null;
    public static String jobname = null;
    public static String instance = "1";
    String recip[];
    public static String formatstr = "|%1$-20s|%2$-12s|%3$-13s|%4$-13s|%5$-20s|%6$-18s|%7$-18s|%8$-15s|%9$-20s|\n";
    public static String sepformatstr = "|%1$-20s/%2$-12s/%3$-13s/%4$-13s/%5$-20s/%6$-18s/%7$-18s/%8$-15s/%9$-20s/\n";
    private static int WKUNKNOWN = 99;
    private static int WKSUSPEND = 5;
    private static int WKABORT = 4;
    private static int WKFAIL = 3;
    private static int WKSTOP = 2;
    private static int WKTERM = 1;
    private static int WKSUCEED = 0;
    public static boolean SRVDET = false;
    IJLMWorkflow wk;
    IJLMTask Task;
    IJLMServerDetails srvdetails;
    EJLMWorkflowStatus wkStatus;
    IJLMWorkflowDetails wkDetails;
    IJLMConnection connection;
    IJLMDriver ld;
    IJLMWorkflowTasks taskrunstatus;
    IJLMTask task;
    IJLMTaskDetails taskDetails;
    IJLMTaskDetails taskDet;
    IJLMSessionDetails sessionDetails;
    private static final String USAGE = "[-h Help] [-f foldername] [-w workflowname] [-r start|stop|recover|abort|force] [-s session] [-sf fromsession] [-p01-p10 Parameters] [-e email] [-pf parameterfile] [-pr propfilename]";
    private static final String HEADER = "\nexecuteworkflow\n\n";
    private static final String FOOTER = "";

}
