Who's Looking, with Atlassian Connect
=====================================

This is an Atlassian Connect approximation of the [Who's Lookin'](https://marketplace.atlassian.com/plugins/com.atlassian.jira.plugins.whoslookin) plugin for JIRA.  

The aim is to show a list of users who are currently viewing a given issue, without causing much load on the host application. Once installed on a JIRA instance, it looks like this:
![Who's Looking with Atlassian Connect](http://i.imgur.com/nNarePB.jpg)

## Implementation Overview
The add-on is a remote app running on Play 2 using the [Atlassian Connect ac-play-java library](https://bitbucket.org/sleberrigaud_atlassian/ac-play-java). Source is here: https://bitbucket.org/atlassianlabs/whoslooking-connect

The Connect app registers an iframe in the View Issue page. The iframe content is served from the Connect app, and includes JavaScript to issue an XHR heartbeat back to the Connect app. This heartbeat results in the current user being stored as a viewer of the current issue in an in-memory map. Entries expire after a few seconds, so if the heartbeat for a given user stops, the user is dropped from the viewer set for that issue.

## Interactions

![Who's Looking Interactions](http://i.imgur.com/ZJ1EApQ.png)

[edit here](http://www.websequencediagrams.com/?lz=dGl0bGUgV2hvJ3MgQ29ubmVjdGVkPyAoZGV0YWlscykKSG9tZXItPkpJUkE6IFJlcXVlc3RzIHZpZXcgaXNzdWUgcGFnZSBmb3IACAdYWVotMQpKSVJBLT4ANwU6IFJlc3BvbmRzIHdpdGggdGhlAC8QLCBpbmNsdWRpbmcgYW4gaWZyYW1lIGxpbmtpbmcgdG8gV2hvc0xvb2tpbmcAgQkIKwAJCwCBDgt0aGUAOAhjb250ZW50LCBwcm92aQBYBU9BdXRoIGhlYWRlcnMgYXMgc3VwcGxpZWQgYnkgSklSQQpub3RlIHJpZ2h0IG9mAGwMOiB2ZXJpZnkANg4Kb3B0IElmIHdlJ3JlIG1pc3MAgT8FdmF0YXIgVVJMcyBvciBmdWxsIG5hbWVzIG9mIGFueQCCOgVlcgAKBQCCKwUgKGFzeW5jIHRhc2ssIGRvZXMgbm90IGJsb2NrAIIHCHJlbmRlcmluZykKAIIECy0AgwoRdXNlciAAgzgHIGZyb20gUkVTVCBBUEkuAIMRBi0-AIIsDwCDEgwAMgwuAIFyHHN0b3JlAF4PAIFnBXR1cmUgcgCEIgcKZW5kAII4HGdlbmVyYXRlIHNpZ25lZCBjb29raWUAgVQNPi0AhDIVAINXDiAoaS5lLiBIVE1MIGFuZCBKYXZhU2NyaXB0IHRvAII0BwCEcQllciBsaXN0ACQFcG9sbACEUwwpADoFc2V0cyBhAIEHDgCFNAoAgl0FSUQKbG9vcCBYSFIAgWUJAIJpBndpdGhpbgCEfwsAhR0WUG9sbHMAhU4MAIEkBmdpc3RlciBjdXJyZW50AINIBmFzIGEAgTIIb2YAhl0MAIYuDACBIA50byBjb25maXJtIElEAIU0I2lkZW50aXR5IGluAIJnHwCHQQsAgkIFb2YAhUQMAId6DGVuZA&s=modern-blue)
