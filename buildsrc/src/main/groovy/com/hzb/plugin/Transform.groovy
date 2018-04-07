package com.hzb.plugin

import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils
import org.gradle.api.Project

import java.util.jar.JarFile

public class Transform extends com.android.build.api.transform.Transform {

    Project project
    AutoInject mAutoInject;
    private int mInjectCount;
    private int mInterfaceClassCount;
    private int mEmptyMethodCount;
    private int mExceptionMethodCount;
    private WBLivePluginExension mLivePluginExension;

    @Override
    String getName() {
        return "wblive"
    }

    public Transform(Project project, WBLivePluginExension pluginExension) {
        this.project = project
        mLivePluginExension = pluginExension;
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
        println 'transform begin >>>.'
        mAutoInject = new AutoInject(mLivePluginExension);

        initInjectClassPath();
        inputs.each { TransformInput input ->

            input.directoryInputs.each { DirectoryInput directoryInput ->
                def dest = outputProvider.getContentLocation(directoryInput.name,
                        directoryInput.contentTypes, directoryInput.scopes,
                        Format.DIRECTORY)
                if (mLivePluginExension.injectEnable) {
                    //文件夹里面包含的是我们手写的类以及R.class、BuildConfig.class以及R$XXX.class等
                    int[] injectResult = mAutoInject.injectDir(directoryInput.file.absolutePath)
                    mInjectCount += injectResult[0];
                    mInterfaceClassCount += injectResult[1];
                    mEmptyMethodCount += injectResult[2];
                    mExceptionMethodCount += injectResult[3];
                    // 获取output目录
                    println("inputDir = " + directoryInput.file.absolutePath);
                    println("dest = " + dest)
                }
                // 将input的目录复制到output指定目录
                FileUtils.copyDirectory(directoryInput.file, dest)
            }

            input.jarInputs.each { JarInput jarInput ->
                //jar文件一般是第三方依赖库jar文件
                // 重命名输出文件（同目录copyFile会冲突）
                def jarName = jarInput.name
                def md5Name = DigestUtils.md5Hex(jarInput.file.getAbsolutePath())
                if (jarName.endsWith(".jar")) {
                    jarName = jarName.substring(0, jarName.length() - 4)
                }
                //生成输出路径
                File dest = outputProvider.getContentLocation(jarName + md5Name,
                        jarInput.contentTypes, jarInput.scopes, Format.JAR)
                if (mLivePluginExension.injectEnable) {
                    mAutoInject.insertClassPath(jarInput.file.getAbsolutePath());
                    def srcJarFile = new JarFile(jarInput.file);
                    println("inputDir = " + jarInput.file.getAbsolutePath());
                    println("dest = " + dest)
                    int[] injectResult = mAutoInject.injectJar(srcJarFile, dest);
                    mInjectCount += injectResult[0];
                    mInterfaceClassCount += injectResult[1];
                    mEmptyMethodCount += injectResult[2];
                    mExceptionMethodCount += injectResult[3];
                } else {
                    //将输入内容复制到输出
                    FileUtils.copyFile(jarInput.file, dest)
                }
            }
        }
        println("inject method count " + mInjectCount + " disInject class count = " + mInterfaceClassCount
                + " empty method count = " + mEmptyMethodCount + " inject exception method count = " + mExceptionMethodCount);
    }

    private void initInjectClassPath () {
        project.android.bootClasspath.each {
            mAutoInject.addClassPath((String) it.absolutePath);
        }

        //加入res modules下的依赖jar包
        File dir = project.file("../res/noexportlibs")
        if (dir != null && dir.isDirectory()) {
            dir.eachFileRecurse { File file ->
                String filePath = file.absolutePath;
                if (filePath.endsWith(".jar")) {
                    mAutoInject.addClassPath(filePath);
                }
            }
        }

        //加入本工程下的依赖jar包
        File localDir = project.file("exlibs");
        if (localDir != null && localDir.isDirectory()) {
            localDir.eachFileRecurse { File file ->
                String filePath = file.absolutePath;
                if (filePath.endsWith(".jar")) {
                    mAutoInject.addClassPath(filePath);
                }
            }
        }
        //加入其它工程的jar包
        File otherModuleDir = project.file("build/intermediates/exploded-aar");
        if (otherModuleDir != null && otherModuleDir.isDirectory()) {
            otherModuleDir.eachFileRecurse { File file ->
                String filePath = file.absolutePath;
                if (filePath.endsWith("classes.jar")) {
                    mAutoInject.addClassPath(filePath);
                }
            }
        }
    }
}