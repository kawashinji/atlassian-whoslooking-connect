Who's Looking, with Atlassian Connect
=====================================

This is an Atlassian Connect approximation of the [Who's Lookin'](https://marketplace.atlassian.com/plugins/com.atlassian.jira.plugins.whoslookin) plugin for JIRA.  

The aim is to show a list of users who are currently viewing a given issue, without causing much load on the host application. Once installed on a JIRA instance, it looks like this:
![Who's Looking with Atlassian Connect](http://i.imgur.com/nNarePB.jpg)

## Implementation Overview
The add-on is a remote app running on Play 2 using the [Atlassian Connect ac-play-java library](https://bitbucket.org/sleberrigaud_atlassian/ac-play-java). Source is here: https://bitbucket.org/atlassianlabs/whoslooking-connect

The Connect app registers an iframe in the View Issue page. The iframe content is served from the Connect app, and includes JavaScript to issue an XHR heartbeat back to the Connect app. This heartbeat results in the current user being stored as a viewer of the current issue in an in-memory map backed by Redis. Entries expire after a few seconds, so if the heartbeat for a given user stops, the user is dropped from the viewer set for that issue.

### Interactions

![Who's Looking Interactions](http://i.imgur.com/ZJ1EApQ.png)

[edit here](http://www.websequencediagrams.com/?lz=dGl0bGUgV2hvJ3MgQ29ubmVjdGVkPyAoZGV0YWlscykKSG9tZXItPkpJUkE6IFJlcXVlc3RzIHZpZXcgaXNzdWUgcGFnZSBmb3IACAdYWVotMQpKSVJBLT4ANwU6IFJlc3BvbmRzIHdpdGggdGhlAC8QLCBpbmNsdWRpbmcgYW4gaWZyYW1lIGxpbmtpbmcgdG8gV2hvc0xvb2tpbmcAgQkIKwAJCwCBDgt0aGUAOAhjb250ZW50LCBwcm92aQBYBU9BdXRoIGhlYWRlcnMgYXMgc3VwcGxpZWQgYnkgSklSQQpub3RlIHJpZ2h0IG9mAGwMOiB2ZXJpZnkANg4Kb3B0IElmIHdlJ3JlIG1pc3MAgT8FdmF0YXIgVVJMcyBvciBmdWxsIG5hbWVzIG9mIGFueQCCOgVlcgAKBQCCKwUgKGFzeW5jIHRhc2ssIGRvZXMgbm90IGJsb2NrAIIHCHJlbmRlcmluZykKAIIECy0AgwoRdXNlciAAgzgHIGZyb20gUkVTVCBBUEkuAIMRBi0-AIIsDwCDEgwAMgwuAIFyHHN0b3JlAF4PAIFnBXR1cmUgcgCEIgcKZW5kAII4HGdlbmVyYXRlIHNpZ25lZCBjb29raWUAgVQNPi0AhDIVAINXDiAoaS5lLiBIVE1MIGFuZCBKYXZhU2NyaXB0IHRvAII0BwCEcQllciBsaXN0ACQFcG9sbACEUwwpADoFc2V0cyBhAIEHDgCFNAoAgl0FSUQKbG9vcCBYSFIAgWUJAIJpBndpdGhpbgCEfwsAhR0WUG9sbHMAhU4MAIEkBmdpc3RlciBjdXJyZW50AINIBmFzIGEAgTIIb2YAhl0MAIYuDACBIA50byBjb25maXJtIElEAIU0I2lkZW50aXR5IGluAIJnHwCHQQsAgkIFb2YAhUQMAId6DGVuZA&s=modern-blue)

## Development

### Prereqs

* A Java 7 JDK.
* [Play 2.1.x](http://www.playframework.com/download)
* [Postgres](http://www.postgresql.org/download/) to store persistent information about host applications that have registered to use this Connect app.
* [Redis](http://redis.io/download) to store transient information abouts which users are looking at given issues.
* [Atlassian SDK](https://developer.atlassian.com/display/DOCS/Getting+Started) to easily spin up local JIRA instances for testing.

### Setup

+ Install the Atlassian SDK, Play, Postgres and Redis.
+ In Postgres, create database 'whoslooking'.
++ By default, the Who's Looking will use credentials whoslooking/whoslooking to access the database 'whoslooking' on localhost. If nececssary, you can configure this in `conf/application.conf`.
+ Start it by running `redis-server`.
++ By default, Who's Looking will access Redis on localhost:6379 (which are Redis's defaults). If nececssary, you can configure this in `conf/application.conf`.
+ Start a JIRA instance by running `atlas-run-standalone --product jira --version 6.0.1`. By default, JIRA will start at `http://localhost:2990/jira`. See the [Atlassian SDK documentation](https://developer.atlassian.com/display/DOCS/atlas-run-standalone) for more options.
++ Log in to the JIRA instance as admin/admin, and change the baseURL to `http://localhost:2990/jira` if necessary. All interactions with JIRA must use the same URL (some aspects of OAuth will fail if we mix uses of `localhost` with `my-machine-name`).
+ Start the Who's Looking app in dev mode by running `play run` in the app directory. It should come up on `http://localhost:9000`. Hit that URL in the browser to trigger initialisation logic.
++ The Connect app should automatically install into the local JIRA instance, but if it doesn't, do so manually by running: ` curl -v -u admin -X POST -d url=http://localhost:9000 http://localhost:2990/jira/rest/remotable-plugins/latest/installer`
++ Create an issue in JIRA, view it, and ensure the "Who's Looking?" panel is visible and populated.

You can now make changes to Who's Looking code, which the Play framework will pick up automatically as new requests come in.