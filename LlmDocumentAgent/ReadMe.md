# LlmDocumentAgent

This is a simple test/demo agent for reading from and writing to Etherpad and invoking an LLM for responses. 
- For test/demo purposes, the agent uses an LLM to **check grammar** within the Etherpad. Each time it detects a sentence with a grammar error, it publishes a suggested correction within the chat. A sentence is defined as a string without a newline character ending in one of the characters in {. ? !}.
- If a message is entered in the chat starting with the agent name ("DocBot") followed by "fix ...", the agent/LLM combination will fix any grammar errors in the Etherpad. 
- The Etherpad is invoked from an HTML page that uses the same socket room naming convention as Bazaar agents, so the Bazaar agent knows how to reference the Etherpad. An example is [document-chat-mm.html](https://github.com/DANCEcollaborative/bazaar/blob/main/bazaar_server/bazaar_server_lobby/lobby/html_pages/document-chat-mm.html).
  - Sample URL invocation:  https://bree.lti.cs.cmu.edu/bazaar/login?roomName=llmdocument&roomId=0001&id=1&username=YourName&html=document-chat-mm.
    - For each invocaton, change the roomId value within the URL to a unique value so you will get a fresh agent -- e.g., 202607010050, where the first 8 digits are the current date (e.g., 2026-08-01), and the subsequent digits or letters are unique as well. 
- As a simple test/demo agent, it  has many deficiencies that could be improved with further work. 