package com.hzb.plugin.method

import com.android.SdkConstants
import javassist.*
import javassist.bytecode.AccessFlag
import org.gradle.api.Project

import java.util.regex.Matcher

//import java.lang.annotation.Annotation

public class MethodUsedCheck {
    Project project
    private ClassPool pool
    private List<String> mCheckPackageKyes = new ArrayList<>();
    ReadMapping mReadMapping;
    private String mResultFile;

    public MethodUsedCheck(CheckMethodUsedExtension pluginExension, Project project) {
        pool = new ClassPool();
        this.project = project;
        initCheckPackages(pluginExension);
    }

    private void initCheckPackages(CheckMethodUsedExtension pluginExension) {
        mResultFile = pluginExension.getOutFilePath();
        if (mResultFile == null || mResultFile.length() == 0) {
            mResultFile = project.rootDir.getAbsolutePath() + "/NoUsedResult.txt";
        }
        FileUtil.deleteFile(mResultFile);
        mReadMapping = ReadMapping.getInstance();
        mReadMapping.initMappingInfo(pluginExension.getMappingFilePath());
    }

    public void addClassPath(String path) {
        //println('add classPath:' + path)
        pool.appendClassPath(path);
    }

    public void insertClassPath(String path) {
        println('insert classPath:' + path)
        pool.insertClassPath(path)
    }

    public int[] checkDir(String path) {
        println("checkDir begin path = " + path)
        int[] resultCount = new int[2];
        pool.appendClassPath(path);
        File dir = new File(path)
        String className, methodName;
        if (dir.isDirectory()) {
            dir.eachFileRecurse { File file ->
                String filePath = file.absolutePath
                //确保当前文件是class文件，并且不是系统自动生成的class文件
                if (filePath.endsWith(SdkConstants.DOT_CLASS)) {
                    // 判断当前目录是否是在我们的应用包里面
                    className = filePath.substring(path.length() + 1, filePath.length() - SdkConstants.DOT_CLASS.length()).replaceAll(Matcher.quoteReplacement(File.separator), '.')
                    //println("className = " + className)
                    CtClass c = pool.getCtClass(className)
                    if (!isQualifiedClass(c)) {
                        return;
                    }
                    if (ReadMapping.getInstance().classHasNoUsed(className)) {
                        println("No used className = " + className)
                        resultCount[0]++;
                        FileUtil.writeFile(mResultFile, "ClassNotUsed:" + className + "\r\n")
                        return;
                    }
                    CtMethod[] methods = c.getDeclaredMethods();
                    for (CtMethod m : methods) {
                        if (!isQualifiedMethod(m)) {
                            continue;
                        }
                        methodName = getJavaMethodSignureWithReturnType(m)
                        //println("method " + className + "." + methodName)
                        if (ReadMapping.getInstance().methodHasNoUsed(className, methodName)) {
                            println("No used method " + className + "." + methodName)
                            FileUtil.writeFile(mResultFile, "MethodNotUsed:" + className + "[" + methodName + "]\r\n")
                            resultCount[1]++;
                            continue;
                        }
                    }
                }
            }
        }
        return resultCount;
    }

    private boolean isQualifiedClass(CtClass ctClass) {
        String className = ctClass.name;
        if (className.endsWith('R')
                || className.contains('R$')
                || className.endsWith('BuildConfig')) {
            return false;
        }
        return true;
    }

    private boolean isQualifiedMethod(CtBehavior ctBehavior) {
        // synthetic 方法暂时不aop 比如AsyncTask 会生成一些同名 synthetic方法,对synthetic 以及private的方法也插入的代码，主要是针对lambda表达式
        if ((ctBehavior.getModifiers() & AccessFlag.SYNTHETIC) != 0 && !AccessFlag.isPrivate(ctBehavior.getModifiers())) {
            return false;
        }
        return true;
    }

    private String getJavaMethodSignureWithReturnType(CtMethod ctMethod) {
        StringBuilder methodSignure = new StringBuilder();
        methodSignure.append(ctMethod.returnType.name)
        methodSignure.append(" ")
        methodSignure.append(ctMethod.name);
        methodSignure.append("(");
        for (int i = 0; i < ctMethod.getParameterTypes().length; i++) {
            methodSignure.append(ctMethod.getParameterTypes()[i].getName());
            if (i != ctMethod.getParameterTypes().length - 1) {
                methodSignure.append(",");
            }
        }
        methodSignure.append(")")
        return methodSignure.toString();
    }

    public void saveTotalCount(int classNotUsedCount, int methodNotUsedCount) {
        FileUtil.writeFile(mResultFile, "NotUsedClassCount:" + classNotUsedCount +
                "   mNotUsedMethodCount:" + methodNotUsedCount +"\r\n")
    }
}