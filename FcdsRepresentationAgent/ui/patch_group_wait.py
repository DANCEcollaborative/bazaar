"""Scoped waiting copy for the FCDS Dev grouping policy; no login-flow changes."""
from pathlib import Path
import sys

def patch(text):
    if 'const fcdsFixedGroupWait' in text:
        return text
    anchor='            // Open window immediately to avoid popup blocker'
    assert text.count(anchor)==1
    text=text.replace(anchor, '''            const fcdsFixedGroupWait = course === 'fcds-p2-26-fall-1a';
            const launchHeading = fcdsFixedGroupWait ? 'Forming your group and starting JupyterLab...' : 'Starting JupyterLab...';
            const launchExplanation = fcdsFixedGroupWait
                ? 'We wait up to one minute to form a group of 1–3 students, then start JupyterLab. Loading can take a few more minutes. Your group stays fixed once the room is created.'
                : 'This may take up to 5 minutes';

'''+anchor)
    assert text.count('<h2>Starting JupyterLab...</h2>')==1
    text=text.replace('<h2>Starting JupyterLab...</h2>', '<h2>${launchHeading}</h2>')
    text=text.replace('<p>This may take up to 5 minutes</p>', '<p>${launchExplanation}</p>')
    text=text.replace("showStatus('⏳ Starting JupyterLab... This may take up to 5 minutes.', 'loading');", "showStatus(fcdsFixedGroupWait ? launchExplanation : '⏳ Starting JupyterLab... This may take up to 5 minutes.', 'loading');")
    return text

if __name__=='__main__':
    source,destination=map(Path,sys.argv[1:]);destination.write_text(patch(source.read_text()))
