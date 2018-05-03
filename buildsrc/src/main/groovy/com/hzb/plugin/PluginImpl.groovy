package com.hzb.plugin

import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryPlugin
import com.hzb.plugin.method.CheckMethodUsedExtension
import com.hzb.plugin.method.NoUsedMethodTransform
import org.gradle.api.Plugin
import org.gradle.api.Project

public class PluginImpl implements Plugin<Project> {
    void apply(Project project) {
        project.beforeEvaluate {
            println("before Evalute")
        }
        def hasApp = project.plugins.withType(AppPlugin)
        def hasLib = project.plugins.withType(LibraryPlugin)
        if (!hasApp && !hasLib) {
            throw new IllegalStateException("'android' or 'android-library' plugin required.")
        }
        println "hasApp = " + hasApp + " hasLib = " + hasLib
        project.extensions.create('wbLivePluginExtension', WBLivePluginExension)
        project.extensions.create('checkMethodUsedExtension', CheckMethodUsedExtension)
        String taskNames = project.gradle.startParameter.taskNames.toString()
        System.out.println("taskNames is " + taskNames);
        AssembleTask assembleTask = getTaskInfo(project.gradle.startParameter.taskNames);
        //仅debug下进行代码注入
        if (assembleTask.isDebug) {
            def android = project.extensions.findByType(AppExtension)
            boolean checkNotUsedMethod = Boolean.parseBoolean(project.properties.get("checkMethodNotUsed"))
            println("checkNotUsedMethod = " + checkNotUsedMethod)
            if (checkNotUsedMethod) {
                android.registerTransform(new NoUsedMethodTransform(project, project.checkMethodUsedExtension))
            }
            android.registerTransform(new Transform(project, project.wbLivePluginExtension))
           // android.registerTransform(new TransformTwo(project, project.wbLivePluginExtension))
        }

        project.afterEvaluate {
            println("after Evalute")
        }
        println("end project")
        project.afterEvaluate(new PackageTestAction())
    }



    private AssembleTask getTaskInfo(List<String> taskNames) {
        AssembleTask assembleTask = new AssembleTask();
        for (String task : taskNames) {
            if (task.toUpperCase().contains("ASSEMBLE")
                    || task.contains("aR")
                    || task.toUpperCase().contains("RESGUARD")) {
                if (task.toUpperCase().contains("DEBUG")) {
                    assembleTask.isDebug = true;
                }
                assembleTask.isAssemble = true;
                break;
            }
        }
        return assembleTask
    }

    private class AssembleTask {
        boolean isAssemble = false;
        boolean isDebug = false;
    }
}