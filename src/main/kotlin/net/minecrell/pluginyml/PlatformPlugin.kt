/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017 Minecrell <https://github.com/Minecrell>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package net.minecrell.pluginyml

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.plugins.JavaPlugin
import org.gradle.kotlin.dsl.register
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType

abstract class PlatformPlugin<T : PluginDescription>(private val platformName: String, private val fileName: String) : Plugin<Project> {

    protected abstract fun createExtension(project: Project): T

    final override fun apply(project: Project) {
        project.run {
            val description = createExtension(this)

            // Add extension
            val prefix = platformName.replaceFirstChar(Char::lowercase)
            extensions.add(prefix, description)

            val generatedResourcesDirectory = layout.buildDirectory.dir("generated/plugin-yml/$platformName")

            // Add library configuration
            val library = project.configurations.maybeCreate("library")
            val libraries = project.configurations.create("${prefix}Library").extendsFrom(library)

            // Create task
            val generateTask = tasks.register<GeneratePluginDescription>("generate${platformName}PluginDescription") {
                group = "plugin-yml"

                fileName.set(this@PlatformPlugin.fileName)
                librariesJsonFileName.set("$prefix-libraries.json")
                librariesRootComponent.set(libraries.incoming.resolutionResult.root)
                outputDirectory.set(generatedResourcesDirectory)
                pluginDescription.set(provider {
                    setDefaults(project, description)
                    description
                })

                doFirst {
                    setLibraries(librariesRootComponent.orNull, description)
                    validate(description)
                }
            }

            plugins.withType<JavaPlugin> {
                extensions.getByType<SourceSetContainer>().named(SourceSet.MAIN_SOURCE_SET_NAME) {
                    resources.srcDir(generateTask)
                    configurations.getByName(compileOnlyConfigurationName).extendsFrom(libraries)
                }
            }
        }
    }

    protected abstract fun setDefaults(project: Project, description: T)
    protected open fun setLibraries(libraries: ResolvedComponentResult?, description: T) {}
    protected abstract fun validate(description: T)

}
