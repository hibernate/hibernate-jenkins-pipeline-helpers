import hudson.model.Cause
import hudson.model.User
import org.jenkinsci.plugins.workflow.steps.FlowInterruptedException
import org.jenkinsci.plugins.workflow.steps.TimeoutStepExecution

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
		def repoInfo = parseChangeUrl()
		if (repoInfo != null && env.CHANGE_ID) {
			if (waitForGHAOrManualApproval(approvalGroup, args, repoInfo, reason)) {
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

private Map parseChangeUrl() {
	def changeUrl = env.CHANGE_URL
	if (!changeUrl) {
		return null
	}
	def match = (changeUrl =~ 'https?://([^/]+)/([^/]+)/([^/]+)/pull/\\d+')
	if (!match.matches()) {
		return null
	}
	def host = match.group(1)
	def apiUri = (host == 'github.com') ? 'https://api.github.com' : "https://${host}/api/v3"
	return [
		apiUri      : apiUri,
		repoFullName: "${match.group(2)}/${match.group(3)}"
	]
}

private boolean waitForGHAOrManualApproval(String approvalGroup, Map args, Map repoInfo, String reason) {
	def prInfo = fetchPrInfo(repoInfo)
	def sha = prInfo?.sha
	if (!sha) {
		return false
	}

	if (checkGHAWorkflowRuns(repoInfo, sha)) {
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
			if (checkGHAWorkflowRuns(repoInfo, sha)) {
				echo "Approved: GitHub Actions workflow runs found for commit ${sha}"
				return true
			}

			prInfo = fetchPrInfo(repoInfo)
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

// ---------- GitHub API via httpRequest ----------

private Map fetchPrInfo(Map repoInfo) {
	def content = httpRequest(
			url: "${repoInfo.apiUri}/repos/${repoInfo.repoFullName}/pulls/${env.CHANGE_ID}",
			validResponseCodes: '200',
			quiet: true
	).content
	def json = new groovy.json.JsonSlurper().parseText(content)
	return [sha: json.head?.sha, updatedAt: json.updated_at]
}

private boolean checkGHAWorkflowRuns(Map repoInfo, String sha) {
	def content = httpRequest(
			url: "${repoInfo.apiUri}/repos/${repoInfo.repoFullName}/actions/runs?head_sha=${sha}&event=pull_request&per_page=1",
			validResponseCodes: '200',
			quiet: true
	).content
	def json = new groovy.json.JsonSlurper().parseText(content)
	return json.total_count > 0
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
