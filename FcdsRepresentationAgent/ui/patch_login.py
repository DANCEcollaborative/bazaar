"""Prepare a Dev-only default-notebook change in the public Bree login page."""
from pathlib import Path
import sys
source, destination = map(Path, sys.argv[1:])
text = source.read_text()
old = "                    newWindow.location.href = url;"
new = r"""                    // The representation Dev opens its instructions immediately.
                    // Preserve the token, identity and room query parameters.
                    const launchUrl = new URL(url);
                    if (course === 'fcds-p2-26-fall-1a' && launchUrl.hostname === 'collab.lti.cs.cmu.edu') {
                        // Collaboration 1.x uses the RTC drive for shared notebooks.
                        launchUrl.pathname = launchUrl.pathname.replace(/\/lab\/?$/, '/lab/tree/RTC:workspace.ipynb');
                    }
                    newWindow.location.href = launchUrl.href;"""
previous_path = "'/lab/tree/workspace.ipynb'"
if text.count(old) == 1:
    text = text.replace(old, new)
else:
    # Upgrade the earlier Dev-only auto-open patch without touching other courses.
    assert "course === 'fcds-p2-26-fall-1a'" in text
    assert text.count(previous_path) == 1
    text = text.replace(previous_path, "'/lab/tree/RTC:workspace.ipynb'")
destination.write_text(text)
