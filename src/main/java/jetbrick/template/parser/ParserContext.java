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
package jetbrick.template.parser;

import java.util.*;
import jetbrick.template.Errors;
import jetbrick.template.JetGlobalContext;
import jetbrick.template.parser.ast.AstDirectiveMacro;
import jetbrick.template.resolver.GlobalResolver;
import jetbrick.template.resolver.clazz.ClassResolver;

/**
 * 传递给 parser 的上下文信息，并回收需要的信息
 */
public final class ParserContext {
    // options
    private boolean strict;
    private boolean safecall;
    private boolean trimLeadingWhitespaces;

    // class resolver (#import)
    private final GlobalResolver globalResolver;
    private ClassResolver localClassResolver;

    // symbols (#define)
    private final JetGlobalContext globalContext;
    private final Map<String, Class<?>> localSymbols;
    private Map<String, Class<?>> currentSymbols;

    // macro (#loadmacro, #macro)
    private List<String> loadMacroFiles;
    private List<AstDirectiveMacro> declaredMacros;

    //
    public ParserContext(GlobalResolver globalResolver, JetGlobalContext globalContext) {
        this.globalResolver = globalResolver;
        this.globalContext = globalContext;
        this.localSymbols = new HashMap<String, Class<?>>();
        this.currentSymbols = this.localSymbols;
    }

    // ---- options (#options) --------------------------------------------

    public boolean isStrict() {
        return strict;
    }

    public void setStrict(boolean strict) {
        this.strict = strict;
    }

    public boolean isSafecall() {
        return safecall;
    }

    public void setSafecall(boolean safecall) {
        this.safecall = safecall;
    }

    public boolean isTrimLeadingWhitespaces() {
        return trimLeadingWhitespaces;
    }

    public void setTrimLeadingWhitespaces(boolean trimLeadingWhitespaces) {
        this.trimLeadingWhitespaces = trimLeadingWhitespaces;
    }

    // ---- class resolver (#import) --------------------------------------------

    public void importClass(String name) {
        if (localClassResolver == null) {
            localClassResolver = new ClassResolver();
        }
        localClassResolver.importClass(name);
    }

    public Class<?> resolveClass(String className) {
        Class<?> cls = null;

        if (localClassResolver != null) {
            cls = localClassResolver.resolveClass(className);
        }

        if (cls == null) {
            cls = globalResolver.resolveClass(className);
        }

        return cls;
    }

    // ---- symbols (#define) --------------------------------------------
    public void enterMacros() {
        // macros arguments, keep order and allow null
        currentSymbols = new LinkedHashMap<String, Class<?>>();
    }

    public void exitMacros() {
        currentSymbols = localSymbols;
    }

    public void defineSymbol(String name, Class<?> type) throws IllegalStateException {
        defineSymbol(name, type, false);
    }

    public void defineSymbol(String name, Class<?> type, boolean allowSameRedefine) throws IllegalStateException {
        if (type == null && strict) {
            throw new IllegalStateException(Errors.format(Errors.PARAM_TYPE_MISSING, name));
        }

        if (currentSymbols.containsKey(name)) {
            Class<?> old = currentSymbols.get(name);
            if (allowSameRedefine && type == old) {
                return;
            }
            throw new IllegalStateException(Errors.format(Errors.VAR_REDEFINE, name));
        }

        currentSymbols.put(name, type);
    }

    public void useSymbol(String name) throws IllegalStateException {
        if (currentSymbols.containsKey(name)) {
            return;
        }

        Class<?> type = globalContext.getType(name);
        currentSymbols.put(name, type); // we also put null value

        if (type == null && strict) {
            throw new IllegalStateException(Errors.format(Errors.VAR_UNDEFINED, name));
        }
    }

    public Map<String, Class<?>> getSymbols() {
        return currentSymbols;
    }

    // 获取 macro 参数名称列表
    public List<String> getMacroArgumentNames() {
        if (currentSymbols instanceof LinkedHashMap) {
            if (currentSymbols.isEmpty()) {
                return Collections.emptyList();
            }
            return new ArrayList<String>(currentSymbols.keySet());
        }
        throw new IllegalStateException("Not in macro defination");
    }

    // ---- macros (#loadmacro / #macro) --------------------------------------------

    public void loadMacroFile(String file) {
        if (loadMacroFiles == null) {
            loadMacroFiles = new ArrayList<String>(4);
        }
        loadMacroFiles.add(file);
    }

    public void defineMacro(AstDirectiveMacro macro) {
        if (declaredMacros == null) {
            declaredMacros = new ArrayList<AstDirectiveMacro>(8);
        } else {
            for (AstDirectiveMacro old : declaredMacros) {
                if (macro.getName().equals(old.getName())) {
                    throw new IllegalStateException(Errors.format(Errors.DUPLICATED_MACRO_NAME, macro.getName()));
                }
            }
        }
        declaredMacros.add(macro);
    }

    public List<String> getLoadMacroFiles() {
        return loadMacroFiles;
    }

    public List<AstDirectiveMacro> getDeclaredMacros() {
        return declaredMacros;
    }

}
