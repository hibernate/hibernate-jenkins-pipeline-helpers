import hudson.model.Cause
import hudson.model.User

def call(String approvalGroup) {
	if (!env.CHANGE_AUTHOR) {
		echo "Not a pull request build; no approval required."
		return
	}
	if (isMember(env.CHANGE_AUTHOR, approvalGroup)) {
		echo "Pull request submitted by '${env.CHANGE_AUTHOR}', who is a member of '${approvalGroup}'; no approval required."
		return
	}
	String buildRequesterId = currentBuild.rawBuild?.getCause(Cause.UserIdCause.class)?.getUserId()
	if (buildRequesterId && isMember(buildRequesterId, approvalGroup)) {
		echo "Build requested by '${buildRequesterId}', who is a member of '${approvalGroup}'; no approval required."
		return
	}
	input message: """Approval is required to build pull request ${env.CHANGE_ID} submitted by '${env.CHANGE_AUTHOR}', who is not a member of '${approvalGroup}' (or never logged in on this Jenkins instance).

Please check the code seems safe to build: no attempt to abuse resources, inspect secrets, etc.""", submitter: approvalGroup
}

static boolean isMember(String userId, String groupId) {
	return User.getById(userId, false)?.getAuthorities()?.contains(groupId)
}