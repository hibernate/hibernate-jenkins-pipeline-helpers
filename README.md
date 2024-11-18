# Hibernate helpers for Jenkins pipelines

This is a shared library containing helpers for Jenkins pipeline.

See below for documentation of the helpers.

See near the bottom of this file for help writing Groovy in a Jenkinsfile environment,
be it a Jenkinsfile or a shared library like this one.

You will also need to [configure your Jenkins instance](#jenkins-configuration).

## Using the helper steps (declarative or programmatic pipelines)

This library provides a set of helper steps that can come in handy
in both declarative and programmatic pipelines.

### requireApprovalForPullRequest

Blocks pull request builds (and only pull request builds)
pending approval from a given user group.

The approval is skipped if:

* the pull request was submitted by a member of the given user group.
* the build was triggered explicitly by a member of the given user group. 

Usage:

```groovy
requireApprovalForPullRequest 'hibernate'
```

### notifyBuildResult

Usage:

```groovy
notifyBuildResult(
  /**
   * Space-separated emails of maintainers to notify of build results on primary branches.
   * Optional, defaults to empty.
   */
  maintainers: "foo@example.com bar@example.com",
  /**
   * Whether maintainers should always be notified.
   * Optional, defaults to false.
   */
  notifySuccessAfterSuccess: true
)
```

## Using the helper classes (programmatic pipelines)

This library provides a set of helper classes that can come in handy
in programmatic pipelines.

See the content of the `execute()` method in `test/SmokePipeline.groovy`
for an example of how the helpers are expected to be configured and used.

## Setup

### `Jenkinsfile`

In order to use this library, you need to explicitly import it into your Jenkinsfile, by adding this near the top (~before imports):

```groovy
@Library('hibernate-jenkins-pipeline-helpers') _
```

This relies on the library being [defined in the Jenkins instance](#jenkins-configuration-definition).

### <a id="jenkins-configuration" /> Jenkins configuration

### <a id="jenkins-configuration-definition" /> Library definition and version

In order for `@Library` to work, you will need to define it in your Jenkins instance.
See [here](https://www.jenkins.io/doc/book/pipeline/shared-libraries/#defining-shared-libraries) for details.

For https://ci.hibernate.org, we define the library [globally](https://www.jenkins.io/doc/book/pipeline/shared-libraries/#global-shared-libraries),
so that we can set the (default) version globally [here](https://ci.hibernate.org/manage/configure#global-untrusted-pipeline-libraries).

### Required plugins

 - https://plugins.jenkins.io/pipeline-maven
 - https://plugins.jenkins.io/email-ext
 - https://plugins.jenkins.io/config-file-provider
 - https://plugins.jenkins.io/pipeline-utility-steps for YAML reading
 - https://plugins.jenkins.io/notification for Gitter notifications

### Script approval

If not already done, you will need to allow the following calls in <jenkinsUrl>/scriptApproval/:

- method java.lang.Class isInstance java.lang.Object
- method java.util.Map putIfAbsent java.lang.Object java.lang.Object
- staticMethod org.jenkinsci.plugins.pipeline.modeldefinition.Utils markStageSkippedForConditional java.lang.String
- new java.lang.IllegalArgumentException java.lang.String
- new java.lang.IllegalStateException java.lang.String
- method hudson.plugins.git.GitSCM getUserRemoteConfigs
- method hudson.plugins.git.UserRemoteConfig getUrl
- method java.lang.Throwable addSuppressed java.lang.Throwable
- new java.util.LinkedHashMap

Just run the script a few times, it will fail and logs will display a link to allow these calls.

## Conventions

### "primary" branches
Branches named "main", "master" or matching the regex `/[0-9]+.[0-9]+/` will be considered as "primary" branches,
and, depending on the Jenkinsfile, may undergo additional testing.

### "tracking" branches
Branches named "tracking-<some-name>" will be considered as "tracking branches",
i.e. branches containing a patch to be applied regularly
on top of a base branch, whenever that base branch changes, or another job succeeds.

In a multibranch pipeline, the specific job for each tracking branch will be configured to run:
- when the branch is updated (as usual)
- when a snapshot dependency is updated in the same Jenkins instance (as usual)
- when the base branch is built successfully
- when the Jenkins jobs mentioned in the branch name are built successfully

Also, when such branches are built, they are automatically merged with the base branch.

### Job configuration file

The job configuration file is optional. Its purpose is to host job-specific configuration, such as notification recipients.

The file is named 'job-configuration.yaml', and it should be set up using the config file provider plugin
(https://plugins.jenkins.io/config-file-provider).

```
notification:
  email:
    # String containing a space-separated list of email addresses to notify in case of failing non-PR builds.
    recipients: ...
  gitter:
    # List of "secret text" credentials for Gitter chat rooms that should be notified
    # Note that only global credentials will work; the notification plugin apparently doesn't handle job-scoped credentials
    urlCredentialsId:
      - <credentialsId>
      - <otherCredentialsId>
scm:
  # User info for commits created during the job. Useful for releases in particular.
  user:
    name: ... # Defaults to 'Hibernate-CI'
    email: ... # Defaults to 'ci@hibernate.org' 
  # Remotes to be added to git when checking out. Useful for tracking (see below) in particular.
  remotes:
    <remote-name>:
      # The URL of a remote
      url: ...
tracking:
  # The tracking ID, used as a suffix for tracking branch names:
  # if the branch is named "tracking-foo", the tracking ID will be "foo".
  <tracking-name>:
    # The Git refspec to the base of this tracking branch.
    # For example this can be "origin/main" or "upstream/main" (if a remote named "upstream" is defined).
    base: ...
    # The Jenkins jobs to track.
    # Use "branchname" to reference jobs corresponding to other branches in the same multibranch job.
    # Use a "/" prefix ("/somename") to reference jobs outside of the multibranch job.
    tracked:
      - <job name>
      - <other job name>
```

## Contributing & writing Jenkinsfiles

See `CONTRIBUTING.md`.
