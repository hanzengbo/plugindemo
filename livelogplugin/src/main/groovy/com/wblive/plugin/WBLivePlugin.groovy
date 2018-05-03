package com.wblive.plugin

import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryPlugin
import com.wblive.plugin.method.NoUsedMethodTransform
import com.wblive.plugin.method.CheckMethodUsedExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

public class WBLivePlugin implements Plugin<Project> {
    void apply(Project project) {
        def hasApp = project.plugins.withType(AppPlugin)
        def hasLib = project.plugins.withType(LibraryPlugin)
        if (!hasApp && !hasLib) {
            throw new IllegalStateException("'android' or 'android-library' plugin required.")
        }

        project.extensions.create('wbLivePluginExtension', WBLivePluginExension)
        project.extensions.create('checkMethodUsedExtension', CheckMethodUsedExtension)

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
        }
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