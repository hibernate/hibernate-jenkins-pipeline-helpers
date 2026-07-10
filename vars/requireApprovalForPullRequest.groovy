import hudson.model.Cause
import hudson.model.User
import jenkins.scm.api.SCMSource
import org.jenkinsci.plugins.github_branch_source.Connector
import org.jenkinsci.plugins.github_branch_source.GitHubSCMSource
import org.jenkinsci.plugins.workflow.steps.FlowInterruptedException
import org.jenkinsci.plugins.workflow.steps.TimeoutStepExecution
import org.kohsuke.github.GHEvent

def call(String approvalGroup) {
	call(approvalGroup, [:])
}

def call(String approvalGroup, Map args) {
	args.putIfAbsent('initialInputTimeoutMinutes', 5)
	args.putIfAbsent('maxInputTimeoutMinutes', 160)

	StringBuilder log = new StringBuilder()
	boolean requireApproval = needsApproval(log, approvalGroup)

	def reason = log.toString().lines()
			.collect({ it -> '    ' + it })
			.join('\n')
	if (!requireApproval) {
		echo """No approval required.
Reason:
${reason}
"""
		return
	}

	// Try the GHA-aware approval flow.
	// On any unexpected error, fall back to the plain input step (old behavior).
	try {
		def ghConfig = getGitHubSCMConfig()
		if (ghConfig != null && env.CHANGE_ID) {
			if (waitForGHAOrManualApproval(approvalGroup, args, ghConfig, reason)) {
				return
			}
		}
	} catch (FlowInterruptedException e) {
		throw e
	} catch (e) {
		echo "GitHub Actions approval check encountered an error: ${e.message}"
		echo "Falling back to standard manual approval."
	}

	// Fallback: plain input (old behavior, no timeout)
	showInputForApproval(approvalGroup, reason, null)
}

private boolean waitForGHAOrManualApproval(String approvalGroup, Map args, Map ghConfig, String reason) {
	def prInfo = doFetchPrInfo(ghConfig)
	def sha = prInfo?.sha
	if (!sha) {
		return false
	}

	if (doCheckGHAWorkflowRuns(ghConfig, sha)) {
		echo "Approved: GitHub Actions workflow runs found for commit ${sha}"
		return true
	}

	int inputTimeout = (int) args.initialInputTimeoutMinutes
	int maxTimeout = (int) args.maxInputTimeoutMinutes
	def lastPrUpdatedAt = prInfo.updatedAt

	echo "Waiting for GitHub Actions approval or manual approval..."

	while (true) {
		try {
			timeout(time: inputTimeout, unit: 'MINUTES') {
				showInputForApproval(approvalGroup, reason, inputTimeout)
			}
			echo "Approved: manual approval on Jenkins"
			return true
		} catch (FlowInterruptedException e) {
			if (!isTimeout(e)) {
				throw e
			}
		}

		try {
			if (doCheckGHAWorkflowRuns(ghConfig, sha)) {
				echo "Approved: GitHub Actions workflow runs found for commit ${sha}"
				return true
			}

			prInfo = doFetchPrInfo(ghConfig)
			if (lastPrUpdatedAt != null && prInfo?.updatedAt != null
					&& prInfo.updatedAt != lastPrUpdatedAt) {
				echo "PR activity detected — resetting check interval"
				inputTimeout = (int) args.initialInputTimeoutMinutes
			} else {
				inputTimeout = (int) Math.min(inputTimeout * 2, maxTimeout)
			}
			lastPrUpdatedAt = prInfo?.updatedAt
		} catch (e) {
			echo "GitHub API check failed: ${e.message}. Will retry next cycle."
			inputTimeout = (int) Math.min(inputTimeout * 2, maxTimeout)
		}
	}
}

