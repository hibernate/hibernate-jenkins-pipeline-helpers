/*
 * Hibernate Helpers for Jenkins pipelines
 *
 * License: Apache License, version 2 or later.
 * See the LICENSE.txt file in the root directory or <https://www.apache.org/licenses/LICENSE-2.0>.
 */


import com.lesfurets.jenkins.unit.declarative.DeclarativePipelineTest
import hudson.model.Cause
import hudson.model.Item
import hudson.model.Result
import hudson.model.User
import jenkins.scm.api.SCMSource
import org.jenkinsci.plugins.github_branch_source.Connector
import org.jenkinsci.plugins.github_branch_source.GitHubSCMSource
import org.jenkinsci.plugins.workflow.steps.FlowInterruptedException
import org.jenkinsci.plugins.workflow.steps.TimeoutStepExecution
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.kohsuke.github.GHCommitPointer
import org.kohsuke.github.GHEvent
import org.kohsuke.github.GHPullRequest
import org.kohsuke.github.GHRepository
import org.kohsuke.github.GHWorkflowRunQueryBuilder
import org.kohsuke.github.GitHub
import org.kohsuke.github.PagedIterable
import org.kohsuke.github.PagedIterator
import org.mockito.MockedStatic

import static com.lesfurets.jenkins.unit.global.lib.LibraryConfiguration.library
import static com.lesfurets.jenkins.unit.global.lib.LocalSource.localSource
import static org.junit.jupiter.api.Assertions.assertThrows
import static org.mockito.ArgumentMatchers.any
import static org.mockito.ArgumentMatchers.anyInt
import static org.mockito.ArgumentMatchers.anyString
import static org.mockito.Mockito.*

class RequireApprovalForPullRequestGHADeclarativeTest extends DeclarativePipelineTest {
	private static final SCRIPT_NAME = "RequireApprovalForPullRequestDeclarativePipeline.groovy"
	static MockedStatic<User> userClassMock
	static MockedStatic<SCMSource.SourceByItem> sourceByItemMock
	static MockedStatic<Connector> connectorMock

	GitHub mockGitHub
	GHRepository mockRepo
	GHPullRequest mockPR
	GHCommitPointer mockHead
	GHWorkflowRunQueryBuilder mockQueryBuilder
	PagedIterable mockPagedIterable
	PagedIterator mockIterator
	GitHubSCMSource mockSCMSource
	Item mockJob

	@BeforeAll
	static void createStaticMocks() {
		userClassMock = mockStatic(User)
		sourceByItemMock = mockStatic(SCMSource.SourceByItem)
		connectorMock = mockStatic(Connector)

		def fooMock = mock(User)
		userClassMock.when { User.getById('foo', false) }
				.thenReturn(fooMock)
		when(fooMock.getAuthorities()).thenReturn(['not-quite.hibernate', 'hibernate.but-not-quite'])
	}

	@AfterAll
	static void closeStaticMocks() {
		userClassMock.close()
		sourceByItemMock.close()
		connectorMock.close()
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

		mockGitHub = mock(GitHub)
		mockRepo = mock(GHRepository)
		mockPR = mock(GHPullRequest)
		mockHead = mock(GHCommitPointer)
		mockQueryBuilder = mock(GHWorkflowRunQueryBuilder)
		mockPagedIterable = mock(PagedIterable)
		mockIterator = mock(PagedIterator)
		mockSCMSource = mock(GitHubSCMSource)
		mockJob = mock(Item)

		when(mockSCMSource.getCredentialsId()).thenReturn('github-creds')
		when(mockSCMSource.getApiUri()).thenReturn('https://api.github.com')
		when(mockSCMSource.getRepoOwner()).thenReturn('hibernate')
		when(mockSCMSource.getRepository()).thenReturn('hibernate-orm')

		connectorMock.when { Connector.lookupScanCredentials(any(), anyString(), anyString(), anyString()) }
				.thenReturn(null)
		connectorMock.when { Connector.connect(anyString(), any()) }
				.thenReturn(mockGitHub)
		connectorMock.when { Connector.release(any()) }
				.then({ })

		when(mockGitHub.getRepository(anyString())).thenReturn(mockRepo)
		when(mockRepo.getPullRequest(anyInt())).thenReturn(mockPR)
		when(mockPR.getHead()).thenReturn(mockHead)
		when(mockHead.getSha()).thenReturn('abc123')
		when(mockPR.getUpdatedAt()).thenReturn(new Date(1000))

		when(mockRepo.queryWorkflowRuns()).thenReturn(mockQueryBuilder)
		when(mockQueryBuilder.headSha(anyString())).thenReturn(mockQueryBuilder)
		when(mockQueryBuilder.event(GHEvent.PULL_REQUEST)).thenReturn(mockQueryBuilder)
		when(mockQueryBuilder.list()).thenReturn(mockPagedIterable)
		when(mockPagedIterable.withPageSize(anyInt())).thenReturn(mockPagedIterable)
		when(mockPagedIterable.iterator()).thenReturn(mockIterator)
	}

