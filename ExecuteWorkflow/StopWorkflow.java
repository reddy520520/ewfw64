// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   StopWorkflow.java

package ExecuteWorkflow;

import com.informatica.powercenter.sdk.lm.*;
import org.apache.commons.cli.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class StopWorkflow
{

    public StopWorkflow()
    {
    }

    private static void printUsage(Options options)
    {
        HelpFormatter helpFormatter = new HelpFormatter();
        helpFormatter.setWidth(160);
        helpFormatter.printHelp("[-h Help] [-f foldername] [-w workflowname] [-s sessionname] [-pr propfilename]",
                "\nexecuteworkflow - A program that executes informatica workflows, Copyright 2010 Michael Wall.",
                options,
                "For more instructions, see the WIKI website at: http://wisdom/display/TechTools/Informatica+PowerCenter");
    }

    public void execute()
    {
        try
        {
            intializeProps(propfilename);
            InitConnection(FolderName, WorkflowName);
            stopwf(WorkflowName);
            disconnect();
        }
        catch(IOException e)
        {
            e.printStackTrace();
            System.exit(-1);
        }
        catch(JLMException f)
        {
            f.printStackTrace();
            System.exit(-1);
        }
    }

    public void intializeProps(String propfilename)
        throws IOException
    {
        Properties props = new Properties();
        if(!propfilename.equalsIgnoreCase(""))
            props.load(new FileInputStream(propfilename));
        else
            props.load(new FileInputStream((new StringBuilder()).append("c:").append(File.separator).append("usr").append(File.separator).append("lib").append(File.separator).append("infa.properties").toString()));
        DISERVICENAME = props.getProperty("DISERVICENAME");
        DOMAINNAME = props.getProperty("DOMAINNAME");
        USERNAME = props.getProperty("USERNAME");
        PASSWORD = props.getProperty("PASSWORD");
        REPOSITORYNAME = props.getProperty("REPOSITORYNAME");
    }

    public void InitConnection(String FolderName, String WorkflowName)
        throws JLMException
    {
        ld = DriverFactory.getDriver("JLMDriver10");
        ld.initialize("lib/locale");
        System.out.println((new StringBuilder()).append("Connecting to as ").append(DOMAINNAME).append(".").append(DISERVICENAME).append(" as user ").append(USERNAME).toString());
        connection = ld.getConnectionEx(DOMAINNAME, DISERVICENAME, 1, null);
        connection.login("", REPOSITORYNAME, USERNAME, PASSWORD, null);
        srvdetails = connection.getServerDetails(EJLMMonitorServerMode.ALL, null);
        System.out.println((new StringBuilder()).append("Server status is: ").append(srvdetails.getStatus()).toString());
        wk = connection.getWorkflow(FolderName, WorkflowName, null);
    }

    private void disconnect()
        throws JLMException
    {
        connection.close(null);
        ld.deinitialize();
    }

    public void stopwf(String WorkflowName)
        throws JLMException
    {
        if(stopwf.equalsIgnoreCase("stop"))
        {
            System.out.println((new StringBuilder()).append("Stopping workflow ").append(WorkflowName).toString());
            wk.stop(false, null);
        } else
        if(stopwf.equalsIgnoreCase("abort"))
        {
            System.out.println((new StringBuilder()).append("Aborting workflow ").append(WorkflowName).toString());
            wk.stop(true, null);
        } else
        {
            System.out.println((new StringBuilder()).append("Invalid option stopping workflow ").append(WorkflowName).append(" use stop or abort").toString());
            System.exit(-1);
        }
        wkDetails = wk.getWorkflowDetails(null);
        wkStatus = wkDetails.getWorkflowRunStatus();
        System.out.println((new StringBuilder()).append("Workflow ").append(WorkflowName).append(" is: ").append(wkStatus.toString()).toString());
    }

    public static void main(String args[])
    {
        CommandLineParser parser = new BasicParser();
        Options options = new Options();
        options.addOption("h", "help", false, "Print this usage information");
        options.addOption(OptionBuilder.isRequired(true).hasArg(true).withDescription("Folder name where workflow is located").withArgName("foldername").withLongOpt("foldername").create("f"));
        options.addOption(OptionBuilder.isRequired(true).hasArg(true).withDescription("Workflow name to be stop").withArgName("workflowname").withLongOpt("workflowname").create("w"));
        options.addOption(OptionBuilder.isRequired(false).hasArg(true).withDescription("Optinal session to be to be stopped in named workflow").withArgName("sessionname").withLongOpt("sessionname").create("s"));
        options.addOption(OptionBuilder.isRequired(true).hasArg(true).withDescription("Stop/Abort workflow").withArgName("stop").withLongOpt("stop").create("a"));
        options.addOption(OptionBuilder.isRequired(false).hasArg(true).withDescription("Full path and name to properties file ").withArgName("propfilename").withLongOpt("propfilename").create("prop"));
        try
        {
            CommandLine commandLine = parser.parse(options, args);
            if(commandLine.hasOption("h"))
            {
                printUsage(options);
                System.exit(0);
            }
            if(commandLine.hasOption("f"))
            {
                FolderName = commandLine.getOptionValue("f");
                FolderName = commandLine.getOptionValue("f");
            } else
            {
                FolderName = "";
            }
            if(commandLine.hasOption("w"))
                WorkflowName = commandLine.getOptionValue("w");
            else
                WorkflowName = "";
            if(commandLine.hasOption("s"))
                sessionname = commandLine.getOptionValue("s");
            else
                sessionname = "";
            if(commandLine.hasOption("a"))
                stopwf = commandLine.getOptionValue("a");
            else
                stopwf = "";
            if(commandLine.hasOption("pr"))
                propfilename = commandLine.getOptionValue("pr");
            else
                propfilename = "";
        }
        catch(MissingOptionException e)
        {
            System.err.println((new StringBuilder()).append("Missing Options ").append(e.getMissingOptions()).toString());
            printUsage(options);
            System.exit(1);
        }
        catch(MissingArgumentException a)
        {
            System.err.println(a.getOption());
        }
        catch(ParseException e)
        {
            e.printStackTrace();
        }
        StopWorkflow StopWorkflow = new StopWorkflow();
        StopWorkflow.execute();
    }

    private static int wkStatusRes = 99;
    private static String DOMAINNAME = null;
    private static String DISERVICENAME = null;
    private static String REPOSITORYNAME = null;
    private static String USERNAME = null;
    private static String PASSWORD = null;
    private static String FolderName = null;
    private static String WorkflowName = null;
    private static String sessionname = null;
    private static String propfilename = null;
    private static String stopwf = null;
    IJLMWorkflow wk;
    IJLMTask Task;
    IJLMServerDetails srvdetails;
    EJLMWorkflowStatus wkStatus;
    IJLMWorkflowDetails wkDetails;
    IJLMConnection connection;
    IJLMDriver ld;
    IJLMWorkflowTasks taskrunstatus;
    private static final String USAGE = "[-h Help] [-f foldername] [-w workflowname] [-s sessionname] [-pr propfilename]";
    private static final String HEADER = "\nexecuteworkflow - A program that executes informatica workflows, Copyright 2010 Michael Wall.";
    private static final String FOOTER = "For more instructions, see the WIKI website at: http://wisdom/display/TechTools/Informatica+PowerCenter";

}
