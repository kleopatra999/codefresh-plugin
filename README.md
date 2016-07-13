![Codefresh Logo](/src/main/webapp/images/16x16/leaves_green.png?raw=true "Codefresh")

[Codefresh](http://g.codefresh.io) Plugin for Jenkins

The purpose of this plugin is to allow integrating [Codefresh](http://g.codefresh.io)  docker flow pipelines into your existing Jenkins flows.

Getting Started:

1. Define the connection to Codefresh in Jenkins system config (Manage Jenkins->Confiure system-> scroll down to find '*Define Codefesh Integration*').
This requires filling out your user name and Codefresh auth token.
You can verify the authentication by using the '_Test Connection_' button.

   To find your auth token:
    - log in to Codefresh and then open https://g.codefresh.io/api/ in another tab of the same browser.
    - Copy your token from the right-hand text field on the Swagger header.
    

2. Trigger Codefresh pipeline execution from your freestyle Jenkins jobs:

    - 'Add Build Step' -> 'Codefresh Integration'
    - 'Launch Codefesh build' is checked by default and will trigger the pipeline that corresponds to the git repo defined in the job's SCM configuration.
    - If there's no SCM defined for current job or you would like to trigger a different service pipeline - check the 'Select Codefresh Service' option. This will present you with drop-down selectable list of all your defined Codefresh services.

Once configured - the plugin will trigger Codefresh, run your tests and report the results.
