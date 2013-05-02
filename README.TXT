Description:
A Java utility that uses the Informatica Operations API allowing parameter inputs, trapping of suspended workflows and ability to send an email on failure. This utility extends the functionality of the pmcmd startworkflow and starttask command. If you pass in a parameter file and individual parameters on the command line, a temporary parameter file is created that has the values from the parameter file and appends the individual parameters. The e-mail sent is in HTML format using tables and looks similar to the workflow monitor output.

Features:
  -Written in java using Informatica Operations API.
  -Platform independent.
  -Can be executed on any OS supported by Informatica SDK without installing the PowerCenter server or client software.
  -Pass in up to 10 individual name-value pair parameters enclosed with "" that will be used in the workflow (ie "$$Parameter=value")
  -Individual parameters can be appended to an existing parameter file.
  -Ability to send an email to comma delimited list of addresses on workflow failures using SMTP.
  -Traps a suspended workflow and fails the command.
  -Use of a default properties file to define server names, authentication and email settings.
  
V1.2
	Bug Fixes:
		-The "-prop" parameter was not recognizing the value of parameter file being passed in.
	
	Enhancements:		
		-Recompiled and re-orgamized to not use classes and call jar file directly. This allows easier updates to the program by just releasing the jar file. 
		-Moved infa.properties to root directory for easier access to modifcations.
		-New parameter "-ri" to pass in a workflow instance name (this can only be used for concurrent workflows).
		-New flag "-d" to enable server details (this was displaying by default before). If -d is not used, no server details will be displayed on the console during execution.
V1.1
	Bug Fixes:
		-Fixed a java error when a worklet fails in a workflow. 
V1.0
	Initial Release	