import hudson.model.Cause
import hudson.model.User

def call(String approvalGroup) {
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

		input message: """Approval is required to build pull request ${env.CHANGE_ID}.
Reason:
${reason}

Please check the code seems safe to build: no attempt to abuse resources, inspect secrets, etc.""", submitter: approvalGroup
}

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