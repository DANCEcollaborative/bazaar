This is a repository of public LightSide plugins.

To add new feature-extraction, machine-learning, or analysis tools to the workbench, you'll want to write a plugin. This repository contains all the plugins currently included in the LightSide Researcher's Workbench. See the appendix in the [Researcher's Manual](http://ankara.lti.cs.cmu.edu/side/LightSide_Researchers_Manual.pdf) for more information.

![Codeship](https://www.codeship.io/projects/bcf73bd0-a8a5-0131-8655-063dfab0229a/status "Codeship Status")

The LightSide Researcher's Workbench is an open-source text-mining tool released under the GNU General Public License. 
To download the latest public release, visit [http://ankara.lti.cs.cmu.edu/side](http://ankara.lti.cs.cmu.edu/side).
See `copyright/gpl.txt` for more information.

This is a mirror of the LightSide [bitbucket repository](https://bitbucket.org/lightsidelabs/genesis-plugins).

To build from source, use *ant*:

    ant build

This will compile the plugins to a jar, and run a modest set of unit tests. Note that this build script presumes that the [LightSide Workbench codebase](https://bitbucket.org/lightsidelabs/lightside) is present in a sister directory `../lightside` -- if you want to keep it somewhere else, you'll need to edit the `lightside.location` variable in the build.xml script.

Note that when you commit to the plugins repository on the "default" branch, the deploy hook will create a snapshot in the http://ankara.lti.cs.cmu.edu/side/snapshots/plugins/ directory with the new plugins jar. However, it will not update the main Workbench distribution -- update the jar in the LightSide repository's default branch to do so.
