# TeamCity AWS SNS Trigger

[![official JetBrains project](http://jb.gg/badges/official.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)
[![plugin status](
https://teamcity.jetbrains.com/app/rest/builds/buildType:(id:TeamCityPluginsByJetBrains_TeamcityAwsSnsTriggerPlugin_Build)/statusIcon.svg)](https://teamcity.jetbrains.com/viewType.html?buildTypeId=TeamCityPluginsByJetBrains_TeamcityAwsSnsTriggerPlugin_Build&guest=1)

The plugin enables a new type of build trigger. Currently, it supports only the HTTP(S) subscription type.

This plugin allows triggers from multiple SNS topics and supports basic build customization for them.

# Compatibility

Compatible with [TeamCity](https://www.jetbrains.com/teamcity/download/) **2022.04** and greater.

# Installation

1. Download the [Amazon SNS Trigger plugin](https://plugins.jetbrains.com/plugin/19879-amazon-sns-trigger/).
2. Install it as
   an [additional TeamCity plugin](https://confluence.jetbrains.com/display/TCDL/Installing+Additional+Plugins).

After that, a new trigger type is automatically added for your build configuration in TeamCity.

# Configuration

To configure AWS SNS Trigger in TeamCity, do the following:

1. Go to the build configuration.
2. Add `AWS SNS Trigger`.
3. Copy generated HTTP(S) link.
4. Add a new subscription for your SNS topic in the AWS console (see the subscription link in a trigger configuration).
5. Save the trigger configuration before sending a subscription event.

You can configure more than one trigger of that type.

## Build Customization

Trigger introduce new build parameters:

`%sns.message.subject%` - extracts Subject value from the SNS message.

`%sns.message.body%` - message body from the SNS message.

`%sns.message.attributes.<name>%` - message attributes. Replace `<name>` with actual attribute name.

Feel free to use them at your will.

Values extracted as-is from the SNS message, so all values passed as strings (null values passed as empty strings).

TeamCity doesn't support attributes with dot (.) symbol in the name.

`sns.message.attributes.<name>` is a dynamically named attribute and can't be used it static context, like build
configuration.
If you want to use values from attributes in the build configuration, create custom property and initialize it via '
Trigger Configuration' -> 'Build Customization' -> 'Build Parameters'

# Build

This project uses Gradle as the build system. You can easily open it
in [IntelliJ IDEA](https://www.jetbrains.com/idea/help/importing-project-from-gradle-model.html)
or [Eclipse](http://gradle.org/eclipse/).
To test & build the plugin, execute the `build` gradle command.

# Contributions

We appreciate all kinds of feedback, so please feel free to send a PR or write an issue.