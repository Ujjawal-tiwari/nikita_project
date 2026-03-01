@echo off
echo =======================================================
echo Starting Credit Risk Explanation Service...
echo =======================================================

echo.
echo Setting JAVA_HOME to JDK 17...
set "JAVA_HOME=c:\Program Files\Microsoft\jdk-17.0.18.8-hotspot"

echo.
echo Launching the application...
echo (You can keep this window open. Every time you save a Java file in your IDE, 
echo the application will automatically restart thanks to Spring Boot DevTools!)
echo.

c:\Users\KIIT\apache-maven-3.9.6\bin\mvn.cmd spring-boot:run
