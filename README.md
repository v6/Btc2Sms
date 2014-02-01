
# Vision

Let's assume we want to bring Bitcoin to the 4 Billion people that are not connected to the internet, and let's further assume we want to do it by SMS, because feature phones have high global penetration. Let's then build an android app that is open source, works in a mesh network to be censorship resistant and can be installed on cheap devices to infiltrate any providers network and open a wallet for any participant.

EnvayaSMS architecture was a central control server. The objective is to get rid of it step by step and create the previously described architecture.


## Build

set <sdk><path> in mobile/pom.xml, then build with mvn clean install

## Tip4Commit

[![tip for next commit](http://tip4commit.com/projects/530.svg)](http://tip4commit.com/projects/530)

## Old


EnvayaSMS is an Android app that acts as a SMS and MMS gateway. 

For more information and installation instructions, 
see http://sms.envaya.org/

This project uses ant (or NetBeans) as the build tool.
Before building, first create a local.properties file in the project root directory,
and set sdk.dir to the path of your android sdk, e.g.:

sdk.dir=C:\\android-sdk

The code is released under the MIT license; see LICENSE