/**
 * Copyright 2013-2014 Guoqiang Chen, Shanghai, China. All rights reserved.
 *
 *   Author: Guoqiang Chen
 *    Email: subchen@gmail.com
 *   WebURL: https://github.com/subchen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jetbrick.template;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import jetbrick.io.resource.Resource;
import jetbrick.io.resource.ResourceNotFoundException;
import jetbrick.template.parser.Symbols;
import jetbrick.template.resolver.GlobalResolver;
import jetbrick.template.resource.SourceResource;
import jetbrick.template.resource.loader.ResourceLoader;
import jetbrick.template.runtime.JetForIterator;
import jetbrick.template.runtime.buildin.*;
import jetbrick.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 默认的模板引擎实现.(用户不可自己访问，由 JetEngine.create() 创建)
 */
final class JetEngineImpl extends JetEngine {
    private final Logger log = LoggerFactory.getLogger(JetEngineImpl.class);

    // 模板对象缓存
    private final ConcurrentMap<String, JetTemplate> cache = new ConcurrentHashMap<String, JetTemplate>(128);

    private final JetConfig config;
    private final List<ResourceLoader> loaders; // 全局的资源载入器
    private final JetGlobalContext globalContext; // 全局变量
    private final GlobalResolver globalResolver;

    public JetEngineImpl(JetConfig config) {
        this.config = config;
        this.loaders = config.getTemplateLoaders();

        // output log
        log.info("JetEngine.version = {}", JetEngine.VERSION);
        for (ResourceLoader loader : loaders) {
            log.info("JetEngine.loader = {}, root = {}", loader.getClass().getName(), loader.getRoot());
        }
        log.info("JetEngine.reload = {}", config.isTemplateReload());

        // create globals
        this.globalResolver = doCreateGlobalResolver();
        this.globalContext = doCreateGlobalContext();
    }

    @Override
    public JetConfig getConfig() {
        return config;
    }

    @Override
    public JetGlobalContext getGlobalContext() {
        return globalContext;
    }

    @Override
    public GlobalResolver getGlobalResolver() {
        return globalResolver;
    }

    @Override
    public boolean templateExists(String name) {
        return internalGetTemplate(name) != null;
    }

    @Override
    public JetTemplate getTemplate(String name) throws TemplateNotFoundException {
        JetTemplate template = internalGetTemplate(name);
        if (template != null) {
            try {
                template.reload();
                return template;
            } catch (TemplateNotFoundException e) {
                cache.remove(template.getName());
                throw e;
            }
        }
        throw new TemplateNotFoundException(name);
    }

    @Override
    public JetTemplate createTemplate(String source) {
        Resource resource = new SourceResource(source);
        JetTemplate template = new JetTemplateImpl(this, resource);
        template.reload();
        return template;
    }

    private JetTemplate internalGetTemplate(String name) {
        // 将一个模板路径名称转为标准格式
        name = PathUtils.normalize(name);
        if (name.startsWith("../")) {
            throw new TemplateNotFoundException("path is not under template root: " + name);
        }

        JetTemplate template = cache.get(name);
        if (template != null) {
            return template;
        }

        for (ResourceLoader loader : loaders) {
            Resource resource = loader.load(name);
            if (resource != null) {
                // create a new template
                template = new JetTemplateImpl(this, resource);
                JetTemplate old = cache.putIfAbsent(name, template);
                if (old != null) {
                    template = old;
                }
                return template;
            }
        }

        return null;
    }

    public Resource getResource(String name) throws ResourceNotFoundException {
        // 将一个路径名称转为标准格式
        name = PathUtils.normalize(name);
        if (name.startsWith("../")) {
            throw new ResourceNotFoundException("path is not under template root: " + name);
        }

        for (ResourceLoader loader : loaders) {
            Resource resource = loader.load(name);
            if (resource != null) {
                return resource;
            }
        }
        throw new ResourceNotFoundException(name);
    }

    private GlobalResolver doCreateGlobalResolver() {
        log.debug("Initializing global resolver ...");
        GlobalResolver resolver = new GlobalResolver();

        resolver.importClass("java.lang.*");
        resolver.importClass("java.util.*");
        for (String className : config.getImportClasses()) {
            resolver.importClass(className);
        }

        resolver.registerMethods(JetMethods.class);
        for (String className : config.getImportMethods()) {
            resolver.registerMethods(className);
        }

        resolver.registerFunctions(JetFunctions.class);
        for (String className : config.getImportFunctions()) {
            resolver.registerFunctions(className);
        }

        resolver.registerTags(JetTags.class);
        for (String className : config.getImportTags()) {
            resolver.registerTags(className);
        }

        List<String> macroFiles = config.getImportMacros();
        for (String file : macroFiles) {
            JetTemplate template = getTemplate(file);
            resolver.registerMacros(template);
        }

        List<String> packageNames = config.getAutoscanPackages();
        if (!packageNames.isEmpty()) {
            resolver.scan(packageNames, config.isAutoscanSkiperrors());
        }

        return resolver;
    }

    private JetGlobalContext doCreateGlobalContext() {
        log.debug("Initializing global context ...");
        JetGlobalContext ctx = new JetGlobalContext();

        ctx.define(JetForIterator.class, Symbols.FOR);

        for (String define : config.getImportDefines()) {
            int pos = define.indexOf(' ');
            String type = define.substring(0, pos);
            String name = define.substring(pos + 1);

            Class<?> cls = globalResolver.resolveClass(type);
            ctx.define(cls, name);
        }

        return ctx;
    }

}
