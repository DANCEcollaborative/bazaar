document.addEventListener("DOMContentLoaded", function() {

    const ipAddresses = {
        "sensorVideoText": "tcp://128.2.204.249:40000",
            "sensorAudio": "tcp://128.2.204.249:40001",
            "sensorDOA": "tcp://128.2.204.249:40002",
            "sensorVAD": "tcp://128.2.204.249:40003"
    }

    // Function to send JSON data at startup
    function sendJsonData(dealer) {
        const ipAddressesJson = JSON.stringify(ipAddresses);
        dealer.send(ipAddressesJson);
    }

    // Function to stream audio data
    function streamAudioData(dealer) {
        navigator.mediaDevices.getUserMedia({ audio: true })
            .then(stream => {
                const audioContext = new AudioContext();
                const source = audioContext.createMediaStreamSource(stream);
                const analyser = audioContext.createAnalyser();
                analyser.fftSize = 2048;
                const bufferLength = analyser.frequencyBinCount;
                const dataArray = new Uint8Array(bufferLength);
                
                source.connect(analyser);
                
                function sendAudioData() {
                    analyser.getByteTimeDomainData(dataArray);
                    dealer.send(dataArray);
                    requestAnimationFrame(sendAudioData);
                }
                
                sendAudioData();
            })
            .catch(err => console.error('Error accessing microphone:', err));
    }

    // Set up ZeroMQ dealer
    const dealer = new JSMQ.Dealer();
    // dealer.connect('ws://localhost:8080');
    dealer.connect('wss://128.2.204.249:40001');
    // tcp://128.2.204.249:40001

    dealer.sendReady = function() {
        // Send JSON data at startup
        sendJsonData(dealer);
    };

    document.getElementById('startButton').addEventListener('click', function() {
        // Start streaming audio when button is clicked
        streamAudioData(dealer);
    });

    dealer.onMessage = function(message) {
        console.log('Received response:', message.popString());
    };
});
