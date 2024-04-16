import hudson.model.Cause
import hudson.model.User

def call(String approvalGroup) {
	if (!needsApproval(approvalGroup)) {
		echo "No approval required."
		return
	}
	input message: """Approval is required to build pull request ${env.CHANGE_ID}.

Please check the code seems safe to build: no attempt to abuse resources, inspect secrets, etc.""", submitter: approvalGroup
}

boolean needsApproval(String approvalGroup) {
	String prAuthorId = env.CHANGE_AUTHOR

	if (!prAuthorId) {
		echo "Not a pull request build."
		return false
	}

	echo "Pull request submitted by ${prAuthorId}"
	if (isMember(prAuthorId, approvalGroup)) {
		return false
	}

	String buildRequesterId = currentBuild.rawBuild?.getCause(Cause.UserIdCause.class)?.getUserId()
	if (buildRequesterId) {
		echo "Build requested by ${buildRequesterId}"
		if (isMember(buildRequesterId, approvalGroup)) {
			return false
		}
	}

	return true
}


static boolean isMember(String userId, String groupId) {
	def user = User.getById(userId, false)
	if (!user) {
		echo "Jenkins user '${userId}' does not exist (maybe they never logged in on this Jenkins instance?)"
		return false
	}
	if (user.getAuthorities()?.contains(groupId)) {
		echo "Jenkins user '${userId}' is a member of '${groupId}'."
		return true
	}
	else {
		echo "Jenkins user '${userId}' is not a member of '${groupId}'"
		return false
	}
}