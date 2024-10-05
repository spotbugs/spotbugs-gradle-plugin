/*
 * Copyright 2023 SpotBugs team
 *
 * <p>Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.spotbugs.snom

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.internal.DefaultGradleRunner
import org.gradle.util.GradleVersion
import spock.lang.Specification
import spock.lang.TempDir

abstract class BaseFunctionalTest extends Specification {
    static String gradleVersion = System.getProperty('gradleVersion', GradleVersion.current().version)

    @TempDir
    File rootDir

    GradleRunner getGradleRunner() {
        return new TestGradleRunner()
                .withGradleVersion(gradleVersion)
                .withProjectDir(rootDir)
                .withArguments('--configuration-cache', '--info', '--stacktrace', '--warning-mode=fail')
                .withTestKitDir(testKitDir)
                .forwardOutput()
                .withPluginClasspath()
    }

    class TestGradleRunner extends DefaultGradleRunner {
        @Override
        DefaultGradleRunner withArguments(List<String> arguments) {
            return super.withArguments(this.arguments + arguments)
        }

        @Override
        DefaultGradleRunner withArguments(String... arguments) {
            return withArguments(Arrays.asList(arguments))
        }
    }

    private static File getTestKitDir() {
        def gradleUserHome = System.getenv("GRADLE_USER_HOME")
        if (!gradleUserHome) {
            gradleUserHome = new File(System.getProperty("user.home"), ".gradle").absolutePath
        }
        return new File(gradleUserHome, "testkit")
    }
}
