/*
 * Hibernate Helpers for Jenkins pipelines
 *
 * License: Apache License, version 2 or later.
 * See the LICENSE.txt file in the root directory or <https://www.apache.org/licenses/LICENSE-2.0>.
 */
package org.hibernate.jenkins.pipeline.helpers.job

import org.hibernate.jenkins.pipeline.helpers.job.configuration.JobConfiguration
import org.hibernate.jenkins.pipeline.helpers.scm.ScmSource

class JobHelper {
	private final def script

	final ScmSource scmSource

	final JobConfiguration configuration

	private boolean configured = false

	JobHelper(def script) {
		this.script = script
		this.scmSource = new ScmSource(script.env, script.scm)
		this.configuration = new JobConfiguration(script, scmSource)
	}

	void runWithNotification(Closure scriptBody) {
		Throwable mainScriptException = null
		try {
			scriptBody()
		}
		catch (any) {
			mainScriptException = any
			throw any
		}
		finally {
			/*
			 * Set the build result so that the email notifications correctly report it.
			 * We have to do it manually because Jenkins only does that automatically *after* the build.
			 */
			if (mainScriptException) {
				script.currentBuild.result = 'FAILURE'
			}
			else {
				script.currentBuild.result = 'SUCCESS'
			}

			try {
				notifyBuildEnd()
			}
			catch (notifyException) {
				if (mainScriptException != null) {
					// We are already throwing an exception, just register the new one as suppressed
					mainScriptException.addSuppressed(notifyException)
				}
				else {
					// We are not already throwing an exception, we can rethrow the new one
					throw notifyException
				}
			}
		}
	}

	def loadYamlConfiguration(String yamlConfigFileId) {
		try {
			script.configFileProvider([script.configFile(fileId: yamlConfigFileId, variable: "FILE_PATH")]) {
				return script.readYaml(file: script.FILE_PATH)
			}
		}
		catch (Exception e) {
			script.echo "Failed to load configuration file '$yamlConfigFileId'; assuming empty file. Exception was: $e"
			return [:]
		}
	}

	void configure(@DelegatesTo(JobConfiguration.DSLElement) Closure closure) {
		script.echo "SCM source: $scmSource"
		configuration.setup(closure)
		configured = true
	}

	void markStageSkipped() {
		org.jenkinsci.plugins.pipeline.modeldefinition.Utils.markStageSkippedForConditional(script.STAGE_NAME)
	}

	void withMavenWorkspace(Closure body) {
		withMavenWorkspace([:], body)
	}

	void withMavenWorkspace(Map args, Closure body) {
		checkConfigured()
		initWorkspace()
		args.putIfAbsent('jdk', configuration.jdk.defaultTool)
		args.putIfAbsent('maven', configuration.maven.defaultTool)
		args.putIfAbsent('options', [script.artifactsPublisher(disabled: true)])
		args.putIfAbsent('mavenLocalRepo', configuration.maven.localRepositoryPath)
		script.withMaven(args, body)
	}

	private void checkConfigured() {
		if (!configured) {
			throw new IllegalStateException(
					"The JobHelper was not configured." +
							" Call helper.configure(...) with the appropriate arguments before you use this method."
			)
		}
	}

	private void initWorkspace() {
		checkConfigured()

		def maven = configuration.maven

		if (maven.producedArtifactPatterns) {
			/*
			 * Remove our own artifacts from the local Maven repository,
			 * because they might have been created by another build executed on the same node,
			 * and thus might result from a different revision of the source code.
			 * We copy the built artifacts from one stage to another explicitly when necessary; see resumeFromDefaultBuild().
			 */
			script.cleanWs(
					deleteDirs: true,
					patterns: maven.producedArtifactPatterns.collect(
							{ pattern -> [type: 'INCLUDE', pattern: "$maven.localRepositoryPath/$pattern"] }
					)
			)
		}

		/*
		 * Remove everything unless we know it's safe, to prevent previous builds from interfering with the current build.
		 * Keep the local Maven repository and Git metadata, since they may be reused safely.
		 */
		script.cleanWs(deleteDirs: true, patterns: [
				// The Maven repository is safe, we cleaned it up just above
				[type: 'EXCLUDE', pattern: "$maven.localRepositoryPath/**"],
				// The Git metadata is safe, we check out the correct branch just below
				[type: 'EXCLUDE', pattern: ".git/**"]
		])

		// Check out the code
		script.checkout script.scm

		// Take tracking configuration into account
		String base = configuration.tracking.fileSection?.base
		if (base) {
			// Add the configured remotes, just in case the "base" references one of these
			if (configuration.file?.scm?.remotes) {
				def remotesConfiguration = configuration.file.scm.remotes
				script.echo "Adding configured remotes"
				remotesConfiguration.each { remoteName, remoteConfig ->
					// Remove the remote if it was added in a previous build
					script.sh "git remote remove '$remoteName' 1>&2 2>/dev/null || true"
					script.sh "git remote add -f '$remoteName' '$remoteConfig.url'"
				}
			}
			script.echo "Merging with tracking base: $base"
			script.sh "git merge $base"
		}
	}

	private void notifyBuildEnd() {
		def currentBuild = script.currentBuild
		boolean success = currentBuild.result == 'SUCCESS'
		boolean successAfterSuccess = success &&
				currentBuild.previousBuild != null && currentBuild.previousBuild.result == 'SUCCESS'

		String explicitRecipients = null

		// Always notify people who explicitly requested a build
		def recipientProviders = [script.requestor()]

		// In case of failure, notify all the people who committed a change since the last non-broken build
		if (!success) {
			script.echo "Notification recipients: adding culprits()"
			recipientProviders.add(script.culprits())
		}
		// Always notify the author of the changeset, except in the case of a "success after a success"
		if (!successAfterSuccess) {
			script.echo "Notification recipients: adding developers()"
			recipientProviders.add(script.developers())
		}

		// Notify the notification recipients configured on the job,
		// except in the case of a non-tracking PR build or of a "success after a success"
		if ((!scmSource.pullRequest || scmSource.branch.tracking) && !successAfterSuccess) {
			explicitRecipients = configuration.file?.notification?.email?.recipients
		}

		// See https://plugins.jenkins.io/email-ext#Email-extplugin-PipelineExamples
		script.emailext(
				subject: '${DEFAULT_SUBJECT}',
				body: '${DEFAULT_CONTENT}',
				recipientProviders: recipientProviders,
				to: explicitRecipients
		)
	}

}

