Instructions (development environment):
    Install docker
    docker-compose -f docker-compose-dev.yml build
    docker-compose -f docker-compose-dev.yml up

If this works, it will serve bazaar on port 80.

For a quick test, open these two windows in different browser tabs:

http://localhost/bazaar/login?roomName=ccc&roomId=100&mturkid=1&username=Abbott&perspective=1&html=index_ccc
http://localhost/bazaar/login?roomName=ccc&roomId=100&mturkid=1&username=Costello&perspective=1&html=index_ccc

Ignore the spreadsheet error.
It's possible you'll have to reload one or the other if it doesn't connect

Instructions (production environment):
    Use secure passwords for mysql instance
  Run:
    docker-compose build
    docker-compose up -d

    Configure your production proxy to route following URL path beginnings "/bazaar" and "/bazsocket" to bazaar backend server
    Test for access
    http(s)://your-domain/bazaar/login?roomName=ccc&roomId=100&mturkid=1&username=Abbott&perspective=1&html=index_ccc
    http(s)://your-domain/bazaar/login?roomName=ccc&roomId=100&mturkid=1&username=Costello&perspective=1&html=index_ccc