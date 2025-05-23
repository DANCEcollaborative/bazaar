# Bazaar Conversational Agent Architecture

Bazaar is a modular framework for designing multi-party collaborative agents that builds upon the earlier Basilica architecture (Kumar & Rosé, 2011). Bazaar plays host to a library of reusable behavioral components that each trigger a simple form of support. More complex supportive interventions are constructed by orchestrating multiple simple behaviors.  It is a publically available architecture for orchestrating conversational agent based support for group learning, inviting further work from a broad and creative community of researchers working on intelligent support for group learning. It is a powerful tool for facilitating research in collaborative learning. Its flexibility and simplicity mean it can be used to very rapidly develop platforms for investigating a wide range of important questions within the design space of dynamic support for collaborative learning. We have developed a number of such research platforms, and actively employ them in our learning studies both in classrooms and Massive Open Online Courses. As we continue to do so, we expect to discover ways in which the Bazaar architecture can be extended and refined.

# Basic Bazaar Network Chart

![Basic Bazaar Network Chart](assets/Basic_Bazaar_Network_Chart.png?raw=true)

# [Resources](https://github.com/DANCEcollaborative/bazaar/blob/main/Bazaar%20References.md)

- [Selected Bazaar References](https://github.com/DANCEcollaborative/bazaar/blob/main/Bazaar%20References.md)
- [Bazaar LLM Agent Documentation](https://github.com/DANCEcollaborative/bazaar/blob/main/doc/Bazaar%20LLM%20Agent%20Documentation.md)
- [Architecture Overview](http://ankara.lti.cs.cmu.edu/bazaar/Bazaar%20Overview.pdf)
- [DANCE website](http://dance.cs.cmu.edu/resources/)
- [Older links and publications](http://ankara.lti.cs.cmu.edu/bazaar/)

# Install Java JDK 8 and Apache Ant

Java 8 (aka 1.8) is recommended for running Bazaar. Older and newer versions may not work. One method of downloading Java 8 is to browse to https://www.azul.com/downloads, select Java 8 (LTS) **JDK** for our machine's operating system and architecture.

- One method of downloading Java 8 is to browse to [https://www.azul.com/downloads](https://www.azul.com/downloads) and select Java 8 (LTS) JDK for your machine's operating system and architecture.
- Install a version 1.10.xx of Apache Ant.

# Install this repository

- Commands:
  - git clone [https://github.com/DANCEcollaborative/bazaar.git](https://github.com/DANCEcollaborative/bazaar.git)
  - cd bazaar/LightSide
  - Make sure you are using Java 1.8 (aka Java 8) Temurin for the 'ant build' steps below:
    - java -version
  - ant build
    - This should finish with "BUILD SUCCESSFUL." If it does not
      - Try the following two steps (*ant build* for Genesis-Plugins), then come back to this step.
      - If it still does not work you will not be able to run LightSide machine-learned classifiers within Bazaar until it succeeds. But you can continue with installation and run Bazaar agents without LightSide classifiers.
  - cd ../Genesis-Plugins
  - ant build
    - This should finish with "BUILD SUCCESSFUL." If it does not, you will not be able to run LightSide machine-learned classifiers within Bazaar until it succeeds. But you can continue with installation and run Bazaar agents without LightSide classifiers.

# Detailed Instructions for Installing in Eclipse

These instructions were created using Eclipse IDE for Java Developers, version 2021-12 (4.22.0). For other IDEs, use an equivalent procedure

- Make sure your Eclipse is set to use Java 1.8 Temurin as the default JRE.
  - Select Eclipse > Preferences
    - Select Java > Installed JREs
      - If more than one JRE is listed, check the box next to the JDK 1.8 (a.k.a. JDK 8) Temurin that you have installed.
      - Click "Apply"
    - Select Java > Compiler
      - Set Compiler Compliance Level to 1.8.
      - Click "Apply and Close"
- Select from the File menu "Open Projects from File System"
  - Click "Directory"
    - Select the just installed ".../bazaar/" directory
    - Click "Open"
  - Check "Search for nested projects"
    - Check "Detect and configure project natures"
  - Click "Finish"

# Install and run in an IDE

- Installing
  - Install a Java JDK. OpenJDK’s 1.8 is recommended. (See above)
  - [Install this repository](https://github.com/DANCEcollaborative/bazaar/tree/master).
- Install Eclipse Java Enterprise or another IDE.
- [Install and run Docker Desktop](https://docs.docker.com)
- Running
  - In a terminal window
    - cd to the following subdirectory where DANCEcollaborative/bazaar is installed
      - E.g., cd ~/git/bazaar/bazaar_server/bazaar_server_lobby
    - Enter:
      - If running on a Mac with an Intel chip or on a PC:
        - docker compose -f docker-compose-dev.yml up --build -d
      - If running on a Mac with an M1 chip:
        - docker compose -f docker-compose-dev-apple-m1.yml up --build -d
      - This command will take longer the first time it is executed as it downloads several things.
    - The '-d' causes the Docker agent to run in the background after startup. Omit the '-d' to see more Docker output.
  - Within the IDE, run a Bazaar agent such as ClimateChangeAgent.
    - A chat room startup window will be displayed.
    - Select the agent’s behavior conditions.
    - Set a “Room Name”.
    - Press ’Start Agent’
  - Join a chat room:  In a web browser, customize the following URL with the ROOM name you selected and a STUDENT name. For multiple students, use a URL with the same customized room name but different student names.
    - [http://localhost/chat/ROOM/1/STUDENT/1/?html=climate_change](http://localhost/chat/ROOM/1/STUDENT/1/?html=climate_change)
    - Use the ROOM you selected in the chat room window.
    - Use your choice for STUDENT. For multiple students:
      - Use a unique STUDENT name for each.
      - Set the numbers in the URL before and after STUDENT:  .../#/STUDENT/#/...
        - The first number is the student ID, and must be unique.
        - The second number is the student perspective, which is used for some agents -- e.g., for MTurk agents.
        - E.g., for multiple students, :
          - .../1/STUDENTA/1/…
          - .../2/STUDENTB/2/...
          - .../3/STUDENTC/3/…

# Install a Bazaar agent on a Linux server

- Install the Bazaar server
  - Server files are in directory [**'bazaar_server/bazaar_server_lobby'**](https://github.com/DANCEcollaborative/bazaar/tree/master/bazaar_server/bazaar_server_lobby).
  - Set up your web server code to route URLs that include
    - '/bazaar' for HTTP protocol or '/bazsocket' for websockets to a port such as '8300'.
    - '/lobby' to a port such as '8400'.
  - If you are using apache2 for web services, sample files for routing URLs on a Linux Ubuntu system are included in 'bazaar_server_lobby/apache2/'.  
    - Install the 'apache2.conf' file in directory '/etc/apache2/', or else update your 'apache2.conf' file with the following lines:  
    <Directory /bazaar>  
    Options Indexes FollowSymLinks  
    AllowOverride All  
    Require all granted  
    </Directory>

    ````
    - Install the file from subdirectory 'apache2/sites-available/' into directory '/etc/apache2/sites-available'.
    - Execute the appropriate command(s) below for your http and/or https version:
      - sudo a2ensite bazaar-docker-lobby.conf
      - sudo a2ensite bazaar-docker-https.conf
    - Depending on your apache2 configuration, you may need to execute the following:
      - sudo a2enmod rewrite
      - sudo a2enmod proxy
      - sudo a2enmod proxy_http
      - sudo a2enmod proxy_wstunnel
      - sudo a2enmod rewrite
    - For the https version, you may also need to execute the following:
      - sudo a2enmod ssl
    - Execute: sudo systemctl restart apache2
    ````

  - Install and start Docker.
    - Install [Docker for Linux](https://docs.docker.com/engine/install/).
    - Start Docker: sudo systemctl start docker
  - Install and run the files from subdirectory 'bazaar_server_lobby' on the server.
    - Structure:
      - YOUR_BASE_DIRECTORY
        - Dockerfile
        - Dockerfile.mysql
        - docker-compose.yml
        - runLobby
        - bazaar
          - All files within subdirectory 'bazaar'.
        - lobby
          - All files within subdirectory 'lobby'.
        - agents
          - Directory(ies) for Bazaar agent(s) created above -- e.g., climatechangeagent/
    - In file 'bazaar/server_lobby_https.js', within the 'Content-Security-Policy' specification, change every instance of 'bazaar.lti.cs.cmu.edu' to your server name.
    - In file 'Dockerfile', replace MYSQL_ROOT_PASSWORD, MYSQL_USER, and MYSQL_PASSWORD with values for your MySQL.
    - In file docker-compose.yml, customize ports '8300' and '8400' to the port you used when setting up your web server above.
- Install a Bazaar agent
  - The agent’s name needs to end in “Agent” or “agent” — e.g., "ClimateChangeAgent”.
  - Customize the agent's 'AGENT_NAME/runtime/properties/WebsocketChatClient.properties file. A sample version is provided for ClimateChangeAgent in [this location](https://github.com/DANCEcollaborative/bazaar/tree/master/ClimateChangeAgent/runtime/properties).
    - Comment out line 'socket_url=[http://127.0.0.1](http://127.0.0.1)' if necessary by inserting '#' at the beginning of the line.
    - Make sure the line 'socket_url=[http://bazaar.lti.cs.cmu.edu:8300](http://bazaar.lti.cs.cmu.edu:8300)' is not commented out. Customize 'bazaar.lti.cs.cmu.edu:8300' with your server name and  port number for Bazaar.
  - Create a runnable .jar file and place it in the agent’s runtime/ directory. E.g., using Eclipse:
    - In Eclipse in the Package Explorer view, right click on the agent’s package name (e.g. ‘ClimateChangeAgent’) and select “Export.”
    - Select an export wizard: Under Java, select “Runnable JAR file,” then select “Next.”
    - Runnable JAR File Specification
      - Launch configuration: Select one from the dropdown. If you’ve created the agent l- ike most agents, there will probably be an option for “NewAgentOperation - <your agent’s name>.”
      - Export destination: Browse to select '<your agent directory name>/runtime/<your agent name>.jar’.
      - Select “Finish.”
    - You’ll get some warnings that you can ignore, so just press “OK.”
      - “This operation repacks referenced libraries. …”
      - "JAR export finished with warnings. …”
  - On the Linux server.
    - Create a subdirectory for the agent within directory 'bazaar_server/bazaar_server_lobby/agents/'— e.g., ‘climatechangeagent’.
    - Copy all of the files within your agent’s runtime directory — but not the ‘runtime/‘ directory itself — to the agent subdirectory.
- Start the Docker containers:
  - cd bazaar_server_lobby
  - sudo docker compose up --build -d

# Run a Bazaar agent on a Linux server.

- Install a Bazaar agent on a server as described above.
- In a browser, start the agent using the following URL format: [http://SERVER/bazaar/login?roomName=ROOM_NAME&roomId=ROOM_NUM&id=ID_NUM&username=USER_NAME](http://SERVER/bazaar/login?roomName=ROOM_NAME&roomId=ROOM_NUM&id=ID_NUM&username=USER_NAME)
  - SERVER: The name or IP address of your Linux server.
  - ROOM_NAME: your agent’s name without the ‘agent’ at the end.
  - ROOM_NUM: A unique string. If you re-use a string, users may see the previous chat associated with the same agent and that ROOM_NUM value.
  - ID_NUM: Unique per user. E.g., 1, 2, 3, ...
  - USER_NAME: a particular user’s name.
  - To assign multiple users to a single agent chat room, use the same ROOM_NAME and ROOM_NUM for all, varying the ID_NUM and the USER_NAME.

# Adding LightSide Machine-Learning Annotations

- Create a LightSide model. Download from [http://ankara.lti.cs.cmu.edu/side/](http://ankara.lti.cs.cmu.edu/side/). Select the "Cutting Edge" version. A LightSide manual and installation instructions are included in the download. Once the model has been created and Bazaar has been configured to reference the model, Bazaar will start up LightSide and obtain annotations from it on a designated port.
- Configure Bazaar. A worked example is provided as agent 'MTurkLightSideAgent' - file names below are specified relative to that agent.
  - Agent classpath: Add
    - LightSideMessageAnnotator
    - lightside
  - File runtime/properties/operation.properties: Include the following line among the list of 'operation.preprocessors': basilica2.side.listeners.LightSideMessageAnnotator,\
    - (The \ at the end of the line above is a continuation symbol, used if other preprocessors are listed after LightSideMessageAnnotator.)
  - Include file runtime/properties/LightSideMessageAnnotator.properties with the following settings.
    - pathToLightSide: Path to the LightSide project code relative to the current agent's code.
      - For running Bazaar locally, this value is typically, '../../lightside/'.
      - For running Bazaar on a server, this is the path to LightSide on the server. E.g., '/usr0/lightside/'.
    - predictionCommand: Typically, 'scripts/prediction_server.sh',
    - modelPath: The path to the location of the model within pathToLightSide. Typically 'saved/' or 'models/'.
    - modelNickname: This is the developer's or the user's choice.
    - classifications: A comma-separated list in format classification1, classification1-probability-threshold, classification2, classification2-probability-threshold, ..., ...
      - Example: 'classifications=detected,70,notdetected,50'
        - Classification 'detected', upper-cased as 'DETECTED', will be returned if LightSide indicates that the probability that 'detected' is true is 70% or greater.
        - Classification 'notdetected', upper-cased as 'NOTDETECTED',  will be returned if LightSide indicates that the probability that 'notdetected' is true is 50% or greater.
      - More than two classification alternatives may be provided (with any classification names), and zero, one, or more classifications may be returned.
    - port: The port on which this LightSideMessageAnnotator will query a particular LightSide model. Bazaar tells LightSide which port to use when it starts LightSide.
      - More than one LightSideMessageAnnotator can be configured (with different names), querying different LightSide models which are listening on different ports.
  - There is more than one way to use LightSide annotations within the agent.
    - Use the annotations just like annotations that are created from dictionaries. A silly example is provided for the first dialog entry in runtime/dialogues/dialogues-example.xml, in which responses are provided for both (1) LightSide classifications 'DETECTED' and 'NOTDETECTED' for a toy student attitude positivity detector, and (2) for dictionary-based classifications 'AFFIRMATIVE' and 'NEGATIVE'.
    - Use the annotations in a Java-based listener. Example:
      - File src/basilica2/myagent/listeners/Register.java is listed as a listener in file runtime/properties/operation.properties.
        - This file references a table of LightSide prompts, which is provided in runtime/dialogues/lightside-prompts.xml. The table can list more than one prompt alternative for each possible classification. If more than one alternative is provided, the prompt will be selected at random from among the alternatives.
        - The file checks for the existence of the LightSide classifications that it is listening for (in this case, "DETECTED" or "NOTDETECTED"), and if it finds such a classification, it proposes one of the associated prompts to return to the student.