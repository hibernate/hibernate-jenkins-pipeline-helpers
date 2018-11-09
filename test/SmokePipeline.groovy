/*
 * Hibernate Helpers for Jenkins pipelines
 *
 * License: Apache License, version 2 or later.
 * See the LICENSE.txt file in the root directory or <https://www.apache.org/licenses/LICENSE-2.0>.
 */

import com.lesfurets.jenkins.unit.global.lib.Library
@Library('hibernate-jenkins-pipeline-helpers@master')
import org.hibernate.jenkins.pipeline.helpers.job.JobHelper
import org.hibernate.jenkins.pipeline.helpers.alternative.AlternativeMultiMap
import org.hibernate.jenkins.pipeline.helpers.version.Version

def execute() {
	JobHelper helper = new JobHelper(this)

	helper.runWithNotification {
		AlternativeMultiMap<StubEnvironment> environments = AlternativeMultiMap.create([
				jdk: [
						new StubEnvironment('env1', false),
						new StubEnvironment('env2', true),
						new StubEnvironment('env3', false)
				]
		])

		stage('Configure') {
			helper.configure({
				configurationNodePattern 'master||whatever'
				file 'job-configuration.yaml'
				maven {
					defaultTool 'THE Maven default tool'
					producedArtifactPattern "org/hibernate/search/*"
					producedArtifactPattern "org/hibernate/hibernate-search*"
				}
				jdk {
					defaultTool environments.content.jdk.default.name
				}
			})

			properties([
					pipelineTriggers(
							[
									issueCommentTrigger('.*test this please.*'),
									// Normally we don't have snapshot dependencies, so this doesn't matter, but some branches do
									snapshotDependencies()
							]
									+ helper.generateUpstreamTriggers()
					),
					helper.generateNotificationProperty()
			])

			environments.content.each { key, envSet ->
				// No need to re-test default environments, they are already tested as part of the default build
				envSet.enabled.remove(envSet.default)
				envSet.enabled.removeAll { env -> "env1" == env.name }
			}

			echo "Enabled environments: $environments.enabledAsString"

			def releaseVersion = Version.parseReleaseVersion('5.10.2.Final')
			def developmentVersion = Version.parseDevelopmentVersion('5.10.3-SNAPSHOT')

			echo "Release version: $releaseVersion, family: $releaseVersion.family"
			echo "Development version: $developmentVersion, family: $developmentVersion.family"
		}

		stage('Build') {
			node() {
				helper.withMavenWorkspace {
					sh "mvn clean install"
				}
			}
		}
	}
}

class StubEnvironment {
	final String name
	final boolean isDefault

	StubEnvironment(String name, boolean isDefault) {
		this.name = name
		this.isDefault = isDefault
	}

	boolean isDefault() {
		isDefault
	}

	String toString() {
		name
	}
}

return this