/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.providermanagement.api.impl;

import org.openmrs.Provider;
import org.openmrs.ProviderAttribute;
import org.openmrs.ProviderAttributeType;
import org.openmrs.api.context.Context;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.api.APIException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.module.providermanagement.ProviderManagementConstants;
import org.openmrs.module.providermanagement.ProviderRole;
import org.openmrs.module.providermanagement.api.ProviderManagementService;
import org.openmrs.module.providermanagement.api.db.ProviderManagementDAO;

import java.util.List;

/**
 * It is a default implementation of {@link ProviderManagementService}.
 */
public class ProviderManagementServiceImpl extends BaseOpenmrsService implements ProviderManagementService {
	
	protected final Log log = LogFactory.getLog(this.getClass());
	
	private ProviderManagementDAO dao;
    
    private ProviderAttributeType providerRoleAttributeType = null;
	
	/**
     * @param dao the dao to set
     */
    public void setDao(ProviderManagementDAO dao) {
	    this.dao = dao;
    }

    /**
     * @return the dao
     */
    public ProviderManagementDAO getDao() {
	    return dao;
    }

    @Override
    public List<ProviderRole> getAllProviderRoles() {
        return dao.getAllProviderRoles(false);
    }

    @Override
    public List<ProviderRole> getAllProviderRoles(boolean includeRetired) {
        return dao.getAllProviderRoles(includeRetired);
    }

    @Override
    public ProviderRole getProviderRole(Integer id) {
        return dao.getProviderRole(id);
    }

    @Override
    public ProviderRole getProviderRoleByUuid(String uuid) {
        return dao.getProviderRoleByUuid(uuid);
    }

    @Override
    public void saveProviderRole(ProviderRole role) {
        dao.saveProviderRole(role);
    }

    @Override
    public void retireProviderRole(ProviderRole role, String reason) {
        // BaseRetireHandler handles retiring the object
        dao.saveProviderRole(role);
    }

    @Override
    public void unretireProviderRole(ProviderRole role) {
        // BaseUnretireHandler handles unretiring the object
        dao.saveProviderRole(role);
    }

    @Override
    public void purgeProviderRole(ProviderRole role) {
        dao.deleteProviderRole(role);
    }

    @Override
    public void setProviderRole(Provider provider, ProviderRole role) {
        // TODO: make sure this syncs properly!

        // initialize the providerRoleAttributeType if need be
        if (providerRoleAttributeType == null) {
            initializeProviderRoleAttributeType();
        }

        if (provider == null) {
            throw new APIException("Cannot set provider role: provider is null");
        }
        else {
            // first, void the existing provider role for this provider (if it existing)
            List<ProviderAttribute> attrs = provider.getActiveAttributes(providerRoleAttributeType);
            if (attrs.size() > 1) {
                throw new APIException("Provider should never have more than one Provider Role");
            }
            else if (attrs.size() == 1) {
                ProviderAttribute roleAttributeToVoid = attrs.get(0);
                roleAttributeToVoid.setVoided(true);
                roleAttributeToVoid.setVoidedBy(Context.getAuthenticatedUser());
                roleAttributeToVoid.setVoidReason("voided while setting a new provider role");
            }

            // now create the new attribute
            ProviderAttribute providerRoleAttribute = new ProviderAttribute();
            providerRoleAttribute.setAttributeType(providerRoleAttributeType);
            providerRoleAttribute.setValue(role);
            provider.setAttribute(providerRoleAttribute);

            // save the provider
            Context.getProviderService().saveProvider(provider);
        }
    }
    
    @Override
    public ProviderRole getProviderRole(Provider provider) {
        // initialize the providerRoleAttributeType if need be
        if (providerRoleAttributeType == null) {
            initializeProviderRoleAttributeType();
        }

        List<ProviderAttribute> attrs = provider.getActiveAttributes(providerRoleAttributeType);

        if (attrs.size() > 1) {
            throw new APIException("Provider should never have more than one Provider Role");
        }
        else {
            return (ProviderRole) attrs.get(0).getValue();
        }
    }

    private void initializeProviderRoleAttributeType() {
        // TODO: error handling?
        providerRoleAttributeType = Context.getProviderService().getProviderAttributeTypeByUuid(ProviderManagementConstants.PROVIDER_ROLE_ATTRIBUTE_TYPE_UUID);
    }
}