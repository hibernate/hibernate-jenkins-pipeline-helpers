/*
 * Hibernate Helpers for Jenkins pipelines
 *
 * License: Apache License, version 2 or later.
 * See the LICENSE.txt file in the root directory or <https://www.apache.org/licenses/LICENSE-2.0>.
 */


import com.lesfurets.jenkins.unit.declarative.DeclarativePipelineTest
import hudson.model.Cause
import hudson.model.Result
import hudson.model.User
import org.jenkinsci.plugins.workflow.steps.FlowInterruptedException
import org.jenkinsci.plugins.workflow.steps.TimeoutStepExecution
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.MockedStatic

import static com.lesfurets.jenkins.unit.global.lib.LibraryConfiguration.library
import static com.lesfurets.jenkins.unit.global.lib.LocalSource.localSource
import static org.junit.jupiter.api.Assertions.assertThrows
import static org.mockito.Mockito.*

class RequireApprovalForPullRequestGHADeclarativeTest extends DeclarativePipelineTest {
	private static final SCRIPT_NAME = "RequireApprovalForPullRequestDeclarativePipeline.groovy"
	static MockedStatic<User> userClassMock

	boolean ghaRunsExist = false
	String prSha = 'abc123'
	String prUpdatedAt = '2024-01-01T00:00:00Z'

	@BeforeAll
	static void createStaticMocks() {
		userClassMock = mockStatic(User)

		def fooMock = mock(User)
		userClassMock.when { User.getById('foo', false) }
				.thenReturn(fooMock)
		when(fooMock.getAuthorities()).thenReturn(['not-quite.hibernate', 'hibernate.but-not-quite'])
	}

	@AfterAll
	static void closeStaticMocks() {
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

		ghaRunsExist = false
		prSha = 'abc123'
		prUpdatedAt = '2024-01-01T00:00:00Z'
	}

	private void setupPRBuild() {
		addEnvVar('CHANGE_AUTHOR', 'foo')
		addEnvVar('CHANGE_ID', '42')
		addEnvVar('CHANGE_URL', 'https://github.com/hibernate/hibernate-orm/pull/42')

		binding.getVariable('currentBuild').rawBuild = [:]
		binding.getVariable('currentBuild').rawBuild.getCause = { return null }

		registerHttpRequestMock()
	}

	private void registerHttpRequestMock() {
		helper.registerAllowedMethod("httpRequest", [Map], { Map params ->
			def url = params.url as String
			if (url.contains('/actions/runs')) {
				def count = ghaRunsExist ? 1 : 0
				return [content: """{"total_count": ${count}, "workflow_runs": []}"""]
			}
			if (url.contains('/pulls/')) {
				return [content: """{"head": {"sha": "${prSha}"}, "updated_at": "${prUpdatedAt}"}"""]
			}
			throw new IOException("Unexpected URL: ${url}")
		})
	}

	@Override
	void registerAllowedMethods() {
		super.registerAllowedMethods()

		helper.registerAllowedMethod("input", [Map])
		helper.registerAllowedMethod("doStuff", [], { String args ->
			echo "Doing stuff"
		})
	}

	@Test
	void gha_alreadyApproved() throws Exception {
		setupPRBuild()
		ghaRunsExist = true

		def script = runScript(SCRIPT_NAME)
		assertJobStatusSuccess()
		assertCallStack().contains('Approved: GitHub Actions workflow runs found')
		assertCallStack().doesNotContain('Approval is required')
	}

	@Test
	void gha_approvedAfterFirstPoll() throws Exception {
		setupPRBuild()
		ghaRunsExist = false

		def ghaCheckCount = 0
		helper.registerAllowedMethod("httpRequest", [Map], { Map params ->
			def url = params.url as String
			if (url.contains('/actions/runs')) {
				ghaCheckCount++
				def count = ghaCheckCount >= 2 ? 1 : 0
				return [content: """{"total_count": ${count}, "workflow_runs": []}"""]
			}
			if (url.contains('/pulls/')) {
				return [content: """{"head": {"sha": "${prSha}"}, "updated_at": "${prUpdatedAt}"}"""]
			}
			throw new IOException("Unexpected URL: ${url}")
		})

		helper.registerAllowedMethod("timeout", [Map, Closure], { Map params, Closure body ->
			throw new FlowInterruptedException(
					Result.NOT_BUILT,
					false,
					new TimeoutStepExecution.ExceededTimeout()
			)
		})

		def script = runScript(SCRIPT_NAME)
		assertJobStatusSuccess()
		assertCallStack().contains('Approved: GitHub Actions workflow runs found')
	}

	@Test
	void gha_manualApproval() throws Exception {
		setupPRBuild()
		ghaRunsExist = false

		helper.registerAllowedMethod("timeout", [Map, Closure], { Map params, Closure body ->
			body()
		})

		def script = runScript(SCRIPT_NAME)
		assertJobStatusSuccess()
		assertCallStack().contains('Approved: manual approval on Jenkins')
	}

	@Test
	void gha_noChangeUrl_fallback() throws Exception {
		addEnvVar('CHANGE_AUTHOR', 'foo')
		addEnvVar('CHANGE_ID', '42')

		binding.getVariable('currentBuild').rawBuild = [:]
		binding.getVariable('currentBuild').rawBuild.getCause = { return null }

		helper.registerAllowedMethod("input", [Map])

		def script = runScript(SCRIPT_NAME)
		assertJobStatusSuccess()
		assertCallStack().contains('Approval is required')
		assertCallStack().doesNotContain('GitHub Actions')
	}

