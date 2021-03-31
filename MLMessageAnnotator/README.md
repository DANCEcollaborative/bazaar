
LightSideMessageAnnotator includes a Bazaar component that calls LightSide's predict.sh script to use a particular model's annotations

Steps to integrate LightSide Message Annotator with an Agent to use annotations from a model developed via Lightside

1. Change pathToLightSide & pathToModel properties in runtime/properties/LightSideMessageAnnotator.properties file to your LightSide installation location and model file respectively. Note that model file location should be specified relative to Lightside location.
2. Add basilica2.side.listeners.LightSideMessageAnnotator to preprocessors list in operation.properties file of the agent which will be using LightSide model for annotations
3. Add LightSideMessageAnnotator Package to classpath of the agent which will be using LightSide model for annotations


