#Who's Looking PDV Tests#

A post-deployment verification test suite for Who's Looking Connect.

This suite can run against a local dev Who's Looking setup, a production deployment, and anything in between.

The tests require a running Who's Looking instance, and a running JIRA with the Who's Looking add-on pre-installed. 

The JIRA instance may be a standalone JIRA (e.g. for local testing) or a JIRA OnDemand instance (e.g. for staging or production post-deployment verification). The tests make some assumptions about the data in JIRA (see Assumptions below).


##Assumptions##

 * The Who's Looking add-on is pre-installed in the JIRA instance. **The suite will *not* automatically install the add-on in JIRA.**
 * There are 2 JIRA users available with which the suite can log.  **The suite will *not* automatically create users in JIRA.**
 * There is 1 publicly visible JIRA issue.  **The suite will *not* automatically create any issues or enable anonymous access.**

##Usage##

Run like:

    mvn3 test [-Pprofile] [-Doption=value]   

Example:

    mvn3 test -Plocal -Dadmin.password=foobar -Dadmin.displayname="Naboo the Enigma"

Approx. test execution time is over 1 minute and under 10 minutes.

### Profiles ###

There are profiles like `local`, `dev`, `stage` and `production` which override the default base urls (but values specified directly on the command line still take precedence).

See `pom.xml` for details.

###Options###

Available config options

 * `xvfb.enable`  (default: `true`)
    * Set to false to see what WebDriver is doing.
 * `baseurl.whoslooking` (default: `http://localhost:9000`)
 * `baseurl.jira` (default: `http://localhost:2990/jira`)
 * `http.jira.port` (default: )
    *  Must be specified if `baseurl.jira` is overridden, even if the port is in already the URL (this looks like an atlassian-webdriver glitch).
 * `public.issue.key`  (default: `DEMO-1`)
 * `admin.username`  (default: `whoslooking-admin`)
 * `admin.password`  (default: `admin`)
 * `admin.displayname`  (default: `Admin Full Name`)
 * `user.username`  (default: `whoslooking-user`)
 * `user.password`  (default: `user`)
 * `user.displayname`  (default: `User Full Name`)

###Debugging Failure###

The WebDriver tests will dump screenshots to directory `test-output` on failure, which may be useful.
