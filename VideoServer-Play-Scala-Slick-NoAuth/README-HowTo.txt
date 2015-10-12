VideoServer-Play-Scala-Slick-NoAuth
===================================

What is it?
-----------

This simple App is a "Micro-YouTube", a web service application
with a restful JSON API which allows the client to ...

- get a list of all video meta-data
- get a the meta-data of a video by its ID
- add (upload) a video (meta-data and data)
- get (download) the data of a video with a certain ID
- delete a video (meta-data and data) by its ID
- add a rating for a video by its ID
- get the rating of a video by its ID

The App is a small case study using ...

- the Play Framework as containerless Web framework
- Scala as implementation language
- Slick as persistence layer
- No client authentication before accessing videos

In this stage the App doesn't implement any security features.
It offers its interface via HTTP (no HTTPS) and does not require
the user to log in before accessing videos on the server.

The project build is based on SBT.

Prerequisites to test and run
-----------------------------

1. Installation of JDK 1.8
2. Installation of Typesage Activator from
   https://downloads.typesafe.com/typesafe-activator/1.3.6/typesafe-activator-1.3.6-minimal.zip
3. Include $ACTIVATOR_HOME/bin into your path PATH


How to test and run (from the command line)
-------------------------------------------

$ activator test                    # runs the tests

$ activator run [ -Dhttp.port=<some_port> ]         # launches the app


How to import the project into IntelliJ
---------------------------------------

$ activator gen-idea    # generates IntelliJ project files

- Open IntelliJ (with Scala Plugin installed)
- Select "File -> Open Project"
- Select "Select the project directory"


How to import the project into (Eclipse based) ScalaIDE
-------------------------------------------------------

$ activator eclipse    # generates Eclipse project files

- Open ScalaIDE
- Select "File -> Import..."
- Select "General -> Existing Projects into Workspace"
- In "Select as root directory" enter the path to the project directory
- Click "Finish"
