#!/bin/bash -e
NEW_RELIC_API_KEY=`heroku config:get NEW_RELIC_API_KEY --remote production`
NEW_RELIC_APP_NAME=`heroku config:get NEW_RELIC_APP_NAME --remote production`
git push production master
curl -H "x-api-key:${NEW_RELIC_API_KEY}" -d "deployment[app_name]=${NEW_RELIC_APP_NAME}" https://rpm.newrelic.com/deployments.xml
