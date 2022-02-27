The LightSide Researcher's Workbench is an open-source text-mining tool released under the GNU General Public License. 
To download the latest public release, visit [http://ankara.lti.cs.cmu.edu/side](http://ankara.lti.cs.cmu.edu/side).
See `copyright/gpl.txt` for more information.

![Codeship](https://www.codeship.io/projects/175d7e90-a872-0131-b075-7a776696ef02/status "Codeship Status")

This is a mirror of the LightSide [bitbucket repository](https://bitbucket.org/lightsidelabs/lightside).

To build from source, use *ant*:

    ant build

To build with Chinese support, use

    ant build-intl

This will compile the workbench and run a modest set of unit tests. 
After that, you can run LightSide by executing run.sh (Linux, Mac) or LightSIDE.bat (Windows)

To add new feature-extraction, machine-learning, or analysis tools to the workbench, you'll want to write a plugin. 
See the appendix in the [Researcher's Manual](http://ankara.lti.cs.cmu.edu/side/LightSide_Researchers_Manual.pdf) for more information, and the core LightSide [plugins repository](https://bitbucket.org/lightsidelabs/genesis-plugins) for examples.
An example plugin is in plugins/examples.

