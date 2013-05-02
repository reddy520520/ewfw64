@ECHO OFF
REM Modify the below path to where you uncompressed the files.
REM set JAVA_HOME="C:\Program Files\Java\jre1.5.0_22"
REM set INFA_DOMAINS_FILE=C:\Informatica\9.1.0\clients\PowerCenterClient\domains.infa

SET JAVA_HOME="%JAVA_HOME%"
set INFA_DOMAINS_FILE=%INFA_DOMAINS_FILE%

pushd %~dp0
set EXECWF_HOME="%cd%"
set BATCHNAME=%~nx0

IF NOT DEFINED JAVA_HOME GOTO JAVAERROR
IF NOT DEFINED INFA_DOMAINS_FILE GOTO DOMERROR

set INFA_HOME==%cd%\lib

set CLASSPATH=.;..;lib\;lib\log4j-1.2.8.jar;lib\jlmapi.jar;lib\pmserversdk.jar;lib\mail.jar;lib\activation.jar;lib\smtp.jar;lib\commons-cli-1.2.jar;lib\log4j.properties;infa.properties;lib\locale\pmlocale.bin;%INFA_HOME%

cd /D %INFA_HOME%

set PATH=%CLASSPATH%;%JAVA_HOME%;%INFA_DOMAINS_FILE%;%INFA_HOME%

IF "%*"=="" GOTO HELPOUT

%JAVA_HOME%\bin\java -Djava.library.path=%INFA_HOME%:%EXECWF_HOME% -cp %CLASSPATH% -jar ..\ExecuteWorkflow.jar %*
set retcode=%errorlevel%
exit /b %retcode%

:JAVAERROR
ECHO ERROR: Please set JAVA_HOME Environment variable in system settings or in %BATCHNAME%
pause
exit /b 1

:DOMERROR
ECHO ERROR: Please set INFA_DOMAINS_FILE Environment variable in system settings or in %BATCHNAME%
pause
exit /b 2

:HELPOUT
%JAVA_HOME%\bin\java -Djava.library.path=%INFA_HOME%:%EXECWF_HOME% -cp %CLASSPATH% -jar ..\ExecuteWorkflow.jar -h
pause
exit /b 3