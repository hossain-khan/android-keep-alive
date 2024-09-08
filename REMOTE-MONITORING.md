# Remote Device Logging
Due to nature of this app being always active and making sure the configured app is started if they were not recently opened, remote logging was added to analyze the app behavior over period of time (eg. days).

For remote app logging, [Airtable](https://airtable.com/) service is used for its convenient way to add and visualize data in excel like data sheet.

Here is a demo of the app logging on the Aritable data sheet

![Remote Logging using Airtable](https://github.com/user-attachments/assets/e91b46d3-cffa-41b6-8ebc-6cd852ede9e7)


## How to configure Airtable
Here is a summary of what needs to be done to prepare the Airtable base for remote logging.

1. Signup for Airtable https://airtable.com/
2. Create a table from scratch and name it `"RemoteLog"`
3. Add single line text cell type with the name `"Device"`
4. Add long text cell type with the name `"Log"`
5. Remote any other column that was created by default
6. Go to https://airtable.com/create/tokens  
    1. Create a token with `data.records:write` scope  
    1. Add your base where the table is created  
    1. Click on "Create token"  
    1. Copy the generated auth token somewhere safe. This will be the one that needs to be put in the app.  
7. Go to https://airtable.com/developers/web/api/introduction and select workspace
8. On the left pane, expand `RemoteLog` table and select `Create records`
9. You should be able to see curl command with URL for this Airtable base.  
    1. For example `https://api.airtable.com/v0/appRwgPWTfxRsfc4V/RemoteLog`  
    1. This URL will need to be put in the app settings as well  
