# Bazaar Conversational Agent Architecture

Bazaar is a modular framework for designing multi-party collaborative agents that builds upon the earlier Basilica architecture (Kumar & Rosé, 2011). Bazaar plays host to a library of reusable behavioral components that each trigger a simple form of support. More complex supportive interventions are constructed by orchestrating multiple simple behaviors.  It is a publically available architecture for orchestrating conversational agent based support for group learning, inviting further work from a broad and creative community of researchers working on intelligent support for group learning. It is a powerful tool for facilitating research in collaborative learning. Its flexibility and simplicity mean it can be used to very rapidly develop platforms for investigating a wide range of important questions within the design space of dynamic support for collaborative learning. We have developed a number of such research platforms, and actively employ them in our learning studies both in classrooms and Massive Open Online Courses. As we continue to do so, we expect to discover ways in which the Bazaar architecture can be extended and refined.
# Resources
  - [DANCE website](http://dance.cs.cmu.edu/resources/)
  - [Bazaar video tutorials and slides](http://dance.cs.cmu.edu/talks/talk12.html)
  - [Architecture Overview](http://ankara.lti.cs.cmu.edu/bazaar/Bazaar%20Overview.pdf)
  - [Atlas of the Bazaar](http://ankara.lti.cs.cmu.edu/bazaar/AtlasOfTheBazaar.pdf) - Explanations about many of the customizable Bazaar files.
  - [Example of specifying prompts & responses for a Bazaar agent](https://docs.google.com/document/d/1XEPlDAgzM1vVnO8SqXwQ6oujABXlgSu9L99sWe-fWKA)
  - [Creating an agent on the Bazaar server](https://docs.google.com/document/d/e/2PACX-1vR-TeY4mhtRzsnyaeqqcvdefTPrI5RyxDteibIqlhec0lZJLx5X6Ap_Pw61mpYJWUNYOPZSC3LFaC12/pub)
  - [Viktoria's notes](https://docs.google.com/document/d/1EoVPrCCcJ1aBVRTRRx64QQ5GzfZdXd8eId-3o1XZWLE/edit#heading=h.u0d1fv8qf6f7) about running Bazaar.
  - [More links and publications](http://ankara.lti.cs.cmu.edu/bazaar/)
  - Most of the dialog files that you'll want to edit are in the runtime directory – e.g., …/WeatherAgent/runtime/.

# Docker agents vs. Legacy agents
- Legacy agents are deprecated. They
    - Often have "Legacy" in their name.
    - Use an older socket.io method.
        - Include Java project 'SocketIOClientLegacy'.
            - Includes Java program 'WebsocketChatClientLegacy.java'.
        - Include file 'AGENT_NAME/runtime/properties/WebsocketChatClientLegacy.properties'.
- Docker agents are current. They
    - Never have "Legacy" in their name.
    - Run in a Docker container.
    - Use a newer socket.io method.
    - Include Java project 'SocketIOClient'.
        - Includes Java program 'WebsocketChatClient.java'.
    - Include file 'AGENT_NAME/runtime/properties/WebsocketChatClient.properties'.

# Install Java JDK 1.8
Java 1.8 is recommended for running Bazaar. Older and newer versions may not work. Following are instructions for installing Java 1.8 in addition to any other Javas you may have installed. Some of these instructions are specific to Mac but can be generalized to Windows, etc.
- On Mac:
  - [Install 'brew'](https://brew.sh) if not already installed. This tool makes it easy to install a lot of other things.
  - [Install jenv](https://developer.bring.com/blog/configuring-jenv-the-right-way/).
  - [Install Java JDK 1.8](https://installvirtual.com/install-openjdk-8-on-mac-using-brew-adoptopenjdk/).
  - Set your environment to JDK 1.8.
    - E.g., follow [these instructions](https://developer.bring.com/blog/configuring-jenv-the-right-way/) under "Verify."
- On PC:
  - Follow [these instructions](https://adoptopenjdk.net/installation.html).

# Install this repository
  - Commands:
    - git clone https://github.com/DANCEcollaborative/bazaar.git
    - cd bazaar/LightSide
    - ant build
    - cd ../Genesis-Plugins
    - ant build

# Install in Eclipse
(For other IDEs, use an equivalent procedure)
  - From Welcome window, select "Import existing projects."
  - Select root directory: Select the directory into which installed this repository -- e.g., 'bazaar'.
  - Select "Search for nested projects."
  - Unselect project 'lightside'.
    - Explanation (optional): The LightSide project is imported as a subtree directly from the [LightSideWorkBench/LightSide repo](https://github.com/LightSideWorkbench/LightSide), where it has a .project file for use as a stand-alone project. Within Bazaar, the LightSide code needs to be referenced within the Genesis-Plugins project (that's part of what the 'ant build's were for during repository installation) rather than as a stand-alone project.
  - Click "Finish."

# Install and run the canonical Docker version in an IDE
- Installing
 - Install a Java JDK. OpenJDK’s 1.8 is recommended. (See above)
 - [Install this repository](https://github.com/DANCEcollaborative/bazaar/tree/master).
 - Install Eclipse Java Enterprise or another IDE.
 - [Install and run Docker Desktop](https://docs.docker.com)
- Running
 - In a terminal window
   - cd to the following subdirectory where DANCEcollaborative/bazaar is installed
     - E.g., cd ~/git/bazaar/bazaar_server/bazaar_server_lobby
   - Enter: docker-compose -f docker-compose-dev.yml build
     - This command will take longer the first time it is executed as it downloads several things.
   - Enter: docker-compose -f docker-compose-dev.yml up -d
     - The '-d' causes the Docker agent to run in the background after startup. Omit the '-d' to see more Docker output.
 - Within the IDE, run a Docker agent, such as ClimateChangeAgent.
 - A chat room startup window will be displayed.
   - Select the agent’s behavior conditions.
   - Set a “Room Name”.
   - Press ’Start Agent’
 - Join a chat room:  In a web browser, customize the following URL with the ROOM name you selected and a STUDENT name. For multiple students, use a URL with the same customized room name but different student names.
   - http://localhost/chat/ROOM/1/STUDENT/1/?html=share_chat
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

# Install and run the legacy version in an IDE
A few of the tutor agents are set up to use an older ("legacy") version of sockets, including WeatherLegacyAgent and MTurkLegacyAgent.
- Installing
  - Install a Java JDK. OpenJDK’s 1.8 is recommended. (See above.)
  - Install Eclipse Java Enterprise or another IDE.
  - Within the IDE, install [this repository](https://github.com/DANCEcollaborative/bazaar).
- Running
  - A chat room startup window will be displayed.
     - Select the agent’s behavior conditions.
     - For “Room Name,” use a unique 5-digit number.
     - Press ’Start Agent’
  - To join the chat room: In a web browser, customize the following URL with the ROOM name you selected and a STUDENT name. For multiple students, use a URL with the same customized room name but different student names. http://bazaar.lti.cs.cmu.edu/chat/ROOM/1/STUDENT/1/?html=index_ccc
     - For ROOM, use the Room Name you selected in the chat room window.
     - Use your choice for STUDENT. For multiple students:
       - Use a unique STUDENT name for each.
       - Set the numbers in the URL before and after STUDENT:  .../#/STUDENT/#/...
       - The first number is the student ID, and must be unique.
       - The second number is the student perspective, which is used for some agents -- e.g., for MTurk agents.
         - E.g., for multiple students, :
           - .../1/STUDENTA/1/…
           - .../2/STUDENTB/2/...
           - .../3/STUDENTC/3/…

# Convert a legacy agent to a Docker agent
- Replace the agent's file '…/runtime/properties/WebsocketChatClientLegacy.properties' with a copy of (e.g.) the file 'ClimateChangeAgent/runtime/properties/WebsocketChatClient.properties'.
- In the agent's file '…/runtime/agent.xml' replace both instances of "WebsocketChatClientLegacy" with "WebsocketChatClient".
- If any src files include the line 'import basilica2.socketchat.WebsocketChatClientLegacy', change those lines to 'import basilica2.socketchat.WebsocketChatClient'.
- In the .classpath file, replace ‘SocketIOClientLegacy’ with ‘SocketIOClient’.
- If you are using a project file, remove 'Legacy' from the agent name.
- If you are using a .launch file, remove 'Legacy' from the agent name.

# Install a Bazaar Docker agent on a Linux server
NOTE: This is only for agents that use the newer Docker sockets method. The older sockets method is deprecated.

- Install the Bazaar server
   - Server files are in [this repository](https://github.com/DANCEcollaborative/bazaar), within subdirectory 'bazaar_server/'. There are two versions with only minor differences.
      - **'bazaar_server/bazaar_server_lobby'** is for an https server with a "Lobby" for combining students in groups. **These instructions will install this version since it is most advanced.**
      - 'bazaar_server/bazaar_server_https' is for an https server without a Bazaar Lobby.
      - The two versions may be installed together on the same server so long as they use different port numbers as described below.

  - Set up your web server code to route URLs that include
      - '/bazaar' for HTTP protocol or '/bazsocket' for websockets to a port such as '8300'.
      - '/lobby' to a port such as '8400'.
  - If you are using apache2 for web services, sample files for routing URLs on a Linux Ubuntu system are included in 'bazaar_server_lobby/apache2/'.
        - Install the 'apache2.conf' file in directory '/etc/apache2/', or else update your 'apache2.conf' file with the following lines:
            <Directory /bazaar>
                Options Indexes FollowSymLinks
                AllowOverride All
                Require all granted
            </Directory\>

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

  - Install MySQL on the server.
    - A copy of the MySQL database structure without content is available in [this repository](https://github.com/DANCEcollaborative/bazaar) at 'bazaar_server_lobby/mysql/msql-structure-only.sql'.

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
               - Directory(ies) for Bazaar Docker agent(s) created above -- e.g., climatechangeagent/

      - In file 'bazaar/server_lobby_https.js', within the 'Content-Security-Policy' specification, change every instance of 'bazaar.lti.cs.cmu.edu' to your server name.
      - In file 'Dockerfile', replace MYSQL_ROOT_PASSWORD, MYSQL_USER, and MYSQL_PASSWORD with values for your MySQL.
      - In file docker-compose.yml, customize ports '8300' and '8400' to the port you used when setting up your web server above.

- Install a Bazaar Docker agent
   - The agent’s name needs to end in “Agent” or “agent” — e.g., "ClimateChangeAgent”.
   - Customize the agent's 'AGENT_NAME/runtime/properties/WebsocketChatClient.properties file. A sample version is provided for ClimateChangeAgent in [this location](https://github.com/DANCEcollaborative/bazaar/tree/master/ClimateChangeAgent/runtime/properties).
      - Comment out line 'socket_url=http://127.0.0.1' if necessary by inserting '#' at the beginning of the line.
      - Make sure the line 'socket_url=http://bazaar.lti.cs.cmu.edu:8300' is not commented out. Customize 'bazaar.lti.cs.cmu.edu:8300' with your server name and  port number for Bazaar.
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
   - sudo docker-compose up --build -d

# Run a Bazaar Docker agent on a Linux server.
  - Install a Bazaar Docker agent on a server as described above.
  - In a browser, start the agent using the following URL format: http://SERVER/bazaar/login?roomName=ROOM_NAME&roomId=ROOM_NUM&id=ID_NUM&username=USER_NAME
      - SERVER: The name or IP address of your Linux server.
      - ROOM_NAME: your agent’s name without the ‘agent’ at the end.
      - ROOM_NUM: a unique number of not more than 5 digits. If you re-use a number, users will see the previous chat.
      - ID_NUM: Unique per user. E.g., 1, 2, 3, ...
      - USER_NAME: a particular user’s name.
    - To assign multiple users to a single agent chat room, use the same ROOM_NAME and ROOM_NUM for all, varying the ID_NUM and the USER_NAME.


# Adding LightSide Machine-Learning Annotations

- Create a LightSide model. Download from http://ankara.lti.cs.cmu.edu/side/. Select the "Cutting Edge" version. A LightSide manual and installation instructions are included in the download. Once the model has been created and Bazaar has been configured to reference the model, Bazaar will start up LightSide and obtain annotations from it on a designated port.

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
