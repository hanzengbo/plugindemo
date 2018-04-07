package com.wblive.plugin

import com.android.SdkConstants
import javassist.*
import javassist.bytecode.AccessFlag

import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.regex.Matcher
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

//import java.lang.annotation.Annotation

public class AutoInject {
    private ClassPool pool;
    private List<String> mInjectPackageKyes = new ArrayList<>();
    private List<String> mDisinjectClassSuffix;
    private String mLogTag;

    public AutoInject(WBLivePluginExension pluginExension) {
        pool = new ClassPool();
        initInjectPackages(pluginExension);
        initLogTag(pluginExension);
        initDisinjectClasses(pluginExension);
    }

    private void initInjectPackages(WBLivePluginExension pluginExension) {
        mInjectPackageKyes = pluginExension.injectPackagesKey;
    }

    private void initLogTag(WBLivePluginExension pluginExension) {
        mLogTag = pluginExension.getLogTag();
        if (mLogTag == null || mLogTag.trim().length() == 0) {
            mLogTag = "WBLiveLog";
        }
    }

    private void initDisinjectClasses(WBLivePluginExension pluginExension) {
        mDisinjectClassSuffix = pluginExension.disinjectClassSuffix;
    }

    public void addClassPath(String path) {
        println('add classPath:' + path)
        pool.appendClassPath(path);
    }

    public void insertClassPath(String path) {
        println('insert classPath:' + path)
        pool.insertClassPath(path)
    }

    public String getInjectContent(CtClass ctClass, CtMethod method) {
        StringBuilder sb = new StringBuilder();
        String className = ctClass.name;
        String methodName = method.name;
        sb.append("String threadName = java.lang.Thread.currentThread().getName();");
        sb.append("android.util.Log.e(\"" + mLogTag + "\", \"ThreadName[\" + threadName + \"] \" + ");
        sb.append("\"enter method[" + className + "." + methodName + "]\");");
        println("insertcode is: " + sb.toString());
        return sb.toString();
    }

    public int[] injectDir(String path) {
        int[] injectCount = new int[4];
        if (mInjectPackageKyes == null || mInjectPackageKyes.size() == 0) {
            return injectCount;
        }
        pool.appendClassPath(path);
        File dir = new File(path)
        if (dir.isDirectory()) {
            dir.eachFileRecurse { File file ->
                String filePath = file.absolutePath
                //确保当前文件是class文件，并且不是系统自动生成的class文件
                if (filePath.endsWith(SdkConstants.DOT_CLASS)) {
                    // 判断当前目录是否是在我们的应用包里面
                    String className = filePath.substring(path.length() + 1, filePath.length() - SdkConstants.DOT_CLASS.length()).replaceAll(Matcher.quoteReplacement(File.separator), '.')
                    CtClass c = pool.getCtClass(className)
                    if (!isQualifiedClass(c)) {
                        injectCount[1]++;
                        return;
                    }
                    if (c.isFrozen()) {
                        c.defrost()
                    }
//                    println("dir className = " + className)
                    CtMethod[] methods = c.getDeclaredMethods();
                    for (CtMethod m : methods) {
                        if (!isQualifiedMethod(m)) {
                            injectCount[2]++;
                            continue;
                        }
  //                      try {
 //                           println("dir methodName = " + m.name)
                            m.insertBefore(getInjectContent(c, m));
                            injectCount[0]++;
         /*               } catch (CannotCompileException e) {
                            injectCount[3]++;
                            continue;
                        }*/
                    }
                    c.writeFile(path)
                    c.detach()
                }
            }
        }
        return injectCount;
    }

    public int[] injectJar(JarFile srcJarFile, File desJarFile) {
        int[] injectCount = new int[4];
        Enumeration<JarEntry> classes = srcJarFile.entries();
        if(!desJarFile.getParentFile().exists()){
            desJarFile.getParentFile().mkdirs();
        }
        if(desJarFile.exists()){
            desJarFile.delete();
        }
        ZipOutputStream outStream = new JarOutputStream(new FileOutputStream(desJarFile));
        // println("classes.size = " + classes.toList().size())
        while (classes.hasMoreElements()) {
            JarEntry libClass = classes.nextElement();
            String className = libClass.getName();
            //println("className = " + className)
            if (className.endsWith(SdkConstants.DOT_CLASS)) {
                className = className.substring(0, className.length() - SdkConstants.DOT_CLASS.length()).replaceAll('/', '.')
                try {
                    CtClass ctClass = pool.get(className);
                    if (ctClass.isFrozen()) {
                        ctClass.defrost()
                    }
                    if (!isQualifiedClass(ctClass)) {
                        zipFile(ctClass.toBytecode(), outStream, ctClass.getName().replaceAll("\\.", "/") + ".class");
                        injectCount[1]++;
                        continue;
                    }
                    CtMethod[] methods = ctClass.getDeclaredMethods();
                    for (CtMethod m : methods) {
                        if (!isQualifiedMethod(m)) {
                            injectCount[2]++;
                            continue;
                        }
                       // println 'method1 name ' + m.getName();
                       // try {
                            m.insertBefore(getInjectContent(ctClass, m));
                            injectCount[0]++;
//                        } catch (CannotCompileException e) {
//                            injectCount[3]++;
//                            continue;
//                        }
                    }
                    zipFile(ctClass.toBytecode(), outStream, ctClass.getName().replaceAll("\\.", "/") + ".class");
                } catch (javassist.NotFoundException e) {
                    println "class not found exception class name: " + className
                }
            }
        }
        outStream.close();
        return injectCount;
    }

    private boolean isQualifiedClass(CtClass ctClass) {
        if (ctClass.interface) {
            return false;
        }
        String className = ctClass.name;
        if (className.endsWith('R')
                || className.contains('R$')
                || className.endsWith('BuildConfig')) {
            return false;
        }

        CtMethod[] methods = ctClass.getDeclaredMethods();
        if (methods == null || methods.length == 0) {
            return false;
        }
        //屏蔽不需要注入的类，以后缀判断
        if (mDisinjectClassSuffix != null && mDisinjectClassSuffix.size() > 0) {
            for (String key : mDisinjectClassSuffix) {
                if (className.endsWith(key)) {
                    return false;
                }
            }
        }

        for (String key : mInjectPackageKyes) {
            if (className.startsWith(key.trim())) {
                return true;
            }
        }
        return false;
    }

    private boolean isQualifiedMethod(CtBehavior ctBehavior) {
        // synthetic 方法暂时不aop 比如AsyncTask 会生成一些同名 synthetic方法,对synthetic 以及private的方法也插入的代码，主要是针对lambda表达式
        if ((ctBehavior.getModifiers() & AccessFlag.SYNTHETIC) != 0 && !AccessFlag.isPrivate(ctBehavior.getModifiers())) {
            return false;
        }
        if (ctBehavior.getMethodInfo().isConstructor()) {
            return false;
        }

        if ((ctBehavior.getModifiers() & AccessFlag.ABSTRACT) != 0) {
            return false;
        }
        if ((ctBehavior.getModifiers() & AccessFlag.NATIVE) != 0) {
            return false;
        }
        if ((ctBehavior.getModifiers() & AccessFlag.INTERFACE) != 0) {
            return false;
        }
        return true;
    }
    private  void zipFile(byte[] classBytesArray, ZipOutputStream zos, String entryName) {
        try {
            ZipEntry entry = new ZipEntry(entryName);
            zos.putNextEntry(entry);
            zos.write(classBytesArray, 0, classBytesArray.length);
            zos.closeEntry();
            zos.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}