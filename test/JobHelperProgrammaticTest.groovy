/*
 * Hibernate Helpers for Jenkins pipelines
 *
 * License: Apache License, version 2 or later.
 * See the LICENSE.txt file in the root directory or <https://www.apache.org/licenses/LICENSE-2.0>.
 */

import com.lesfurets.jenkins.unit.BasePipelineTest
import jenkinsapis.GitScmStub
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import static com.lesfurets.jenkins.unit.global.lib.LibraryConfiguration.library
import static com.lesfurets.jenkins.unit.global.lib.LocalSource.localSource

class JobHelperProgrammaticTest extends BasePipelineTest {
	private static final SCRIPT_NAME = "JobHelperProgrammaticPipeline.groovy"
	
	private Map jobConfigurationFile = [
			'notification': [
					'gitter': [
							'urlCredentialsId': 'some-id'
					]
			]
	]


	@Override
	@BeforeEach
	void setUp() throws Exception {
		setScriptRoots(['src', 'test', 'vars'] as String[])
		setScriptExtension('groovy')

		super.setUp()

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
	}

	@Override
	void registerAllowedMethods() {
		super.registerAllowedMethods()

		helper.registerAllowedMethod("configFile", [Map.class], {Map args -> [type: 'configFile', args: args]})
		helper.registerAllowedMethod("configFileProvider", [List.class, Closure.class], {List args, Closure c ->
			args.forEach {
				binding.setVariable(it.args.variable, "/path/to/$it.args.fileId")
			}
			c.delegate = delegate
			def res = helper.callClosure(c, args)
			args.forEach {
				binding.setVariable(it.args.variable, null)
			}
			return res
		})
		helper.registerAllowedMethod("readYaml", [Map.class], {Map args ->
			if ('/path/to/job-configuration.yaml' == args.file) {
				jobConfigurationFile
			}
			else {
				throw new IllegalStateException("File not found")
			}
		})
		helper.registerAllowedMethod("cleanWs", [Map.class], null)
		helper.registerAllowedMethod("withMaven", [Map.class, Closure.class], {Map args, Closure c ->
			c.delegate = delegate
			helper.callClosure(c, args)
		})
		helper.registerAllowedMethod("artifactsPublisher", [Map.class], {Map args -> [type: 'artifactsPublisher', args: args]})

		helper.registerAllowedMethod("pipelineTriggers", [List.class], null)
		helper.registerAllowedMethod("issueCommentTrigger", [String.class], {String args -> [type: 'issueCommentTrigger', args: args]})
		helper.registerAllowedMethod("snapshotDependencies", [], {String args -> [type: 'snapshotDependencies']})
		helper.registerAllowedMethod("upstream", [String.class], {String args -> [type: 'upstream', args: args]})

		helper.registerAllowedMethod("emailext", [Map.class], null)
		helper.registerAllowedMethod("requestor", [], {String args -> [type: 'requestor', args: args]})
		helper.registerAllowedMethod("culprits", [], {String args -> [type: 'culprits', args: args]})
		helper.registerAllowedMethod("developers", [], {String args -> [type: 'developers', args: args]})
		helper.registerAllowedMethod("pwd", [Map.class], {Map args -> args.tmp ? '/path/to/workspace@tmp/' : '/path/to/workspace/'})

		helper.registerAllowedMethod("checkout", [GitScmStub.class], {GitScmStub args -> args})
	}

	@Override
	void setVariables() {
		super.setVariables()

		binding.setVariable('scm', new GitScmStub())
		addEnvVar('BRANCH_NAME', 'branch_name')
		addEnvVar('WORKSPACE', '/path/to/workspace')
	}

	@Test
	void branch() throws Exception {
		Script script = loadScript(SCRIPT_NAME)
		script.execute()
		printCallStack()
		assertJobStatusSuccess()
	}

	@Test
	void pullRequest() throws Exception {
		addEnvVar('CHANGE_ID', 'PR 2')
		addEnvVar('CHANGE_TARGET', 'targetBranch')
		addEnvVar('CHANGE_BRANCH', 'sourceBranch')

		Script script = loadScript(SCRIPT_NAME)
		script.execute()
		printCallStack()
		assertJobStatusSuccess()
	}

	@Test
	void trackingBranch() throws Exception {
		addEnvVar('BRANCH_NAME', 'tracking-foo')

		jobConfigurationFile.tracking = [
		        'foo': [
						'base': 'base-branch',
						'tracked': ['tracked-job-1', 'tracked-job-2']
		        ]
		]

		Script script = loadScript(SCRIPT_NAME)
		script.execute()
		printCallStack()
		assertJobStatusSuccess()
	}
}