private void showInputForApproval(String approvalGroup, String reason, Integer nextCheckMinutes) {
	def message = """Approval is required to build pull request ${env.CHANGE_ID}.
Reason:
${reason}

Please check the code seems safe to build: no attempt to abuse resources, inspect secrets, etc."""
	if (nextCheckMinutes != null) {
		message += """
Approve the GitHub Actions run to auto-approve here, or approve manually below.
(Next auto-check in ~${nextCheckMinutes} min.)"""
	}
	input message: message, submitter: approvalGroup
}

@NonCPS
private boolean isTimeout(FlowInterruptedException e) {
	return e.getCauses()?.getAt(0) instanceof TimeoutStepExecution.ExceededTimeout
}

// ---------- GitHub API ----------

@NonCPS
private Map getGitHubSCMConfig() {
	def job = currentBuild.rawBuild?.parent
	if (job == null) {
		return null
	}
	def scmSource = SCMSource.SourceByItem.findSource(job)
	if (!(scmSource instanceof GitHubSCMSource)) {
		return null
	}
	return [
		credentialsId: scmSource.credentialsId,
		apiUri       : scmSource.apiUri ?: 'https://api.github.com',
		repoFullName : "${scmSource.repoOwner}/${scmSource.repository}",
		repoOwner    : scmSource.repoOwner
	]
}

@NonCPS
private Map doFetchPrInfo(Map ghConfig) {
	def github = connectToGitHub(ghConfig)
	try {
		def repo = github.getRepository(ghConfig.repoFullName)
		def pr = repo.getPullRequest(env.CHANGE_ID as int)
		return [sha: pr.head.sha, updatedAt: pr.updatedAt?.toString()]
	} finally {
		Connector.release(github)
	}
}

@NonCPS
private boolean doCheckGHAWorkflowRuns(Map ghConfig, String sha) {
	def github = connectToGitHub(ghConfig)
	try {
		def repo = github.getRepository(ghConfig.repoFullName)
		def iterator = repo.queryWorkflowRuns()
				.headSha(sha)
				.event(GHEvent.PULL_REQUEST)
				.list()
				.withPageSize(1)
				.iterator()
		return iterator.hasNext()
	} finally {
		Connector.release(github)
	}
}

@NonCPS
private connectToGitHub(Map ghConfig) {
	def job = currentBuild.rawBuild?.parent
	def creds = Connector.lookupScanCredentials(
			(hudson.model.Item) job,
			ghConfig.apiUri, ghConfig.credentialsId, ghConfig.repoOwner)
	return Connector.connect(ghConfig.apiUri, creds)
}

// ---------- Existing methods (unchanged) ----------

boolean needsApproval(StringBuilder log, String approvalGroup) {
	String prAuthorId = env.CHANGE_AUTHOR

	if (!prAuthorId) {
		log.append("Not a pull request build.\n")
		return false
	}

	log.append("Pull request submitted by '${prAuthorId}'.\n")
	if (isMember(log, prAuthorId, approvalGroup)) {
		return false
	}

	String buildRequesterId = currentBuild.rawBuild?.getCause(Cause.UserIdCause.class)?.getUserId()
	if (buildRequesterId) {
		log.append("Build requested by '${buildRequesterId}'.\n")
		if (isMember(log, buildRequesterId, approvalGroup)) {
			return false
		}
	}
	else {
		log.append("Build not requested by a user.\n")
	}

	return true
}


boolean isMember(StringBuilder log, String userId, String groupId) {
	def user = User.getById(userId, false)
	if (!user) {
		log.append("Jenkins user '${userId}' does not exist, or has never logged in on this Jenkins instance for a long time.\n")
		return false
	}
	def auths = user.getAuthorities()
	if (auths?.contains(groupId)) {
		log.append("Jenkins user '${userId}' is a member of '${groupId}'.\n")
		return true
	}
	else if (auths) {
		log.append("Jenkins user '${userId}' is not a member of '${groupId}'.\n")
		return false
	}
	else {
		log.append("Jenkins user '${userId}' is not a member of '${groupId}', or hasn't logged in on this Jenkins instance for a long time.\n")
		return false
	}
}
