VideoServer-WsApiTest-NoAuth
============================


What is it?
-----------

This is an external TestApp for the Video web service App.

It is implemented in Java 8 using Retrofit.

The project build is based on Gradle.


Prerequisites to test and run
-----------------------------

1. Installation of JDK 1.8
2. Installation of Gradle from
   https://services.gradle.org/distributions/gradle-2.7-bin.zip
3. Include $GRADLE_HOME/bin into your path PATH


How to run this test (from the command line)
--------------------------------------------

$ gradle test -i [ -Dserver.url=<the_servers_url> ]           # runs the test

If you don't specify the server.url the test uses http://localhost:9000 as default.


How to import the project into IntelliJ
---------------------------------------

- Open IntelliJ
- Select "File -> New Project -> Project from Existing Sources"
- Select "Import Project from External Model"
- Select "Gradle"


How to import the project into (Eclipse based) ScalaIDE
-------------------------------------------------------

- Open ScalaIDE
- Select "File -> Import..."
- Select "Gradle -> Gradle Project"
- Select the project directory as "Root Folder" 
- Click "Build Model"
- When the model is built, select the project
- Click "Finish"
