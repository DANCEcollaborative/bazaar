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

# Installing and running the canonical DANCEcollaborative Docker version
- Installing
 - Install a Java JDK. OpenJDK’s 1.8 is recommended. (See above)
 - Install Eclipse Java Enterprise or another IDE.
 - Within the IDE, install [this repository](https://github.com/DANCEcollaborative/bazaar).
 - [Install and run Docker Desktop](https://docs.docker.com)
- Running
 - In a terminal window
   - cd to the directory where DANCEcollaborative/bazaar is installed
     - E.g., cd ~/git/bazaar
   - Enter: docker-compose -f docker-compose-dev.yml build
     - This command will take longer the first time it is executed as it downloads several things.
   - Enter: docker-compose -f docker-compose-dev.yml up
     - Wait until the terminal output quiesces. (The terminal command prompt won’t return until you stop the process – e.g., with ctrl-c.)
 - Within the IDE, run an agent that uses the SocketIOClient project, such as WeatherAgent or MturkAgent.
 - A chat room startup window will be displayed.
   - Select the agent’s behavior conditions.
   - Set a “Room Name”.
   - Press ’Start Agent’
 - Join a chat room:  In a web browser, customize the following URL with the ROOM name you selected and a STUDENT name. For multiple students, use a URL with the same customized room name but different student names.
   - http://localhost/bazaar/chat/ROOM/1/STUDENT/1/?html=index_ccc&forum=undefined

# Installing and running the legacy version
A few of the tutor agents are set up to use an older ("legacy") version of sockets, including WeatherAgentLegacy and MTurkAgentLegacy.
- Installing
 - Install a Java JDK. OpenJDK’s 1.8 is recommended. (See above.)
 - Install Eclipse Java Enterprise or another IDE.
 - Within the IDE, install [this repository](https://github.com/DANCEcollaborative/bazaar).
 - [Install and run Docker Desktop](https://docs.docker.com)
- Running
 - A chat room startup window will be displayed.
   - Select the agent’s behavior conditions.
   - Set a “Room Name”.
   - Press ’Start Agent’
 - To join the chat room: In a web browser, customize the following URL with the ROOM name you selected and a STUDENT name. For multiple students, use a URL with the same customized room name but different student names. http://bazaar.lti.cs.cmu.edu/chat/ROOM/1/STUDENT/1/?html=index&forum=undefined#
   - Use the ROOM you selected in the chat room window.
   - Use your choice for STUDENT. For multiple students:
     - Use a unique STUDENT name for each.
     - Set the numbers in the URL before and after STUDENT:  .../#/STUDENT/#/...
       - The first number is {the student ID}, and must be unique.
       - The second number is {the student perspective}.
       - E.g., for multiple students, :
         - .../1/STUDENT1/1/…
         - .../2/STUDENT2/2/...
         - .../3/STUDENT3/3/…
   - You can also use a URL address that includes an Etherpad panel with the same URL customizations specified above.
       - http://bazaar.lti.cs.cmu.edu/chat/ROOM/1/STUDENT/1/?html=xu2

# Converting a Docker agent to a legacy agent
In case you don't want to run Docker.
- Replace the agent's file '…/runtime/properties/WebsocketChatClient.properties' with a copy of (e.g.) the file 'WeatherAgentLegacy/runtime/properties/WebsocketChatClientLegacy.properties'
- In the agent's file '…/runtime/agent.xml' replace both instances of "WebsocketChatClient" with "WebsocketChatClientLegacy".
- If any src files include the line 'import basilica2.socketchat.WebsocketChatClient', change those lines to 'import basilica2.socketchat.WebsocketChatClientLegacy'.
- In the .classpath file, replace ‘SocketIOClient’ with ‘SocketIOClientLegacy’.
