import org.hibernate.jenkins.pipeline.helpers.notification.Notifier

def call(Map args) {
	new Notifier(this).doNotifyBuildResult(args)
}