	private void setupPRBuild() {
		addEnvVar('CHANGE_AUTHOR', 'foo')
		addEnvVar('CHANGE_ID', '42')

		binding.getVariable('currentBuild').rawBuild = [:]
		binding.getVariable('currentBuild').rawBuild.parent = mockJob
		binding.getVariable('currentBuild').rawBuild.getCause = { return null }

		sourceByItemMock.when { SCMSource.SourceByItem.findSource(mockJob) }
				.thenReturn(mockSCMSource)
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
		when(mockIterator.hasNext()).thenReturn(true)

		def script = runScript(SCRIPT_NAME)
		assertJobStatusSuccess()
		assertCallStack().contains('Approved: GitHub Actions workflow runs found')
		assertCallStack().doesNotContain('Approval is required')
	}

	@Test
	void gha_approvedAfterFirstPoll() throws Exception {
		setupPRBuild()
		when(mockIterator.hasNext()).thenReturn(false).thenReturn(true)

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
		when(mockIterator.hasNext()).thenReturn(false)

		helper.registerAllowedMethod("timeout", [Map, Closure], { Map params, Closure body ->
			body()
		})

		def script = runScript(SCRIPT_NAME)
		assertJobStatusSuccess()
		assertCallStack().contains('Approved: manual approval on Jenkins')
	}

	@Test
	void gha_notGitHubSCMSource_fallback() throws Exception {
		addEnvVar('CHANGE_AUTHOR', 'foo')
		addEnvVar('CHANGE_ID', '42')

		binding.getVariable('currentBuild').rawBuild = [:]
		binding.getVariable('currentBuild').rawBuild.parent = mockJob
		binding.getVariable('currentBuild').rawBuild.getCause = { return null }

		sourceByItemMock.when { SCMSource.SourceByItem.findSource(mockJob) }
				.thenReturn(null)

		helper.registerAllowedMethod("input", [Map])

		def script = runScript(SCRIPT_NAME)
		assertJobStatusSuccess()
		assertCallStack().contains('Approval is required')
		assertCallStack().doesNotContain('GitHub Actions')
	}

	@Test
	void gha_apiError_fallback() throws Exception {
		setupPRBuild()
		when(mockRepo.getPullRequest(anyInt())).thenThrow(new IOException("API error"))

		helper.registerAllowedMethod("input", [Map])

		def script = runScript(SCRIPT_NAME)
		assertJobStatusSuccess()
		assertCallStack().contains('Falling back to standard manual approval')
		assertCallStack().contains('Approval is required')
	}

	@Test
	void gha_userAbort_propagates() throws Exception {
		setupPRBuild()
		when(mockIterator.hasNext()).thenReturn(false)

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
		when(mockIterator.hasNext()).thenReturn(false)
		when(mockPR.getUpdatedAt()).thenReturn(new Date(1000))

		def timeoutValues = []
		def callCount = 0
		helper.registerAllowedMethod("timeout", [Map, Closure], { Map params, Closure body ->
			timeoutValues << params.time
			callCount++
			if (callCount >= 4) {
				// On the 4th timeout cycle, let manual approval through
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
		when(mockIterator.hasNext()).thenReturn(false)
		when(mockPR.getUpdatedAt())
				.thenReturn(new Date(1000))
				.thenReturn(new Date(1000))
				.thenReturn(new Date(2000))
				.thenReturn(new Date(2000))

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
		// updatedAt changed on the 2nd GH check → reset
		assert timeoutValues[2] == 5
		assert timeoutValues[3] == 10
	}

	@Test
	void gha_apiErrorDuringPoll_continues() throws Exception {
		setupPRBuild()
		def checkCallCount = 0
		when(mockIterator.hasNext()).thenAnswer({
			checkCallCount++
			if (checkCallCount == 1) return false
			if (checkCallCount == 2) throw new IOException("transient error")
			return true
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
