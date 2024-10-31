/*
 *     SPDX-License-Identifier: MIT
 *
 *     Copyright (c) 2017 Minecrell <https://github.com/Minecrell>
 *     Copyright (c) 2024 EldoriaRPG and Contributor <https://github.com/eldoriarpg>
 */

package net.minecrell.pluginyml.bukkit

import net.minecrell.pluginyml.InvalidPluginDescriptionException
import net.minecrell.pluginyml.PlatformPlugin
import net.minecrell.pluginyml.collectLibraries
import org.gradle.api.Project
import org.gradle.api.artifacts.result.ResolvedComponentResult

class BukkitPlugin : PlatformPlugin<BukkitPluginDescription>("Bukkit", "plugin.yml") {

    companion object {
        @JvmStatic private val VALID_NAME = Regex("^[A-Za-z0-9 _.-]+$")
    }

    override fun createExtension(project: Project) = BukkitPluginDescription(project)

    override fun setDefaults(project: Project, description: BukkitPluginDescription) {
        description.name = description.name ?: project.name
        description.version = description.version ?: project.version.toString()
        description.description = description.description ?: project.description
        description.website = description.website ?: project.findProperty("url")?.toString()
        description.author = description.author ?: project.findProperty("author")?.toString()
    }

    override fun setLibraries(libraries: ResolvedComponentResult?, description: BukkitPluginDescription) {
        description.libraries = libraries.collectLibraries(description.libraries)
    }

    override fun validate(description: BukkitPluginDescription) {
        val name = description.name ?: throw InvalidPluginDescriptionException("Plugin name is not set")
        if (!VALID_NAME.matches(name)) throw InvalidPluginDescriptionException("Invalid plugin name: should match $VALID_NAME")

        if (description.version.isNullOrEmpty()) throw InvalidPluginDescriptionException("Plugin version is not set")

        val main = description.main ?: throw InvalidPluginDescriptionException("Main class is not defined")
        if (main.isEmpty()) throw InvalidPluginDescriptionException("Main class cannot be empty")
        if (main.startsWith("org.bukkit.")) throw InvalidPluginDescriptionException("Main may not be within the org.bukkit namespace")

        for (command in description.commands) {
            if (command.name.contains(':')) throw InvalidPluginDescriptionException("Command '${command.name}' cannot contain ':'")
            command.aliases?.forEach { alias ->
                if (alias.contains(':')) throw InvalidPluginDescriptionException("Alias '$alias' of '${command.name}' cannot contain ':'")
            }
        }

        if (description.provides?.all(VALID_NAME::matches) == false) {
            throw InvalidPluginDescriptionException("Invalid plugin provides name: all should match $VALID_NAME")
        }
    }

}
