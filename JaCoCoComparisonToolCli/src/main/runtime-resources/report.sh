#!/bin/bash
#

declare JAVA_BINARY

if [[ -z $JAVA_HOME ]] ; then
  JAVA_BINARY=java
else
  JAVA_BINARY="$JAVA_HOME/bin/java"
fi

declare MY_DIR

MY_DIR=`dirname $0`

source ./report.conf

if [ ! -z "$packages" ]
then
	package=$(echo "--package $packages")
fi

if [ ! -z "$titles" ]
then
	title=$(echo "--title $titles")
fi

echo '$JAVA_BINARY -Dlogback.configurationFile="${MY_DIR}/logback.xml" -jar "${MY_DIR}/${project.build.finalName}.${project.packaging}" --sourceFileDir $sourceFileDir --classFileDir $classFileDir --report $reports --exec ${execfile[0]},${execfile[1]} $package $title'
$JAVA_BINARY -Dlogback.configurationFile="${MY_DIR}/logback.xml" -jar "${MY_DIR}/${project.build.finalName}.${project.packaging}" --sourceFileDir $sourceFileDir --classFileDir $classFileDir --report $reports --exec ${execfile[0]},${execfile[1]} $package $title
