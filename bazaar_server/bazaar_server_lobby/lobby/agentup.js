const { parentPort, workerData } =  require("worker_threads");
const exec = require('child_process').exec;

const agentList = [];
parentPort.on("message", message => {
  agentList.push(message);
  console.log("agentup.js, agentList.pushMessage from parent: " + JSON.stringify(message));
});

function delay(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
}


function puts(error, stdout, stderr) {
  // console.log(stdout)
}

const looping = async () => {
  while(true){
    await delay(400);
    // console.log("loop forever every 3 seconds")
    const agent = agentList.pop();
    if(agent !== undefined){   
      console.log("agentup.js launching agent: " + JSON.stringify(agent));
      console.log("agentup.js launching agent -- agent.roomName = " + agent.roomName + " -- agent.teamNumber = " + agent.teamNumber); 
      exec("../bazaar/launch_agent_docker.sh " + agent.roomName + " " + agent.teamNumber + ' "none"', puts);
    }
  }
}

function fireUp(){
  looping();
}

fireUp();