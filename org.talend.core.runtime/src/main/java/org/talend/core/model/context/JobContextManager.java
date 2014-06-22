// ============================================================================
//
// Copyright (C) 2006-2011 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.core.model.context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.eclipse.emf.common.util.EList;
import org.talend.commons.ui.runtime.exception.ExceptionHandler;
import org.talend.commons.utils.PasswordEncryptUtil;
import org.talend.core.language.ECodeLanguage;
import org.talend.core.language.LanguageManager;
import org.talend.core.model.metadata.MetadataTalendType;
import org.talend.core.model.metadata.types.ContextParameterJavaTypeManager;
import org.talend.core.model.process.IContext;
import org.talend.core.model.process.IContextListener;
import org.talend.core.model.process.IContextManager;
import org.talend.core.model.process.IContextParameter;
import org.talend.core.model.properties.ContextItem;
import org.talend.designer.core.model.utils.emf.talendfile.ContextParameterType;
import org.talend.designer.core.model.utils.emf.talendfile.ContextType;
import org.talend.designer.core.model.utils.emf.talendfile.TalendFileFactory;
import org.talend.repository.model.RepositoryConstants;

/**
 * DOC nrousseau class global comment. Detailled comment <br/>
 * 
 * $Id: JobContextManager.java 51129 2010-11-10 06:35:59Z nrousseau $
 * 
 */
public class JobContextManager implements IContextManager {

    private IContext defaultContext = new JobContext(IContext.DEFAULT);

    private List<IContext> listContext = new ArrayList<IContext>();

    private List<IContextListener> contextListenerList = new ArrayList<IContextListener>();

    /*
     * record the renamed var.
     */
    private Map<String, String> nameMap = new HashMap<String, String>();

    private Map<ContextItem, Map<String, String>> repositoryRenamedMap = new HashMap<ContextItem, Map<String, String>>();

    /*
     * when modify(renamed var, removed var, modified value) the context, it will be true.
     * 
     * this flag only works for update the var of reference(job context, tRunjob).
     */
    private boolean modified = false;

    /*
     * detected the context parameter source is lost. (feature 3232)
     */
    private Set<String> lostParameters = new HashSet<String>();

    private Set<String> newParameters = new HashSet<String>();

    /*
     * record the original parameters (bug 4988)
     */
    private Set<String> originalParamerters = new HashSet<String>();

    private Map<ContextItem, Set<String>> newParametersMap = new HashMap<ContextItem, Set<String>>();

    /*
     * for context group
     */
    private List<IContext> addGroupContext = new ArrayList<IContext>();

    private List<IContext> removeGroupContext = new ArrayList<IContext>();

    private Map<IContext, String> renameGroupContext = new HashMap<IContext, String>();

    // add this for remark
    private Map<ContextItem, List<IContext>> addContextGroupMap = new HashMap<ContextItem, List<IContext>>();

    private Map<ContextItem, List<IContext>> removeContextGroupMap = new HashMap<ContextItem, List<IContext>>();

    private Map<ContextItem, List<IContext>> renameContextGroupMap = new HashMap<ContextItem, List<IContext>>();

    public Map<ContextItem, List<IContext>> getAddContextGroupMap() {
        return this.addContextGroupMap;
    }

    public void setAddContextGroupMap(Map<ContextItem, List<IContext>> addContextGroupMap) {
        this.addContextGroupMap = addContextGroupMap;
    }

    public Map<ContextItem, List<IContext>> getRemoveContextGroupMap() {
        return this.removeContextGroupMap;
    }

    public void setRemoveContextGroupMap(Map<ContextItem, List<IContext>> removeContextGroupMap) {
        this.removeContextGroupMap = removeContextGroupMap;
    }

    public Map<ContextItem, List<IContext>> getRenameContextGroupMap() {
        return this.renameContextGroupMap;
    }

    public void setRenameContextGroupMap(Map<ContextItem, List<IContext>> renameContextGroupMap) {
        this.renameContextGroupMap = renameContextGroupMap;
    }

