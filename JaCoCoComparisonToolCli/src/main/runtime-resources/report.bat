@echo off

rem parse report.conf
FOR /F "tokens=1,2 delims==" %%G IN (report.conf) DO (set %%G=%%H)

rem removing double qoutes
set sourceFileDir=%sourceFileDir:"=%
set classFileDir=%classFileDir:"=%
set reports=%reports:"=%
set execfile[0]=%execfile[0]:"=%
set execfile[1]=%execfile[1]:"=%
set packages=%packages:"=%
set titles=%titles:"=%

IF NOT "%packages%"=="" SET package=--package %packages%
IF NOT "%titles%"=="" SET title=--title %titles%

IF "%JAVA_HOME%"=="" SET LOCAL_JAVA=java
IF NOT "%JAVA_HOME%"=="" SET LOCAL_JAVA=%JAVA_HOME%\bin\java

%LOCAL_JAVA% -Dlogback.configurationFile=logback.xml -jar ${project.build.finalName}.${project.packaging}

echo '%LOCAL_JAVA% -Dlogback.configurationFile=logback.xml -jar ${project.build.finalName}.${project.packaging} --sourceFileDir %sourceFileDir% --classFileDir %classFileDir% --report %reports% --exec %execfile[0]%,%execfile[1]% %package% %title%'
%LOCAL_JAVA% -Dlogback.configurationFile=logback.xml -jar ${project.build.finalName}.${project.packaging} --sourceFileDir %sourceFileDir% --classFileDir %classFileDir% --report %reports% --exec %execfile[0]%,%execfile[1]% %package% %title%

