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
        def config = project.android.defaultConfig
        def oldVersionName = config.versionName
        def newVersionName = getNewVersionName(oldVersionName)
        println "\nChaiOne Gradle build plugin activate!\n   Version Name: ${versionName}\n   Version Code: ${versionCode}"
        
        config.versionName = newVersionName
        config.versionCode = versionCode
        performBuild(variant.name)

        // Break execution here
        // throw new GradleException('error occurred')
        
        uploadApk(variant)
        uploadReleaseNotes(variant)
      }
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

    // Match versionCode followed by space followed by decimal number
    def pattern = ~"versionCode\\s+\\d+"
    def matcher = pattern.matcher(fileText)
    matcher.find()

    def versionCodeContent = matcher.replaceAll("versionCode " + versionCode)
    myFile.write(versionCodeContent)
    println "Saved versionCode " + versionCode
  }

  /**
   * The base value for the versionName is read from the android.defaultConfig section of the build.gradle file. 
   * If the current Git branch is something other than master or dev, then the branch name will be appended, 
   * resulting in a version name such as 1.0.0-feature-branch. 
   *
   * @param currentName The current versionName in buld.gradle.
   */
  def getNewVersionName(currentName) {
    println "Previous versionName was " + currentName

    def result

    // Get a string that includes a dash followed by any characters
    def pattern2 = ~"-.*"
    // Subtract above string from the version name
    def numericDecimal = currentName - pattern2

    def branch = execShellCommand('git', ['rev-parse', '--abbrev-ref', 'HEAD'])
    if(branch == "master" || branch == "dev") {
      result = numericDecimal
    }
    else {
      result = "${numericDecimal}-${branch}".toString()
    }

    saveVersionName(result)
    
    // Return the resulting versionName
    result
  }

  /**
   * Save the versionName in project's buld.gradle file.
   */
  def saveVersionName(newVersionName) {
     def myFile = new File("${project.getBuildFile()}")
     def fileText = myFile.text
     
     // Match versionName followed by space followed by any number of characters
     def pattern = ~"versionName\\s+.+"
     def matcher = pattern.matcher(fileText)
     matcher.find()

     def versionNameContent = matcher.replaceAll("versionName " + "\"" + newVersionName + "\"")
     myFile.write(versionNameContent)
     println "Saved new versionName as " + newVersionName
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