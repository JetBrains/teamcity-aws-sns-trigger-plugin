# TeamCity AWS SNS Trigger

[![official JetBrains project](http://jb.gg/badges/official.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)
[![plugin status](
https://teamcity.jetbrains.com/app/rest/builds/buildType:(id:TeamCityPluginsByJetBrains_TeamcityAwsSnsTriggerPlugin_Build)/statusIcon.svg)](https://teamcity.jetbrains.com/viewType.html?buildTypeId=TeamCityPluginsByJetBrains_TeamcityAwsSnsTriggerPlugin_Build&guest=1)

This plugin enables new type of build trigger. Currently, plugin supports only HTTP(S) subscription type.

# Features

When installed and configured, the plugin:

* allows build triggering from multiple SNS topics
* allows basic build customization

# Download

You can [download the plugin](https://plugins.jetbrains.com/plugin/19879-amazon-sns-trigger/) and install it
as [an additional TeamCity plugin](https://confluence.jetbrains.com/display/TCDL/Installing+Additional+Plugins).

# Compatibility

The plugin is compatible with [TeamCity](https://www.jetbrains.com/teamcity/download/) **2022.04** and greater.

# Configuration

The plugin adds new trigger type for build configuration.

To configure AWS SNS Trigger you should

* go to the build configuration
* add 'AWS SNS Trigger'
* copy generated HTTP(S) link
* add new subscription for your SNS topic in AWS console (you can find link in trigger configuration)
* don't forget to save trigger configuration before sending subscription event

You can configure more than one trigger of that type.

# Build

This project uses gradle as the build system. You can easily open it
in [IntelliJ IDEA](https://www.jetbrains.com/idea/help/importing-project-from-gradle-model.html)
or [Eclipse](http://gradle.org/eclipse/).
To test & build the plugin, execute the `build` gradle command.

# Contributions

We appreciate all kinds of feedback, so please feel free to send a PR or write an issue.