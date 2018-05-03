package com.hzb.plugin

import org.gradle.api.Action
import org.gradle.api.Project

class PackageTestAction implements Action<Project> {

    @Override
    void execute(Project project) {
        project.android.applicationVariants.each { variant ->
            def packageTask = project.tasks.findByName("transformClassesWithDexFor${variant.name.capitalize()}")

            if (packageTask == null) {
                return
            }
            println("packageTask is " + packageTask.name)
            //packageTask.setEnabled(false)
            packageTask.doFirst {
                println("packageTask do first is " + packageTask.name)
                Collection<File> dexFolders = null
                try {
                    dexFolders = packageTask.dexFolders
                } catch (MissingPropertyException e) {
                    // api is not public
                }
                if (null != dexFolders) {
                    dexFolders.each {folder ->
                        println("dexfolder is " + folder.name)
                    }
                }
            }
        }
    }
}
