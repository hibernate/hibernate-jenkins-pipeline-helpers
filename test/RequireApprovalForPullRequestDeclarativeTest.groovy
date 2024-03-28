/*
 * Hibernate Helpers for Jenkins pipelines
 *
 * License: Apache License, version 2 or later.
 * See the LICENSE.txt file in the root directory or <https://www.apache.org/licenses/LICENSE-2.0>.
 */


import com.lesfurets.jenkins.unit.declarative.DeclarativePipelineTest
import hudson.model.Cause
import hudson.model.User
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.MockedStatic

import static com.lesfurets.jenkins.unit.global.lib.LibraryConfiguration.library
import static com.lesfurets.jenkins.unit.global.lib.LocalSource.localSource
import static org.mockito.Mockito.*

class RequireApprovalForPullRequestDeclarativeTest extends DeclarativePipelineTest {
	private static final SCRIPT_NAME = "RequireApprovalForPullRequestDeclarativePipeline.groovy"
	static MockedStatic<User> userClassMock

	@BeforeAll
	static void createMock() {
		userClassMock = mockStatic(User)

		def yrodiereMock = mock(User)
		userClassMock.when { User.getById('yrodiere', false) }
				.thenReturn(yrodiereMock)
		when(yrodiereMock.getAuthorities()).thenReturn(['hibernate*ci', 'hibernate'])

		def fooMock = mock(User)
		userClassMock.when { User.getById('foo', false) }
				.thenReturn(fooMock)
		when(fooMock.getAuthorities()).thenReturn(['not-quite.hibernate', 'hibernate.but-not-quite'])
	}

	@AfterAll
	static void closeMock() {
		userClassMock.close()
	}

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

		helper.registerAllowedMethod("input", [Map])
		helper.registerAllowedMethod("doStuff", [], {String args ->
			echo "Doing stuff"
		})
	}

	@Test
	void branch_noApprovalRequired() throws Exception {
		def script = runScript(SCRIPT_NAME)
		assertJobStatusSuccess()
		assertCallStack().contains('no approval required')
	}

	@Test
	void pullRequest_buildRequester_noApprovalRequired() throws Exception {
		addEnvVar('CHANGE_AUTHOR', 'foo')
		addEnvVar('CHANGE_ID', 'PR 2')

		binding.getVariable('currentBuild').rawBuild = [:]
		binding.getVariable('currentBuild').rawBuild.getCause = { return new Cause.UserIdCause('yrodiere') }

		def script = runScript(SCRIPT_NAME)
		assertJobStatusSuccess()
		assertCallStack().contains('no approval required')
	}

	@Test
	void pullRequest_buildRequester_approvalRequired() throws Exception {
		addEnvVar('CHANGE_AUTHOR', 'foo')
		addEnvVar('CHANGE_ID', 'PR 2')

		binding.getVariable('currentBuild').rawBuild = [:]
		binding.getVariable('currentBuild').rawBuild.getCause = { return new Cause.UserIdCause('foo') }

		def script = runScript(SCRIPT_NAME)
		assertJobStatusSuccess()
		assertCallStack().doesNotContain('no approval required')
	}

	@Test
	void pullRequest_prAuthor_noApprovalRequired() throws Exception {
		addEnvVar('CHANGE_AUTHOR', 'yrodiere')
		addEnvVar('CHANGE_ID', 'PR 2')

		def script = runScript(SCRIPT_NAME)
		assertJobStatusSuccess()
		assertCallStack().contains('no approval required')
	}

	@Test
	void pullRequest_prAuthor_approvalRequired() throws Exception {
		addEnvVar('CHANGE_AUTHOR', 'foo')
		addEnvVar('CHANGE_ID', 'PR 2')

		def script = runScript(SCRIPT_NAME)
		assertJobStatusSuccess()
		assertCallStack().doesNotContain('no approval required')
	}
}
