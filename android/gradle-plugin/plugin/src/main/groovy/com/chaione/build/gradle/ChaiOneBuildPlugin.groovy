package com.chaione.build.gradle;

import org.gradle.api.*;

class ChaiOneBuildPlugin implements Plugin {
  def void apply(Object project) {
    project.task('internalAppStoreDeploy') << {
      def sha1 = execShellCommand(project, 'git', ['rev-parse', '--short', 'HEAD'])
      def branch = execShellCommand(project, 'git', ['rev-parse', '--abbrev-ref', 'HEAD'])
      println "ChaiOne Build Plugin activate! ${sha1}_${branch}"
    }
  }

  def execShellCommand(project, command, arguments) {
    def stdout = new ByteArrayOutputStream()
    project.exec {
      executable command
      args arguments
      standardOutput = stdout
    }
    stdout.toString().trim()
  }
}