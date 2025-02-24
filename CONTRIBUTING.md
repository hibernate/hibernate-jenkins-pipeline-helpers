# Contributing

## Legal

You should check the relevant license under which all contributions will be licensed for the specific project you are contributing to.

All contributions are subject to the [Developer Certificate of Origin (DCO)](https://developercertificate.org/).

The DCO text is available verbatim in the [dco.txt](dco.txt) file.

## Guidelines

See https://hibernate.org/community/contribute/.

## Releasing

To release changes: `mvn release:prepare && mvn release:perform`.

This will run tests and create a tag, which is enough to publish the library.

## Making changes to this library

Documentation on shared libraries in Jenkins pipelines can be found [there](https://jenkins.io/doc/book/pipeline/shared-libraries/).

This project include smoke tests; run `mvn clean test` to execute them.
The tests won't do much, but will display the resulting call tree,
which can be helpful when debugging.
Be aware that the tests don't replicate a full Jenkins environment (no sandboxing in particular),
thus testing on Jenkins itself is still necessary.

## Writing Groovy in Jenkins

[This page on our Jenkins instance](http://ci.hibernate.org/pipeline-syntax/) helps when you want to add a call to a pipeline step:
it will provide you with a GUI to generate a call.

More generally, see [this part of the Jenkins documentation](https://jenkins.io/doc/book/pipeline/development/#pipeline-development-tools)
for resources that will help you write Jenkinsfiles.

## Troubleshooting

```
org.jenkinsci.plugins.scriptsecurity.sandbox.RejectedAccessException: Scripts not permitted to use method groovy.lang.GroovyObject getProperty java.lang.String (path.to.some.Class.someProperty)
```

Check that the property exists, and that there's no typo.
The interpreted apparently defaults to using reflection when unknown methods/properties are referenced.


```
hudson.remoting.ProxyException: com.cloudbees.groovy.cps.impl.CpsCallableInvocation
```

Check that you're not executing a method inside a constructor (see "Known limitations").
This includes calling methods from field initializers (`@Field String myField = someObject.someMethod()`).

Move the method execution to the body of the script if necessary.

## Known limitations & Jenkinsfile quirks

- Constructors should never, ever call a method defined in your Jenkinsfile script.
  This apparently has to do with the transformations the interpreter applies to methods
  so that they can be executed as "continuations", which requires transformations on both
  the declaration site and call site in order to work.
  Constructors are not transformed, so they can't call such transformed methods.
- `.with` cannot be used, because it requires to use reflection,
  which is disallowed in Jenkinsfile, probably because it would allow to bypass the sandbox.
- Nested classes support is quite bad. In particular:
  - [you cannot reference an inner class (`ParentClass.NestedClass`) from another class](https://issues.jenkins-ci.org/browse/JENKINS-41896).
  - you cannot used "qualified this", e.g. `ParentClass.this`.
- Behavioral annotations such as `@Delegate`, `@Immutable`, etc. are unlikely to work.
- Static type checking (`@TypeChecked`) is not recommended since it will cause cryptic errors.
- Closure support is limited. In particular:
  - `closure.rehydrate(...)` will not work.
    The resulting copy of the closure will execute, but any of its calls to
    will be silently ignored.
    You should just set the delegate of the original closure (`closure.delegate = ...`).
  - Only the default "resolveStrategy" can be expected to work correctly,
    because other strategies rely on reflection (`GroovyObject.invoke(String, Map)`
    or `GroovyObject.getProperty(String)`), which are not safe to execute in the
    Jenkins sandbox.
- `java.util.Set` should be avoided, because at least some methods don't work correctly in Jenkins.
  In particular, `removeAll` has no effect whatsoever. `java.util.List` works fine.
