README — Spider-Man Distributed (MPI Web Crawler)
==================================================

REQUIREMENTS
------------
- Java JDK 8 or higher
- MPJ Express (already installed on this machine)

INTELLIJ SETUP
--------------
1. Open the project in IntelliJ

2. Add the MPJ library:
      File → Project Structure → Libraries → + → Java
      Navigate to $MPJ_HOME/lib and select all .jar files

3. Create a Run Configuration — go to Run → Edit Configurations → + → Application and set:

   Field                  Value
   -------                -------
   Main class             runtime.starter.MPJRun
   VM options             -jar /path/to/mpj/lib/starter.jar Main -np 4
   Environment variables  MPJ_HOME=/path/to/mpj

   Replace /path/to/mpj with the actual path to MPJ Express
   on this machine, for example /home/user/mpj-v0_44

4. Click Apply and run the project

CONFIGURATION
-------------
To change the start URL or page limit, edit these lines
at the top of master() in Main.java and rebuild before running:

      String startUrl = "https://www.famnit.upr.si/sl/";
      int limit = 20;
