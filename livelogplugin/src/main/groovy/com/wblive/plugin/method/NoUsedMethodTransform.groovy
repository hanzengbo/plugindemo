package com.wblive.plugin.method

import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import org.gradle.api.Project

public class NoUsedMethodTransform extends com.android.build.api.transform.Transform {

    Project project
    MethodUsedCheck mMethodUsedCheck;
    private int mNotUsedClassCount;
    private int mNotUsedMethodCount;
    private CheckMethodUsedExtension mCheckMethodUsedExension;

    @Override
    String getName() {
        return "noUsedMethod"
    }

    public NoUsedMethodTransform(Project project, CheckMethodUsedExtension checkMethodUsedExension) {
        this.project = project
        mCheckMethodUsedExension = checkMethodUsedExension;
    }

    @Override
    Set<QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS
    }

    @Override
    boolean isIncremental() {
        return false
    }

    @Override
    void transform(Context context, Collection<TransformInput> inputs, Collection<TransformInput> referencedInputs, TransformOutputProvider outputProvider, boolean isIncremental) throws IOException, TransformException, InterruptedException {
        println 'noUsedMethod transform begin >>>.'
        mMethodUsedCheck = new MethodUsedCheck(mCheckMethodUsedExension, project);
        initInjectClassPath();
        inputs.each { TransformInput input ->
            input.directoryInputs.each { DirectoryInput directoryInput ->
                int[] resultCount = mMethodUsedCheck.checkDir(directoryInput.file.absolutePath)
                mNotUsedClassCount += resultCount[0]
                mNotUsedMethodCount += resultCount[1]
            }
        }
        println("Not used class count = " + mNotUsedClassCount)
        println("Not used method count = " + mNotUsedMethodCount)
        mMethodUsedCheck.saveTotalCount(mNotUsedClassCount, mNotUsedMethodCount)
        throw new RuntimeException("check method end successfully")
    }

    private void initInjectClassPath () {
        project.android.bootClasspath.each {
            mMethodUsedCheck.addClassPath((String) it.absolutePath);
        }

        //加入res modules下的依赖jar包
        File dir = project.file("../res/noexportlibs")
        if (dir != null && dir.isDirectory()) {
            dir.eachFileRecurse { File file ->
                String filePath = file.absolutePath;
                if (filePath.endsWith(".jar")) {
                    mMethodUsedCheck.addClassPath(filePath);
                }
            }
        }

        //加入本工程下的依赖jar包
        File localDir = project.file("exlibs");
        if (localDir != null && localDir.isDirectory()) {
            localDir.eachFileRecurse { File file ->
                String filePath = file.absolutePath;
                if (filePath.endsWith(".jar")) {
                    mMethodUsedCheck.addClassPath(filePath);
                }
            }
        }
        //加入其它工程的jar包
        File otherModuleDir = project.file("build/intermediates/exploded-aar");
        if (otherModuleDir != null && otherModuleDir.isDirectory()) {
            otherModuleDir.eachFileRecurse { File file ->
                String filePath = file.absolutePath;
                if (filePath.endsWith(".jar")) {
                    mMethodUsedCheck.addClassPath(filePath);
                }
            }
        }
    }
}