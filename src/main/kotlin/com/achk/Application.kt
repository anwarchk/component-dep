package com.achk

import com.achk.MainApp.CommandType.*
import java.util.*
import java.util.stream.Collectors

object MainApp {
    private val installedComponents = LinkedHashMap<String, SoftwareComponent>()
    private val componentDependencies = LinkedHashMap<SoftwareComponent, Collection<SoftwareComponent>>()

    private fun doIt(input: Array<String?>) {
        val commandParser = SoftwareCommandParser()
        val commands = commandParser.parseCommand(input)
        commands.forEach { c -> c.executeCommand(installedComponents, componentDependencies) }
    }

    @JvmStatic
    fun main(args: Array<String>) {
        val `in` = Scanner(System.`in`)

        val size = Integer.parseInt(`in`.nextLine().trim { it <= ' ' })
        val inputs = arrayOfNulls<String>(size)
        var inputItem = ""
        (0 until size).forEach { i ->
            try {
                inputItem = `in`.nextLine()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            inputs[i] = inputItem
        }
        doIt(inputs)
    }


    internal class SoftwareComponent(internal val name: String) {
        internal val dependencies = ArrayList<SoftwareComponent>()
        private val parents = LinkedList<SoftwareComponent>()

        fun addDependency(component: SoftwareComponent) {
            dependencies.add(component)
        }

        override fun equals(other: Any?): Boolean {
            if (this === other)
                return true
            else if (other == null)
                return false
            val obj = other as SoftwareComponent?
            return name == obj!!.name
        }

        override fun hashCode(): Int {
            return Objects.hashCode(name)
        }

        override fun toString(): String {
            return name
        }

        fun dependsOn(component: SoftwareComponent): Boolean {
            return dependencies.contains(component)
        }

        fun addParent(component: SoftwareComponent) {
            parents.add(component)
        }
    }

    internal class SoftwareCommand private constructor(private val commandType: CommandType) {
        private val components = ArrayList<SoftwareComponent>()

        fun executeCommand(
            installedComponents: MutableMap<String, SoftwareComponent>,
            componentDependencies: MutableMap<SoftwareComponent, Collection<SoftwareComponent>>
        ) {

            println(this.toString())

            when (commandType) {

                LIST -> installedComponents.forEach { (s, component) -> println(component) }

                DEPEND -> {
                    val primary = components[0]
                    for (i in 1 until components.size) {
                        val dependentCandidate = components[i]
                        if (dependentCandidate.dependsOn(primary)) {
                            println(dependentCandidate.name + " depends on " + primary.name + ", ignoring command")
                        } else {
                            primary.addDependency(dependentCandidate)
                            dependentCandidate.addParent(primary)
                        }
                    }
                    componentDependencies[primary] = primary.dependencies
                }

                INSTALL -> {
                    val component = components[0]
                    if (installedComponents.containsKey(component.name)) {
                        println(component.name + " is already installed")
                    } else {
                        doInstallComponent(component, installedComponents, componentDependencies)
                    }
                }

                REMOVE -> {
                    val componentToRemove = components[0]
                    if (!installedComponents.containsKey(componentToRemove.name)) {
                        println(componentToRemove.name + " is not installed")
                    } else {
                        doRemoveComponent(componentToRemove, installedComponents, componentDependencies)
                    }
                }
            }
        }

        private fun doInstallComponent(
            component: SoftwareComponent,
            installedComponents: MutableMap<String, SoftwareComponent>,
            componentDependencies: Map<SoftwareComponent, Collection<SoftwareComponent>>
        ) {
            val dependencies = componentDependencies[component]
            if (dependencies != null) {
                for (sc in dependencies) {
                    doInstallComponent(sc, installedComponents, componentDependencies)
                }
            }
            if (!installedComponents.containsKey(component.name)) {
                println("Installing " + component.name)
                installedComponents[component.name] = component
            }
        }

        private fun doRemoveComponent(
            component: SoftwareComponent,
            installedComponents: MutableMap<String, SoftwareComponent>,
            componentDependencies: Map<SoftwareComponent, Collection<SoftwareComponent>>
        ) {
            val dependencies = componentDependencies[component]
            if (dependencies != null) {
                for (sc in dependencies) {
                    doRemoveComponent(sc, installedComponents, componentDependencies)
                }
            }
            val collect = installedComponents
                .values
                .stream()
                .filter { sc -> sc.dependsOn(component) }
                .collect(Collectors.toList())

            if (installedComponents.containsKey(component.name)) {
                println("removing " + component.name)
                installedComponents.remove(component.name)
            }
        }

        override fun toString(): String {
            val s = StringBuilder(commandType.name)
            for (component in components) {
                s.append(" ").append(component.name)
            }
            return s.toString()
        }

        fun addSoftwareComponent(component: SoftwareComponent) {
            components.add(component)
        }

        companion object {

            fun valueOf(command: String?): SoftwareCommand? {
                return if (command.isNullOrEmpty()) null
                else SoftwareCommand(CommandType.valueOf(command.toUpperCase()))
            }
        }
    }

    internal class SoftwareCommandParser {

        fun parseCommand(arguments: Array<String?>): Collection<SoftwareCommand> {
            if (arguments.isEmpty()) {
                throw RuntimeException("Invalid command")
            }
            val commands = ArrayList<SoftwareCommand>()
            var command: SoftwareCommand
            for (s in arguments) {
                val commandAndArgs = s?.split("\\s+".toRegex())?.dropLastWhile { it.isEmpty() }?.toTypedArray()
                if (commandAndArgs != null && commandAndArgs.isNotEmpty()) {
                    command = SoftwareCommand.valueOf(commandAndArgs[0])!!
                    for (i in 1 until commandAndArgs.size) {
                        val component = SoftwareComponent(commandAndArgs[i])
                        command.addSoftwareComponent(component)
                    }
                } else {
                    command = SoftwareCommand.valueOf(s)!!
                }
                commands.add(command)
            }
            return commands
        }
    }

    internal enum class CommandType {
        DEPEND,
        INSTALL,
        REMOVE,
        LIST
    }
}