/*
 * Hibernate Helpers for Jenkins pipelines
 *
 * License: Apache License, version 2 or later.
 * See the LICENSE.txt file in the root directory or <https://www.apache.org/licenses/LICENSE-2.0>.
 */
package org.hibernate.jenkins.pipeline.helpers.scm

class ScmSource {
	final String remoteUrl
	final String gitHubRepoId
	final ScmBranch branch
	final ScmPullRequest pullRequest

	ScmSource(env, scm) {
		// See https://stackoverflow.com/a/38255364/6692043
		remoteUrl = scm.getUserRemoteConfigs()[0].getUrl()
		def gitHubUrlMatcher = (remoteUrl =~ /^(?:git@github.com:|https:\/\/github\.com\/)([^\/]+)\/([^.]+)\.git$/)
		if (gitHubUrlMatcher.matches()) {
			String owner = gitHubUrlMatcher.group(1)
			String name = gitHubUrlMatcher.group(2)
			gitHubRepoId = owner + '/' + name
		}
		else {
			gitHubRepoId = null
		}
		if (env.CHANGE_ID) {
			def source = new ScmBranch(name: env.CHANGE_BRANCH)
			def target = new ScmBranch(name: env.CHANGE_TARGET)
			pullRequest = new ScmPullRequest(id: env.CHANGE_ID, source: source, target: target)
			branch = source
		}
		else {
			branch = new ScmBranch(name: env.BRANCH_NAME)
			pullRequest = null
		}
	}

	@Override
	String toString() {
		"ScmSource(remoteUrl: $remoteUrl, gitHubRepoId: $gitHubRepoId, branch: $branch, pullRequest: $pullRequest)"
	}
}