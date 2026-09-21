"""Small Dev-only camera feedback changes; retain the existing capture transport."""
def patch_camera(js):
    js = js.replace('    setStatus(`Camera failed: ${error.message}`);\n    stopCamera();', '    stopCamera();\n    setStatus(`Camera failed: ${error.message}`);')
    js = js.replace('  stopButton.disabled = true;\n}', '  stopButton.disabled = true;\n  setStatus("Camera stopped. Continue in your JupyterLab notebook.");\n}')
    js = js.replace('socket.on("disconnect", () => {\n    addSystemFeedItem("Disconnected", "Lost connection to Bazaar.");', 'socket.on("disconnect", (reason) => {\n    if (reason === "io client disconnect") addSystemFeedItem("Stopped", "Camera capture stopped.");\n    else addSystemFeedItem("Disconnected", "Lost connection to Bazaar. Check your connection, then restart the camera.");')
    return js
