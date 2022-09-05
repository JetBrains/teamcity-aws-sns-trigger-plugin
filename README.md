# TeamCity AWS SNS Trigger

[![official JetBrains project](http://jb.gg/badges/official.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)

[//]: # ([![plugin status]&#40;)

[//]: # (https://teamcity.jetbrains.com/app/rest/builds/buildType:&#40;id:TeamCityPluginsByJetBrains_TeamcityGoogleStorage_Build&#41;/statusIcon.svg&#41;]&#40;https://teamcity.jetbrains.com/viewType.html?buildTypeId=TeamCityPluginsByJetBrains_TeamcityGoogleStorage_Build&guest=1&#41;)

This plugin enables new type of build trigger. Currently, plugin supports only HTTP(S) subscription type.

# Features

When installed and configured, the plugin:

* allows build triggering from multiple SNS topics
* allows basic build customization

# Download

[//]: # (You can [download the plugin]&#40;https://plugins.jetbrains.com/plugin/9634-google-artifact-storage&#41; and install it as [an additional TeamCity plugin]&#40;https://confluence.jetbrains.com/display/TCDL/Installing+Additional+Plugins&#41;.)

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