	@Test
	void gha_apiError_fallback() throws Exception {
		addEnvVar('CHANGE_AUTHOR', 'foo')
		addEnvVar('CHANGE_ID', '42')
		addEnvVar('CHANGE_URL', 'https://github.com/hibernate/hibernate-orm/pull/42')

		binding.getVariable('currentBuild').rawBuild = [:]
		binding.getVariable('currentBuild').rawBuild.getCause = { return null }

		helper.registerAllowedMethod("httpRequest", [Map], { Map params ->
			throw new IOException("API error")
		})

		helper.registerAllowedMethod("input", [Map])

		def script = runScript(SCRIPT_NAME)
		assertJobStatusSuccess()
		assertCallStack().contains('Falling back to standard manual approval')
		assertCallStack().contains('Approval is required')
	}

	@Test
	void gha_userAbort_propagates() throws Exception {
		setupPRBuild()
		ghaRunsExist = false

		helper.registerAllowedMethod("timeout", [Map, Closure], { Map params, Closure body ->
			throw new FlowInterruptedException(
					Result.ABORTED,
					false
			)
		})

		def thrown = assertThrows(Exception) {
			runScript(SCRIPT_NAME)
		}
		def cause = thrown
		while (cause != null && !(cause instanceof FlowInterruptedException)) {
			cause = cause.cause
		}
		assert cause instanceof FlowInterruptedException
	}

	@Test
	void gha_exponentialBackoff() throws Exception {
		setupPRBuild()
		ghaRunsExist = false

		def timeoutValues = []
		def callCount = 0
		helper.registerAllowedMethod("timeout", [Map, Closure], { Map params, Closure body ->
			timeoutValues << params.time
			callCount++
			if (callCount >= 4) {
				body()
				return
			}
			throw new FlowInterruptedException(
					Result.NOT_BUILT,
					false,
					new TimeoutStepExecution.ExceededTimeout()
			)
		})

		def script = runScript(SCRIPT_NAME)
		assertJobStatusSuccess()

		assert timeoutValues[0] == 5
		assert timeoutValues[1] == 10
		assert timeoutValues[2] == 20
		assert timeoutValues[3] == 40
	}

	@Test
	void gha_activityResetsBackoff() throws Exception {
		setupPRBuild()
		ghaRunsExist = false

		def prFetchCount = 0
		helper.registerAllowedMethod("httpRequest", [Map], { Map params ->
			def url = params.url as String
			if (url.contains('/actions/runs')) {
				return [content: '{"total_count": 0, "workflow_runs": []}']
			}
			if (url.contains('/pulls/')) {
				prFetchCount++
				// updatedAt changes on the 3rd PR fetch (2nd poll)
				def updatedAt = prFetchCount >= 3 ? '2024-01-02T00:00:00Z' : '2024-01-01T00:00:00Z'
				return [content: """{"head": {"sha": "abc123"}, "updated_at": "${updatedAt}"}"""]
			}
			throw new IOException("Unexpected URL: ${url}")
		})

		def timeoutValues = []
		def callCount = 0
		helper.registerAllowedMethod("timeout", [Map, Closure], { Map params, Closure body ->
			timeoutValues << params.time
			callCount++
			if (callCount >= 4) {
				body()
				return
			}
			throw new FlowInterruptedException(
					Result.NOT_BUILT,
					false,
					new TimeoutStepExecution.ExceededTimeout()
			)
		})

		def script = runScript(SCRIPT_NAME)
		assertJobStatusSuccess()

		assert timeoutValues[0] == 5
		assert timeoutValues[1] == 10
		// updatedAt changed on the 2nd poll → reset
		assert timeoutValues[2] == 5
		assert timeoutValues[3] == 10
	}

	@Test
	void gha_apiErrorDuringPoll_continues() throws Exception {
		setupPRBuild()

		def ghaCheckCount = 0
		helper.registerAllowedMethod("httpRequest", [Map], { Map params ->
			def url = params.url as String
			if (url.contains('/actions/runs')) {
				ghaCheckCount++
				if (ghaCheckCount == 1) return [content: '{"total_count": 0, "workflow_runs": []}']
				if (ghaCheckCount == 2) throw new IOException("transient error")
				return [content: '{"total_count": 1, "workflow_runs": []}']
			}
			if (url.contains('/pulls/')) {
				return [content: """{"head": {"sha": "abc123"}, "updated_at": "2024-01-01T00:00:00Z"}"""]
			}
			throw new IOException("Unexpected URL: ${url}")
		})

		def timeoutCallCount = 0
		helper.registerAllowedMethod("timeout", [Map, Closure], { Map params, Closure body ->
			timeoutCallCount++
			throw new FlowInterruptedException(
					Result.NOT_BUILT,
					false,
					new TimeoutStepExecution.ExceededTimeout()
			)
		})

		def script = runScript(SCRIPT_NAME)
		assertJobStatusSuccess()
		assertCallStack().contains('GitHub API check failed')
		assertCallStack().contains('Approved: GitHub Actions workflow runs found')
	}
}
