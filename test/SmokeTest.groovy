/*
 * Hibernate Helpers for Jenkins pipelines
 *
 * License: Apache License, version 2 or later.
 * See the LICENSE.txt file in the root directory or <https://www.apache.org/licenses/LICENSE-2.0>.
 */

import com.lesfurets.jenkins.unit.BasePipelineTest
import jenkinsapis.EnvStub
import jenkinsapis.GitScmStub
import org.junit.Before
import org.junit.Test

import static com.lesfurets.jenkins.unit.global.lib.LibraryConfiguration.library
import static com.lesfurets.jenkins.unit.global.lib.LocalSource.localSource

class SmokeTest extends BasePipelineTest {
	private Map jobConfigurationFile = [:]

	@Override
	@Before
	void setUp() throws Exception {

		String sharedLibs = this.class.getResource('./').getFile()

		def library = library()
				.name('hibernate-jenkins-pipeline-helpers')
				.allowOverride(true)
				.retriever(localSource(sharedLibs))
				.targetPath(sharedLibs)
				.defaultVersion("master")
				.implicit(false)
				.build()
		helper.registerSharedLibrary(library)

		setScriptRoots([ 'src', 'test' ] as String[])
		setScriptExtension('groovy')

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
		helper.registerAllowedMethod("upstream", [String.class], {String args -> [type: 'issueCommentTrigger', args: args]})

		binding.setVariable('scm', new GitScmStub())
		helper.registerAllowedMethod("checkout", [GitScmStub.class], {GitScmStub args -> args})

		super.setUp()
	}

	@Test
	void branch() throws Exception {
		binding.setVariable('env', new EnvStub())
		Script script = loadScript("SmokePipeline.groovy")
		script.execute()
		printCallStack()
		assertJobStatusSuccess()
	}

	@Test
	void pullRequest() throws Exception {
		binding.setVariable('env', new EnvStub(CHANGE_ID: "PR 2", CHANGE_TARGET: "targetBranch", CHANGE_BRANCH: "sourceBranch"))
		Script script = loadScript("SmokePipeline.groovy")
		script.execute()
		printCallStack()
		assertJobStatusSuccess()
	}

	@Test
	void trackingBranch() throws Exception {
		binding.setVariable('env', new EnvStub(BRANCH_NAME: "tracking-foo"))

		jobConfigurationFile.tracking = [
		        'foo': [
						'base': 'base-branch',
						'tracked': ['tracked-job-1', 'tracked-job-2']
		        ]
		]

		Script script = loadScript("SmokePipeline.groovy")
		script.execute()
		printCallStack()
		assertJobStatusSuccess()
	}
}
