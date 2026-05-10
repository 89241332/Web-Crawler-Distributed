README — Spider-Man Distributed (MPI Web Crawler)
==================================================

REQUIREMENTS
------------
- Java JDK 8 or higher
- MPJ Express

INTELLIJ SETUP
--------------
1. Open the project in IntelliJ

2. Add the MPJ library:
      File → Project Structure → Libraries → + → Java
      Navigate to: $MPJ_HOME/lib and select all .jar files

3. Set up Run Configuration:
      Run → Edit Configurations → + → Application
      Main class: Main
      VM options: (leave empty)
      Program arguments: (leave empty)

4. In Edit Configurations, change the run type to JAR:
      Path to JAR: $MPJ_HOME/lib/starter.jar
      Program arguments: -np 4 Main
      Working directory: path to the src/ folder

5. Set environment variable in Edit Configurations:
      Environment variables: MPJ_HOME=/path/to/mpj

6. Apply changes and run the project

CONFIGURATION
-------------
To change the start URL or page limit, edit Main.java:

      String startUrl = "https://www.famnit.upr.si/sl/";
      int limit = 20;