    public JobContextManager() {
        listContext.add(defaultContext);
    }

    public JobContextManager(EList contextTypeList, String defaultContextName) {
        loadFromEmf(contextTypeList, defaultContextName);
    }

    public void addContextListener(IContextListener listener) {
        contextListenerList.add(listener);
    }

    public void removeContextListener(IContextListener listener) {
        contextListenerList.remove(listener);
    }

    public void fireContextsChangedEvent() {
        for (IContextListener contextListener : contextListenerList) {
            contextListener.contextsChanged();
        }
    }

    public void setAddGroupContext(List<IContext> addGroupContext) {
        this.addGroupContext = addGroupContext;
    }

    public List<IContext> getAddGroupContext() {
        return addGroupContext;
    }

    public void setRemoveGroupContext(List<IContext> removeGroupContext) {
        this.removeGroupContext = removeGroupContext;
    }

    public List<IContext> getRemoveGroupContext() {
        return removeGroupContext;
    }

    public void setRenameGroupContext(Map<IContext, String> renameGroupContext) {
        this.renameGroupContext = renameGroupContext;
    }

    public Map<IContext, String> getRenameGroupContext() {
        return renameGroupContext;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.core.model.process.IContextManager#getDefaultContext()
     */
    public IContext getDefaultContext() {
        return defaultContext;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.core.model.process.IContextManager#setDefaultContext(org.talend.core.model.process.IContext)
     */
    public void setDefaultContext(IContext context) {
        defaultContext = context;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.core.model.process.IContextManager#getListContext()
     */
    public List<IContext> getListContext() {
        return listContext;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.core.model.process.IContextManager#getListContext(java.util.List)
     */
    public void setListContext(List<IContext> listContext) {
        this.listContext = listContext;
    }

    /**
     * Check if the given name will be unique in the process. If another link already exists with that name, false will
     * be returned.
     * 
     * @param uniqueName
     * @return true if the name is unique
     */
    public boolean checkValidParameterName(String oldContextName, String newContextName) {
        for (IContextParameter contextParameter : listContext.get(0).getContextParameterList()) {
            // TDI-17682:avoid to compare the lower/uper case with the parameter itself
            if (oldContextName != null) {
                if (contextParameter.getName() != oldContextName) {
                    if (contextParameter.getName().equals(newContextName)
                            || contextParameter.getName().toLowerCase().equals(newContextName.toLowerCase())) {
                        return false;
                    }
                }
            } else {
                if (contextParameter.getName().equals(newContextName)
                        || contextParameter.getName().toLowerCase().equals(newContextName.toLowerCase())) {
                    return false;
                }
            }
        }
        if (LanguageManager.getCurrentLanguage() == ECodeLanguage.JAVA) {
            // for java, the var name not be named with java keywords.
            if (ContextUtils.isJavaKeyWords(newContextName)) {
                return false;
            }
        }
        return Pattern.matches(RepositoryConstants.CONTEXT_AND_VARIABLE_PATTERN, newContextName);
    }

    public IContext getContext(String name) {
        for (int i = 0; i < listContext.size(); i++) {
            if (listContext.get(i).getName().equals(name)) {
                return listContext.get(i);
            }
        }
        return defaultContext;
    }

    public void saveToEmf(EList contextTypeList) {
        ContextType contextType;
        IContext context;

        EList contextTypeParamList;
        ContextParameterType contextParamType;
        IContextParameter contextParam;
        if (listContext.isEmpty()) {
            retrieveDefaultContext();
        }
        for (int i = 0; i < listContext.size(); i++) {
            contextType = TalendFileFactory.eINSTANCE.createContextType();
            context = listContext.get(i);
            contextType.setName(context.getName());
            contextType.setConfirmationNeeded(context.isConfirmationNeeded());
            contextTypeParamList = contextType.getContextParameter();

            if (context.getContextParameterList() != null) {
                for (int j = 0; j < context.getContextParameterList().size(); j++) {
                    contextParamType = TalendFileFactory.eINSTANCE.createContextParameterType();
                    contextParam = context.getContextParameterList().get(j);
                    contextParamType.setName(contextParam.getName());
                    contextParamType.setPrompt(contextParam.getPrompt());
                    contextParamType.setType(contextParam.getType());
                    if (PasswordEncryptUtil.isPasswordType(contextParamType.getType())) {
                        // see 0000949: Encryption of DB passwords in XMI
                        // repository files
                        try {
                            String password = PasswordEncryptUtil.encryptPassword(contextParam.getValue());
                            contextParamType.setValue(password);
                        } catch (Exception e) {
                            ExceptionHandler.process(e);
                        }

                    } else {
                        contextParamType.setValue(contextParam.getValue());
                    }
                    contextParamType.setPromptNeeded(contextParam.isPromptNeeded());
                    contextParamType.setComment(contextParam.getComment());
                    if (!contextParam.isBuiltIn()) {
                        ContextItem item = ContextUtils.getContextItemById2(contextParam.getSource());
                        if (item != null) {
                            contextParamType.setRepositoryContextId(item.getProperty().getId());
                        }
                    }
                    contextTypeParamList.add(contextParamType);
                }
            }
            contextTypeList.add(contextType);
        }
    }

    public void loadFromEmf(EList contextTypeList, String defaultContextName) {
        IContext context;
        ContextType contextType;
        List<IContextParameter> contextParamList;
        EList contextTypeParamList;
        ContextParameterType contextParamType;
        JobContextParameter contextParam;

        lostParameters.clear();
        listContext.clear();
        if (contextTypeList == null || contextTypeList.isEmpty()) {
            // set default context
            retrieveDefaultContext();
            return;
        }
        List<ContextItem> contextItemList = ContextUtils.getAllContextItem();
        boolean setDefault = false;
        for (int i = 0; i < contextTypeList.size(); i++) {
            contextType = (ContextType) contextTypeList.get(i);
            String name = contextType.getName();
            if (name == null) {
                name = "Default";
            }
            context = new JobContext(name);
            context.setConfirmationNeeded(contextType.isConfirmationNeeded());
            contextParamList = new ArrayList<IContextParameter>();
            contextTypeParamList = contextType.getContextParameter();

            for (int j = 0; j < contextTypeParamList.size(); j++) {
                contextParamType = (ContextParameterType) contextTypeParamList.get(j);
                contextParam = new JobContextParameter();
                contextParam.setContext(context);
                contextParam.setName(contextParamType.getName());
                contextParam.setPrompt(contextParamType.getPrompt());
                originalParamerters.add(contextParam.getName());
                boolean exists = false;
                ECodeLanguage curLanguage = LanguageManager.getCurrentLanguage();
                if (curLanguage == ECodeLanguage.JAVA) {
                    exists = true;
                    try {
                        ContextParameterJavaTypeManager.getJavaTypeFromId(contextParamType.getType());
                    } catch (IllegalArgumentException e) {
                        exists = false;
                    }
                } else {
                    String[] existingTypes;
                    existingTypes = ContextParameterJavaTypeManager.getPerlTypesLabels();
                    for (int k = 0; k < existingTypes.length; k++) {
                        if (existingTypes[k].equals(contextParamType.getType())) {
                            exists = true;
                        }
                    }
                }
                if (exists) {
                    contextParam.setType(contextParamType.getType());
                } else {
                    contextParam.setType(MetadataTalendType.getDefaultTalendType());
                }
                contextParam.setValue(contextParamType.getValue());
                contextParam.setPromptNeeded(contextParamType.isPromptNeeded());
                contextParam.setComment(contextParamType.getComment());

                String repositoryContextId = contextParamType.getRepositoryContextId();
                String source = IContextParameter.BUILT_IN;
                if (repositoryContextId != null && !"".equals(repositoryContextId)) { //$NON-NLS-1$
                    ContextItem item = ContextUtils.getContextItemById(contextItemList, repositoryContextId);
                    if (item != null) {
                        source = item.getProperty().getId();
                    } else {
                        lostParameters.add(contextParam.getName());
                    }
                }
                contextParam.setSource(source);
                contextParamList.add(contextParam);
            }
            context.setContextParameterList(contextParamList);

            if (context.getName().equals(defaultContextName)) {
                setDefaultContext(context);
                setDefault = true;
            }
            listContext.add(context);
        }
        if (!setDefault) {
            setDefaultContext(listContext.get(0));
        }
    }

    public boolean sameAs(IContextManager contextManager) {
        if (!contextManager.getDefaultContext().getName().equals(defaultContext.getName())) {
            return false;
        }
        if (listContext.size() != contextManager.getListContext().size()) {
            return false;
        }
        for (int i = 0; i < listContext.size(); i++) {
            IContext curContext = listContext.get(i);
            IContext testContext = contextManager.getListContext().get(i);
            if (!curContext.sameAs(testContext)) {
                return false;
            }
        }
        return true;
    }

    public void addNewName(String newName, String oldName) {
        String name = nameMap.get(oldName);
        if (name != null) {
            nameMap.remove(oldName);
            nameMap.put(newName, name);
        } else {
            nameMap.put(newName, oldName);
        }

        // check if the newly added parameters is renamed
        updateNewParameters(newName, oldName);
    }

    public Map<String, String> getNameMap() {
        return nameMap;
    }

    public void setModified(boolean modified) {
        this.modified = modified;
    }

    public boolean isModified() {
        return modified;
    }

    /**
     * 
     * DOC ggu Comment method "retrieveDefaultContext".
     */
    private void retrieveDefaultContext() {
        listContext.clear();
        // set default context
        IContext context = new JobContext(IContext.DEFAULT);
        listContext.add(context);
        setDefaultContext(context);

    }

    /**
     * 
     * ggu Comment method "getLostParameters".
     * 
     * @return
     */
    public Set<String> getLostParameters() {
        return this.lostParameters;
    }

    public Map<ContextItem, Map<String, String>> getRepositoryRenamedMap() {
        return this.repositoryRenamedMap;
    }

    public void setRepositoryRenamedMap(Map<ContextItem, Map<String, String>> repositoryRenamedMap) {
        this.repositoryRenamedMap.clear();
        if (repositoryRenamedMap == null) {
            return;
        }
        for (ContextItem item : repositoryRenamedMap.keySet()) {
            Map<String, String> map = repositoryRenamedMap.get(item);
            Map<String, String> tmpMap = new HashMap<String, String>();
            for (String newName : map.keySet()) {
                tmpMap.put(newName, map.get(newName));
            }
            this.repositoryRenamedMap.put(item, tmpMap);
        }

    }

    public void addNewParameters(String param) {
        newParameters.add(param);
    }

    public Set<String> getNewParameters() {
        return newParameters;
    }

    public void setNewParameters(Set<String> newParameters) {
        this.newParameters = newParameters;
    }

    /**
     * The newly added parameter is renamed.
     * 
     * @param newName
     * @param oldName
     */
    private void updateNewParameters(String newName, String oldName) {
        if (newParameters.remove(oldName)) {
            newParameters.add(newName);
        }
    }

    public Map<ContextItem, Set<String>> getNewParametersMap() {
        return newParametersMap;
    }

    public void setNewParametersMap(Map<ContextItem, Set<String>> newParametersMap) {
        this.newParametersMap = newParametersMap;
    }

    /**
     * 
     * ggu Comment method "isOriginalParameter".
     * 
     * (bug 4988)
     */
    public boolean isOriginalParameter(String name) {
        if (name != null) {
            String oldName = nameMap.get(name);
            if (oldName != null) { // renamed
                return originalParamerters.contains(oldName);
            } else {
                return originalParamerters.contains(name);
            }
        }
        return false;
    }
}
