import groovy.json.JsonSlurper
import org.codehaus.groovy.runtime.InvokerHelper
import java.time.Duration

@Suppress("UNCHECKED_CAST")
fun configureRuns(extension: Any, configure: (Any) -> Unit) {
    val runs = InvokerHelper.getProperty(extension, "runs") as DomainObjectCollection<Any>
    runs.configureEach(configure)
}

if (providers.gradleProperty("clientTestSource").isPresent) {
    gradle.projectsEvaluated {
        rootProject.allprojects {
            val runDirectory = layout.buildDirectory.dir("client-agent/run").get().asFile

            extensions.findByName("loom")?.let { extension ->
                configureRuns(extension) { run ->
                    InvokerHelper.invokeMethod(run, "runDir", "build/client-agent/run")
                }
            }
            for (name in listOf("neoForge", "legacyForge")) {
                extensions.findByName(name)?.let { extension ->
                    configureRuns(extension) { run ->
                        val gameDirectory = InvokerHelper.getProperty(run, "gameDirectory")
                        InvokerHelper.invokeMethod(gameDirectory, "set", runDirectory)
                    }
                }
            }
            if (plugins.hasPlugin("net.minecraftforge.gradle")) {
                configureRuns(extensions.getByName("minecraft")) { run ->
                    val workingDirectory = InvokerHelper.getProperty(run, "workingDir")
                    InvokerHelper.invokeMethod(workingDirectory, "set", runDirectory)
                }
            }

            tasks.matching { it.name == "runClient" }.configureEach {
                val clientTask = this as? JavaExec
                    ?: throw GradleException("Unsupported client run task: $path (${javaClass.name})")
                val agent = rootProject.file("tests/agent/build/libs/client-test-agent.jar")
                val report = layout.buildDirectory.file("reports/client-agent/results.json").get().asFile

                clientTask.workingDir(runDirectory)
                clientTask.jvmArgs("-javaagent:${agent.absolutePath}")
                clientTask.systemProperty("itemnamecopy.test.target", name)
                clientTask.systemProperty("itemnamecopy.test.report", report.absolutePath)
                clientTask.systemProperty("itemnamecopy.test.source", findProperty("clientTestSource") ?: "unknown")
                clientTask.timeout.set(Duration.ofMinutes(5))
                clientTask.doFirst {
                    runDirectory.mkdirs()
                    runDirectory.resolve("options.txt")
                        .writeText("lang:en_us\nguiScale:1\nonboardAccessibility:false\n")
                    for (file in listOf(report, report.parentFile.resolve("TEST-client.xml"))) {
                        if (file.exists() && !file.delete()) {
                            throw GradleException("Cannot remove stale report: $file")
                        }
                    }
                }
                clientTask.doLast {
                    if (!report.isFile) throw GradleException("Client did not produce a report: $report")
                    val result = JsonSlurper().parse(report) as Map<*, *>
                    val failed = (result["failed"] as Number).toInt()
                    val passed = (result["passed"] as Number).toInt()
                    val expectedTests = (result["expectedTests"] as Number).toInt()
                    if (failed != 0 || passed == 0 || passed != expectedTests) {
                        throw GradleException("Client verification failed: $report")
                    }
                }
            }
        }
    }
}
