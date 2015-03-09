# ChaiOne Gradle Build Plugin

This plugin sets the `versionName` & `versionCode`, builds the APK, and uploads it along with any release notes to Amazon S3 for inclusion in the internal app store.

## Version Name

The base value for the `versionName` is read from the `android.defaultConfig` section of the `build.gradle` file. If the current Git branch is something other than `master` or `dev`, then the branch name will be appended, resulting in a version name such as `1.0.0-feature-branch`.

## Version Code

The value for `versionCode` is simply the Git revision number. If you commit changes, the version code changes. It's that simple. No more remembering to increment the version code with each release to Google Play!

## Building the APK

For each build variant in your `build.gradle` file, you'll see corresponding tasks in the output of the `gradle tasks` command. The default variants of release and debug will create `internalAppStoreDeployRelease` and `internalAppStoreDeployDebug` tasks respectively. The plugin runs the associated `assembleRelease` or `assembleDebug` command after setting the version name/code.

## Release Notes

Release notes can optionally be included with each app store deployment. The plugin looks for release notes in the build project directory (where the `build.gradle` file is located) with the same name as the APK but with a `_RELEASE_NOTES.md` suffix in place of the `.apk` suffix of the build output. If your APK is named `app-debug.apk`, then your release notes file would be `app-debug_RELEASE_NOTES.md`. In this case, the release notes file for the release build would be named `app-release_RELEASE_NOTES.md`.

## Uploading to Amazon S3

After the build is completed, the APK and any release notes are uploaded to the internal app store's inbox on Amazon S3. Roughly every 10 minutes, any files placed there are processed and reflected at [appstore.chaione.com](http://appstore.chaione.com).


# Setup

## Install s3cmd

```
brew install s3cmd
```

## Install gpg

```
brew install gpg
```

## Configure s3cmd

```
s3cmd --configure
```

Follow the prompts to add your access key, secret key, and any other options you wish to configure.

## Applying the Plugin

To add the plugin to your project, add the following to your `build.gradle` file:

```groovy
buildscript {
    repositories {
        maven { url file(getProperty('com.chaione.build.pluginRepoPath')).toURI() }
    }
    dependencies {
        classpath 'com.chaione.build:com.chaione.build:1.0'
    }
}
```

Note the Maven URL specified as `file(getProperty('com.chaione.build.pluginRepoPath')).toURI()` above. This reads a `com.chaione.build.pluginRepoPath` property from your `gradle.properties` file. If this file does not already exist, create it in the same directory as your `build.gradle` file. Specify the relative path to this plugin's repo directory, for example:

```
com.chaione.build.pluginRepoPath=../../../build-scripts/android/gradle-plugin/repo
```

Finally, apply the plugin:

```groovy
apply plugin: 'com.chaione.build'
```
