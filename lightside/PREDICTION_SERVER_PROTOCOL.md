Besides its workbench function, the LightSide code also contains a simple web service that can apply a model to a string and immediately return a classification.  Develop one or more models using the workbench, save the model(s) to .xml or .ser files, and this web service can make use of those saved models.

Startup
======
Start by running from the same jar file as the Lightside workbench, just with a different main class (edu.cmu.side.recipe.PredictionServer).  You can (optionally) specify any number of model files and their "nicknames" on the command line to preload, in the form  nickname:model_path nickname:model_path ...  

If you don't list any models, you can load them live by posting to the /upload endpoint.

This mini server serves the following endpoints on port 8000.  There is no security; it's just simple http.
The best way to start testing it out is to run it on a laptop then point your browser to http://localhost:8000/upload

Endpoints
========
/upload    GET
   -> displays an html form for uploading (with post enctype="multipart/form-data")
            The form collects variables: 
               model -> a model (a *.ser or *xml file, saved from the LightSide workbench)
               modelNick  -> choose a nickname for the model

/upload     POST 
   -> expects model and modelNick variables; simply remembers the model by that name
   -> returns 400 if one of those is missing
   -> returns 409 if a model with that modelNick or file name is already uploaded
   -> returns 418 if model file seems corrupt

/try/<modelName>          GET
     -> displays an html form, POSTing to /try/modelName, filling in the model name, with a blank called "sample"

/try/<modelName>          POST
    -> expects some sample text submitted as variable "sample" 
    -> If there's an error, it prints a message, and does not present the form again.
       -> 404 if model not found
       -> 418 if model corrupt, wrong version, etc.
    -> if no error,
       -> prints label:score for each label the named model assigns
       -> followed by the same form as /try/modelName GET offers
     
/predict/<modelName>        GET
    -> expects one or more URL parameters q=<text to apply prediction to>
    -> returns a string consisting of predicted labels for all instances, separated by single spaces
    -> 404 if model not found
    -> 418 if model corrupt, wrong version, etc.
    -> 500 on prediction error
    -> 400 on any other error

/favicon.ico
    -> returns an icon
