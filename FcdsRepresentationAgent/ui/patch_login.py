"""Prepare a Dev-only default-notebook change in the public Bree login page."""
from pathlib import Path
import sys
source, destination = map(Path, sys.argv[1:])
text = source.read_text()
old = "                    newWindow.location.href = url;"
assert text.count(old) == 1
new = r"""                    // The representation Dev opens its instructions immediately.
                    // Preserve the token, identity and room query parameters.
                    const launchUrl = new URL(url);
                    if (course === 'fcds-p2-26-fall-1a' && launchUrl.hostname === 'collab.lti.cs.cmu.edu') {
                        launchUrl.pathname = launchUrl.pathname.replace(/\/lab\/?$/, '/lab/tree/workspace.ipynb');
                    }
                    newWindow.location.href = launchUrl.href;"""
destination.write_text(text.replace(old, new))
