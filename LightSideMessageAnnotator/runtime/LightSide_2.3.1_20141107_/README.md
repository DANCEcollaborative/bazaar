The LightSide Researcher's Workbench is an open-source text-mining tool released under the GNU General Public License. 
To download the latest public release, visit [http://ankara.lti.cs.cmu.edu/side](http://ankara.lti.cs.cmu.edu/side).
See `copyright/gpl.txt` for more information.

![Codeship](https://www.codeship.io/projects/175d7e90-a872-0131-b075-7a776696ef02/status "Codeship Status")

To build from source, use *ant*:

    ant build

This will compile the workbench and run a modest set of unit tests.

To add new feature-extraction, machine-learning, or analysis tools to the workbench, you'll want to write a plugin. 
See the appendix in the [Researcher's Manual](http://ankara.lti.cs.cmu.edu/side/LightSide_Researchers_Manual.pdf) for more information, and the core LightSide [plugins repository](https://bitbucket.org/lightsidelabs/genesis-plugins) for examples.