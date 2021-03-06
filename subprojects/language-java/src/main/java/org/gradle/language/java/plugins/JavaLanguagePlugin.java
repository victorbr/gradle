/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.language.java.plugins;

import org.gradle.api.*;
import org.gradle.api.internal.file.FileResolver;
import org.gradle.internal.reflect.Instantiator;
import org.gradle.internal.service.ServiceRegistry;
import org.gradle.jvm.JvmBinarySpec;
import org.gradle.jvm.JvmByteCode;
import org.gradle.language.base.LanguageSourceSet;
import org.gradle.language.base.internal.registry.AbstractLanguageRegistration;
import org.gradle.language.base.internal.registry.LanguageRegistry;
import org.gradle.language.base.internal.SourceTransformTaskConfig;
import org.gradle.language.base.plugins.ComponentModelBasePlugin;
import org.gradle.language.java.JavaSourceSet;
import org.gradle.language.java.internal.DefaultJavaSourceSet;
import org.gradle.language.java.tasks.PlatformJavaCompile;
import org.gradle.language.jvm.plugins.JvmResourcesPlugin;
import org.gradle.model.Mutate;
import org.gradle.model.RuleSource;
import org.gradle.platform.base.BinarySpec;
import org.gradle.platform.base.TransformationFileType;

import java.io.File;
import java.util.Collections;
import java.util.Map;

/**
 * Plugin for compiling Java code. Applies the {@link org.gradle.language.base.plugins.ComponentModelBasePlugin} and {@link org.gradle.language.jvm.plugins.JvmResourcesPlugin}. Registers "java"
 * language support with the {@link JavaSourceSet}.
 */
public class JavaLanguagePlugin implements Plugin<Project> {

    public void apply(Project project) {
        project.getPluginManager().apply(ComponentModelBasePlugin.class);
        project.getPluginManager().apply(JvmResourcesPlugin.class);
    }

    /**
     * Model rules.
     */
    @SuppressWarnings("UnusedDeclaration")
    @RuleSource
    static class Rules {
        @Mutate
        void registerLanguage(LanguageRegistry languages, ServiceRegistry serviceRegistry) {
            languages.add(new Java(serviceRegistry.get(Instantiator.class), serviceRegistry.get(FileResolver.class)));
        }
    }

    private static class Java extends AbstractLanguageRegistration<JavaSourceSet> {
        public Java(Instantiator instantiator, FileResolver fileResolver) {
            super(instantiator, fileResolver);
        }

        public String getName() {
            return "java";
        }

        public Class<JavaSourceSet> getSourceSetType() {
            return JavaSourceSet.class;
        }

        public Class<? extends JavaSourceSet> getSourceSetImplementation() {
            return DefaultJavaSourceSet.class;
        }

        public Map<String, Class<?>> getBinaryTools() {
            return Collections.emptyMap();
        }

        public Class<? extends TransformationFileType> getOutputType() {
            return JvmByteCode.class;
        }

        public SourceTransformTaskConfig getTransformTask() {
            return new SourceTransformTaskConfig() {
                public String getTaskPrefix() {
                    return "compile";
                }

                public Class<? extends DefaultTask> getTaskType() {
                    return PlatformJavaCompile.class;
                }

                public void configureTask(Task task, BinarySpec binarySpec, LanguageSourceSet sourceSet) {
                    PlatformJavaCompile compile = (PlatformJavaCompile) task;
                    JavaSourceSet javaSourceSet = (JavaSourceSet) sourceSet;
                    JvmBinarySpec binary = (JvmBinarySpec) binarySpec;

                    compile.setDescription(String.format("Compiles %s.", javaSourceSet));
                    compile.setDestinationDir(binary.getClassesDir());
                    compile.setToolChain(binary.getToolChain());
                    compile.setPlatform(binary.getTargetPlatform());

                    compile.setSource(javaSourceSet.getSource());
                    compile.setClasspath(javaSourceSet.getCompileClasspath().getFiles());
                    compile.setTargetCompatibility(binary.getTargetPlatform().getTargetCompatibility().toString());
                    compile.setSourceCompatibility(binary.getTargetPlatform().getTargetCompatibility().toString());

                    compile.setDependencyCacheDir(new File(compile.getProject().getBuildDir(), "jvm-dep-cache"));
                    compile.dependsOn(javaSourceSet);
                    binary.getTasks().getJar().dependsOn(compile);
                }
            };
        }

        public boolean applyToBinary(BinarySpec binary) {
            return binary instanceof JvmBinarySpec;
        }
    }
}
