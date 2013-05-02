#!/bin/sh

cd `dirname $0`
export FDS_EXECWF_HOME=`pwd`
export SCRIPTNAME=`basename $0`

#export JAVA_HOME=/opt/informatica/INFA91/java/bin
#export INFA_DOMAINS_FILE=/opt/informatica/INFA91/domains.infa

export JAVA_HOME=$JAVA_HOME
export INFA_DOMAINS_FILE=$INFA_DOMAINS_FILE
export INFA_HOME=${FDS_EXECWF_HOME}/lib
export LIBPATH=${INFA_HOME}:/usr/lib:/lib

cd ${FDS_EXECWF_HOME}/lib

if [ -z "$JAVA_HOME" ] 
then 
	print "Please set JAVA_HOME Environment variable in system settings or in $SCRIPTNAME"
	exit 1
fi

if [ -z "$INFA_DOMAINS_FILE" ] 
then 
	print "Please set INFA_DOMAINS_FILE Environment variable in system settings or in $SCRIPTNAME"
	exit 2
fi

export CLASSPATH=.:..:lib/:lib/log4j-1.2.8.jar:lib/jlmapi.jar:lib/pmserversdk.jar:lib/mail.jar:lib/activation.jar:lib/smtp.jar:lib/commons-cli-1.2.jar:lib/log4j.properties:infa.properties:lib/locale/pmlocale.bin:${INFA_HOME}/

export PATH=${CLASSPATH}:${INFA_HOME}:${PATH}:${JAVA_HOME}:${INFA_DOMAINS_FILE}

${JAVA_HOME}/java -Djava.library.path=${INFA_HOME}:${FDS_EXECWF_HOME}:${FDS_EXECWF_HOME}/lib -cp ${CLASSPATH} -jar ../ExecuteWorkflow.jar $@
exit $?
