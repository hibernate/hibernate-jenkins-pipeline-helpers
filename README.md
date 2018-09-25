# Hibernate helpers for Jenkins pipelines

This is a shared library containing helpers for Jenkins pipeline.

See below for documentation of the helpers.

See near the bottom of this file for help writing Groovy in a Jenkinsfile environment,
be it a Jenkinsfile or a shared library like this one. 

## Using the helpers

See the content of the `execute()` method in `test/SmokePipeline.groovy`
for an example of how the helpers are expected to be configured and used.

## Required configuration

### Jenkins configuration

#### Required plugins

 - https://plugins.jenkins.io/pipeline-maven
 - https://plugins.jenkins.io/email-ext
 - https://plugins.jenkins.io/config-file-provider
 - https://plugins.jenkins.io/pipeline-utility-steps for YAML reading

#### Script approval

If not already done, you will need to allow the following calls in <jenkinsUrl>/scriptApproval/:

- method java.util.Map putIfAbsent java.lang.Object java.lang.Object
- staticMethod org.jenkinsci.plugins.pipeline.modeldefinition.Utils markStageSkippedForConditional java.lang.String
- new java.lang.IllegalArgumentException java.lang.String
- new java.lang.IllegalStateException java.lang.String
- method hudson.plugins.git.GitSCM getUserRemoteConfigs
- method hudson.plugins.git.UserRemoteConfig getUrl
- method java.lang.Throwable addSuppressed java.lang.Throwable
- new java.util.LinkedHashMap
- method groovy.lang.Closure getOwner
- method groovy.lang.Closure getThisObject
- method groovy.lang.Closure rehydrate java.lang.Object java.lang.Object java.lang.Object

Just run the script a few times, it will fail and logs will display a link to allow these calls.

### Job configuration

#### Branch name

##### "primary" branches
Branches named "master" or matching the regex /[0-9]+.[0-9]+/ will be considered as "primary" branches,
and, depending on the Jenkinsfile, may undergo additional testing.

##### "tracking" branches
Branches named "tracking-<some-name>" will be considered as "tracking branches",
i.e. branches containing a patch to be applied regularly
on top of a base branch, whenever that base branch changes, or another job succeeds.

In a multibranch pipeline, the specific job for each tracking branch will be configured to run:
- when the branch is updated (as usual)
- when a snapshot dependency is updated in the same Jenkins instance (as usual)
- when the base branch is built successfully
- when the Jenkins jobs mentioned in the branch name are built successfully

Also, when such branches are built, they are automatically merged with the base branch.

#### Job configuration file

The job configuration file is optional. Its purpose is to host job-specific configuration, such as notification recipients.

The file is named 'job-configuration.yaml', and it should be set up using the config file provider plugin
(https://plugins.jenkins.io/config-file-provider).

```
notification:
  email:
    # String containing a space-separated list of email addresses to notify in case of failing non-PR builds.
    recipients: ...
 # Remotes to be added to git when checking out. Useful for tracking (see below) in particular.
scm:
  remotes:
    <remote-name>:
      # The URL of a remote
      url: ...
tracking:
  # The tracking ID, used as a suffix for tracking branch names:
  # if the branch is named "tracking-foo", the tracking ID will be "foo".
  <tracking-name>:
    # The Git refspec to the base of this tracking branch.
    # For example this can be "origin/master" or "upstream/master" (if a remote named "upstream" is defined).
    base: ...
    # The Jenkins jobs to track.
    # Use "branchname" to reference jobs corresponding to other branches in the same multibranch job.
    # Use a "/" prefix ("/somename") to reference jobs outside of the multibranch job.
    tracked:
      - <job name>
      - <other job name>
```

## Writing Groovy in Jenkins

[This page on our Jenkins instance](http://ci.hibernate.org/pipeline-syntax/) helps when you want to add a call to a pipeline step:
it will provide you with a GUI to generate a call.
 
More generally, see [this part of the Jenkins documentation](https://jenkins.io/doc/book/pipeline/development/#pipeline-development-tools)
for resources that will help you write Jenkinsfiles.

### Making changes to this library

Documentation on shared libraries in Jenkins pipelines can be found [there](https://jenkins.io/doc/book/pipeline/shared-libraries/).

This project include smoke tests; run `mvn clean test` to execute them.
The tests won't do much, but will display the resulting call tree,
which can be helpful when debugging.
Be aware that the tests don't replicate a full Jenkins environment (no sandboxing in particular),
thus testing on Jenkins itself is still necessary.

### Known limitations

- Nested classes support is quite bad.
In particular, [you cannot reference an inner class (ParentClass.NestedClass) from another class](https://issues.jenkins-ci.org/browse/JENKINS-41896).
- Behavioral annotations such as `@Delegate`, `@Immutable`, etc. are unlikely to work.

### Troubleshooting

```
org.jenkinsci.plugins.scriptsecurity.sandbox.RejectedAccessException: Scripts not permitted to use method groovy.lang.GroovyObject getProperty java.lang.String (path.to.some.Class.someProperty)
```

Check that the property exists, and that there's no typo.
The interpreted apparently defaults to using reflection when unknown methods/properties are referenced.