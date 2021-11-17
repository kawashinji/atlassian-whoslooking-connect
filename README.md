Who's Looking, with Atlassian Connect
=====================================

This is an Atlassian Connect approximation of the [Who's Lookin'](https://marketplace.atlassian.com/plugins/com.atlassian.jira.plugins.whoslookin) plugin for JIRA.  

The aim is to show a list of users who are currently viewing an issue, without causing much load on the host application. Once installed on a JIRA instance, it looks like this:
![Who's Looking with Atlassian Connect](http://i.imgur.com/nNarePB.jpg)

## Implementation Overview
The add-on is a remote app running on Play 2 using the [Atlassian Connect ac-play-java library](https://bitbucket.org/atlassian/atlassian-connect-play-java). Source is here: https://bitbucket.org/atlassian/whoslooking-connect

The Connect app registers an iframe in the View Issue page. The iframe content is served from the Connect app, and includes JavaScript to issue an XHR heartbeat back to the Connect app. This heartbeat results in the current user being stored as a viewer of the current issue in an in-memory map backed by Redis. Entries expire after a few seconds, so if the heartbeat for a given user stops, the user is dropped from the viewer set for that issue.

### Interactions

![Who's Looking Interactions](https://www.websequencediagrams.com/cgi-bin/cdraw?lz=dGl0bGUgV2hvJ3MgQ29ubmVjdGVkPyAoZGV0YWlscykgVXBkYXRlZCB3aXRoIHN0ZXBzCkhvbWVyLT5KSVJBOiBSZXF1ZXN0cyB2aWV3IGlzc3VlIHBhZ2UgZm9yAAgHWFlaLTEKSklSQS0-ADcFOiAxLlJlc3BvbmRzAFMGdGhlADEQLCBpbmNsdWRpbmcgYW4gaWZyYW1lIGxpbmtpbmcgdG8gV2hvc0xvb2tpbmcAgQsIKwAJCzogMi4AgRQJdGhlADoIY29udGVudCwgcHJvdmkAWgVPQXV0aCBoZWFkZXJzIGFzIHN1cHBsaWVkIGJ5IEpJUkEKbm90ZSByaWdodCBvZgBuDDogMy52ZXJpZnkAOA4AFhw0LiBHZXQgSG9zdCxBY2NvdW50LFJlc291cmNlIElkcwpvcHQgSWYgd2UncmUgbWlzc2luZyBkaXNwbGF5IG5hbWVzIG9mIGFueQCCcAVlcgAKBQCCYQUgKGFzeW5jIHRhc2ssIGRvZXMgbm90IGJsb2NrAII7CHJlbmRlcmluZykKAII4Cy0Ag0kINQCCLAp1c2VyIACEAwcgZnJvbSBSRVNUIEFQSS4Ag0kGLT4AgmQNNQCDRw8ANAwuAIIoHDYuU2F2ZQBqBnRvIGhlYXJ0YmVhdCBsaXN0AIRFBWN1cnJlbnQgaG9zdCsAhGEFAIJ4HDcuc3RvcgBKBwCBOAlvciBmdXR1cmUgcgCFLgcKZW5kAINAHDguZ2VuZXJhdGUgSUQgdG9rZW4AhVMFWEhSADoKAII5DD4tAIVaBzkAhVIPAIRvDiAoSFRNTCBhbmQgSmF2YVNjcmlwdCB0bwCDFQcAhgYJZXIAgXwGYW5kIHBvbGwAhWgMKQpsb29wAH4NAIMeBndpdGhpbgCFaAsAhggWMTAuUG9sbHMAhjwMAH0GZ2lzdGUAgmkKAIQBBWFzIGEAgQsIb2YAh00MAIccDACCHAZ0byBjb25maXJtIElECgCGIBwxMQCGNQhpZGVudGl0eSB1AIVoBQCCZgUAgkQWMTIuUmUAiDALAIQeBW9mAIV_DACIawxlbmQAgWkXMy5XaGVuIGxlYXZlAIh8BXBhZ2UvAIhiBiwgc2VuZCBkZWxldACDAgkAhC8HAIE0HTQuUmVtb3ZlAIIwDgCGNwUAhUgOCg&s=default)

[edit here](https://www.websequencediagrams.com/?lz=dGl0bGUgV2hvJ3MgQ29ubmVjdGVkPyAoZGV0YWlscykKSG9tZXItPkpJUkE6IFJlcXVlc3RzIHZpZXcgaXNzdWUgcGFnZSBmb3IACAdYWVotMQpKSVJBLT4ANwU6IFJlc3BvbmRzIHdpdGggdGhlAC8QLCBpbmNsdWRpbmcgYW4gaWZyYW1lIGxpbmtpbmcgdG8gV2hvc0xvb2tpbmcAgQkIKwAJCwCBDgt0aGUAOAhjb250ZW50LCBwcm92aQBYBU9BdXRoIGhlYWRlcnMgYXMgc3VwcGxpZWQgYnkgSklSQQpub3RlIHJpZ2h0IG9mAGwMOiB2ZXJpZnkANg4Kb3B0IElmIHdlJ3JlIG1pc3NpbmcgZGlzcGxheSBuYW1lcyBvZiBhbnkAgi4FZXIACgUAgh8FIChhc3luYyB0YXNrLCBkb2VzIG5vdCBibG9jawCBewhyZW5kZXJpbmcpCgCBeAstAIJ-EXVzZXIgAIMsByBmcm9tIFJFU1QgQVBJLgCDBQYtPgCCIA8AgwYMADIMLgCBZhxzdG9yZQBeD29yIGZ1dHVyZSByAIQWBwplbmQAgiwcZ2VuZXJhdGUgSUQgdG9rZW4AhDkFWEhSADgKAIFhDD4tAIQyFQCDVw4gKEhUTUwgYW5kIEphdmFTY3JpcHQgdG8AgjsHAIRsCWVyIGxpc3QAJAVwb2xsAIRODCkKbG9vcAB8DQCCRgZ3aXRoaW4AhFALAIRuFlBvbGxzAIUfDAB6Bmdpc3RlciBjdXJyZW50AIMlBmFzIGEAgQgIb2YAhi4MAIV_DACCFwZ0byBjb25maXJtIElECgCEfiNpZGVudGl0eSB1AIUIBQCCXgUAgjoYAIcNCwCCEwVvZgCFHAwAh0YMZW5k&s=modern-blue)

## Development

### Prereqs

* A Java 7 JDK.
* [Play 2.2.x](http://www.playframework.com/download)
* [Postgres](http://www.postgresql.org/download/) to store persistent information about host applications that have registered to use this Connect app.
* [Redis](http://redis.io/download) to store transient information abouts which users are looking at given issues.
* [Atlassian SDK](https://developer.atlassian.com/display/DOCS/Getting+Started) to easily spin up local JIRA instances for testing.

### Setup

+ Install the Atlassian SDK, Play, Postgres and Redis.
+ Create postgres database 'whoslooking'.
	+ By default, Who's Looking will use credentials `whoslooking/whoslooking` to access database 'whoslooking' on localhost. If nececssary, you can configure this in `conf/application.conf`.
+ Start Redis by running `redis-server`.
	+ By default, Who's Looking will access Redis on localhost:6379 (which are Redis's defaults). If nececssary, you can configure this in `conf/application.conf`.
+ Start a JIRA instance by running `atlas-run-standalone --product jira --version 6.4-OD-14-082 --bundled-plugins com.atlassian.bundles:json-schema-validator-atlassian-bundle:1.0.4,com.atlassian.webhooks:atlassian-webhooks-plugin:2.0.0,com.atlassian.jwt:jwt-plugin:1.2.2,com.atlassian.upm:atlassian-universal-plugin-manager-plugin:2.18.2-D20141112T015724,com.atlassian.plugins:atlassian-connect-plugin:1.1.22 --jvmargs -Datlassian.upm.on.demand=true`. 
	+ [See the Connect documentation](https://developer.atlassian.com/static/connect/docs/developing/developing-locally.html) for the latest version of the above command.
	+ By default, JIRA will start at `http://localhost:2990/jira`. See the [Atlassian SDK documentation](https://developer.atlassian.com/display/DOCS/atlas-run-standalone) for more options.
	+ All interactions with JIRA must use the same URL (some aspects of OAuth will fail if we mix uses of `localhost` with `my-machine-name`). If necessary, log in to the JIRA instance as admin/admin, and change the baseURL to `http://localhost:2990/jira`. 
+ Start the Who's Looking app in dev mode by running `play run` in the app directory. It should come up on `http://localhost:9000`. Hit that URL in the browser to trigger initialisation logic.
	+ The Connect app should automatically install into the local JIRA instance, but if it doesn't, do so manually by running: ` curl -v -u admin -X POST -d url=http://localhost:9000 http://localhost:2990/jira/rest/atlassian-connect/1.0/installer`

You can now create an issue in JIRA, view it, and ensure the "Who's Looking?" panel is visible and populated. If you make changes to Who's Looking code, the Play framework will pick up the code changes as new requests come in.

## Running Integration Tests

* Run `play test` for basic unit/integration tests.
* Run `cd pdv; mvn3 test -Plocal` to run browser-based PDV tests. See `pdv/README.md` for more info.

## Reporting Bugs

Please report issues here: https://ecosystem.atlassian.net/browse/WLC


## License

See `LICENSE.txt` and `./public/third-party/licenses.txt`
