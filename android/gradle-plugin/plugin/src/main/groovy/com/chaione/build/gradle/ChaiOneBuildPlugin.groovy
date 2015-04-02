package com.chaione.build.gradle;

import org.gradle.api.*

class ChaiOneBuildPlugin implements Plugin {

  Project project

  def void apply(Object project) {
    this.project = project

    // Get version code first otherwise it returns -1 for some reason
    def versionCode = getVersionCode()

    project.android.applicationVariants.all { variant ->
      project.task("internalAppStoreDeploy${variant.name.capitalize()}", description: "Deploys a ${variant.name} build to the ChaiOne internal app store.") << {
        def versionName = getVersionName()
        println "\nChaiOne Gradle build plugin activate!\n   Version Name: ${versionName}\n   Version Code: ${versionCode}"

        def config = project.android.defaultConfig
        config.versionName = versionName
        config.versionCode = versionCode

        performBuild(variant.name)

        uploadApk(variant)
        uploadReleaseNotes(variant)
      }
    }
  }

  def getVersionName() {
    def currentName = project.android.defaultConfig.versionName
    def branch = execShellCommand('git', ['rev-parse', '--abbrev-ref', 'HEAD'])

    if(branch == "master" || branch == "dev") {
      currentName
    }
    else {
      "${currentName}-${branch}".toString()
    }
  }

  def getVersionCode() {
    def result = Integer.parseInt(execShellCommand('git', ['rev-list', '--count', 'HEAD']))
    incrementVersionCode(result)
    result > 0 ? result : 1
  }

  def incrementVersionCode(versionCode) {
    println "Writing to build file: " + project.getBuildFile()
    def myFile = new File("${project.getBuildFile()}")
    def fileText = myFile.text
    def pattern = ~"versionCode\\s+\\d+"
    def matcher = pattern.matcher(fileText)
    matcher.find()
    def versionCodeContent = matcher.replaceAll("versionCode " + versionCode)
    myFile.write(versionCodeContent)
    println "Saved versionCode " + versionCode
  }

  def performBuild(variantName) {
    println "\nPerforming build...\n"
    println execShellCommand('gradle', ["assemble${variantName.capitalize()}"])
  }

  def uploadApk(variant) {
    println '\nUploading APK to S3...\n'
    println execShellCommand('s3cmd', ['put', '--recursive', '--exclude', '*-unaligned.apk', '--exclude', '*.txt', "${project.getBuildDir()}/outputs/apk/", 's3://chaione-app-store/_inbox/'])
  }

  def uploadReleaseNotes(variant) {
    variant.outputs.each { output ->
      def outputFile = output.outputFile
      if (outputFile != null && outputFile.name.endsWith('.apk') && !outputFile.name.contains('-unaligned')) {
        def releaseNotesFileName = outputFile.name.replace('.apk', '_RELEASE_NOTES.md')
        if(project.file(releaseNotesFileName).exists()) {
          println '\nUploading release notes to S3...\n'
          println execShellCommand('s3cmd', ['put', "${project.getProjectDir()}/${releaseNotesFileName}", "s3://chaione-app-store/_inbox/${releaseNotesFileName}"])
        }
      }
    }
  }

  def execShellCommand(command, arguments) {
    def stdout = new ByteArrayOutputStream()

    project.exec {
      executable command
      args arguments
      standardOutput = stdout
    }
    stdout.toString().trim()
  }
}