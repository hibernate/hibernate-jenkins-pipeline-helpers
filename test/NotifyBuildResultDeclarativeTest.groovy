/*
 * Hibernate Helpers for Jenkins pipelines
 *
 * License: Apache License, version 2 or later.
 * See the LICENSE.txt file in the root directory or <https://www.apache.org/licenses/LICENSE-2.0>.
 */

import com.lesfurets.jenkins.unit.declarative.DeclarativePipelineTest
import jenkinsapis.EnvStub
import jenkinsapis.GitScmStub
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

import static com.lesfurets.jenkins.unit.global.lib.LibraryConfiguration.library
import static com.lesfurets.jenkins.unit.global.lib.LocalSource.localSource

class NotifyBuildResultDeclarativeTest extends DeclarativePipelineTest {
	private static final SCRIPT_NAME = "NotifyBuildResultDeclarativePipeline.groovy"

	private Map jobConfigurationFile = [
			'notification': [
					'gitter': [
							'urlCredentialsId': 'some-id'
					]
			]
	]

	boolean doStuffShouldSucceed = true

	@Override
	@Before
	void setUp() throws Exception {
		String sharedLibs = this.class.getResource('./').getFile()

		def library = library()
				.name('hibernate-jenkins-pipeline-helpers')
				.allowOverride(true)
				.retriever(localSource(sharedLibs))
				.targetPath(sharedLibs)
				.defaultVersion("main")
				.implicit(false)
				.build()
		helper.registerSharedLibrary(library)

		setScriptRoots([ 'src', 'test', 'vars' ] as String[])
		setScriptExtension('groovy')

		helper.registerAllowedMethod("post", [Closure.class], {Closure c ->
			c.delegate = delegate
			helper.callClosure(c)
		})
		helper.registerAllowedMethod("always", [Closure.class], {Closure c ->
			c.delegate = delegate
			helper.callClosure(c)
		})

		helper.registerAllowedMethod("doStuff", [], {String args ->
			if (!doStuffShouldSucceed) {
				throw new RuntimeException("doStuff() failed!");
			}
		})

		helper.registerAllowedMethod("emailext", [Map.class], null)
		helper.registerAllowedMethod("requestor", [], {String args -> [type: 'requestor', args: args]})
		helper.registerAllowedMethod("developers", [], {String args -> [type: 'developers', args: args]})
		helper.registerAllowedMethod("culprits", [], {String args -> [type: 'developers', args: args]})

		binding.setVariable('scm', new GitScmStub())

		super.setUp()
	}

	@Test
	void branch_success() throws Exception {
		binding.setVariable('env', new EnvStub())
		doStuffShouldSucceed = true
		def script = runScript(SCRIPT_NAME)
		assertJobStatusSuccess()
	}

	@Test
	@Ignore // post() doesn't work in case of failures due to a bug in the testing framework: https://github.com/jenkinsci/JenkinsPipelineUnit/issues/338
	void branch_failure() throws Exception {
		binding.setVariable('env', new EnvStub())
		doStuffShouldSucceed = false
		def script = runScript(SCRIPT_NAME)
		assertJobStatusFailure()
	}

	@Test
	void pullRequest_success() throws Exception {
		binding.setVariable('env', new EnvStub(CHANGE_ID: "PR 2", CHANGE_TARGET: "targetBranch", CHANGE_BRANCH: "sourceBranch"))
		doStuffShouldSucceed = true
		def script = runScript(SCRIPT_NAME)
		assertJobStatusSuccess()
	}

	@Test
	@Ignore // post() doesn't work in case of failures due to a bug in the testing framework: https://github.com/jenkinsci/JenkinsPipelineUnit/issues/338
	void pullRequest_failure() throws Exception {
		binding.setVariable('env', new EnvStub(CHANGE_ID: "PR 2", CHANGE_TARGET: "targetBranch", CHANGE_BRANCH: "sourceBranch"))
		doStuffShouldSucceed = false
		def script = runScript(SCRIPT_NAME)
		assertJobStatusFailure()
	}

	@Test
	void trackingBranch_success() throws Exception {
		binding.setVariable('env', new EnvStub(BRANCH_NAME: "tracking-foo"))

		jobConfigurationFile.tracking = [
		        'foo': [
						'base': 'base-branch',
						'tracked': ['tracked-job-1', 'tracked-job-2']
		        ]
		]

		doStuffShouldSucceed = true
		def script = runScript(SCRIPT_NAME)
		assertJobStatusSuccess()
	}

	@Test
	@Ignore // post() doesn't work in case of failures due to a bug in the testing framework: https://github.com/jenkinsci/JenkinsPipelineUnit/issues/338
	void trackingBranch_failure() throws Exception {
		binding.setVariable('env', new EnvStub(BRANCH_NAME: "tracking-foo"))

		jobConfigurationFile.tracking = [
				'foo': [
						'base': 'base-branch',
						'tracked': ['tracked-job-1', 'tracked-job-2']
				]
		]

		doStuffShouldSucceed = false
		def script = runScript(SCRIPT_NAME)
		assertJobStatusSuccess()
	}
}
