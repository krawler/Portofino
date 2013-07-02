/*
 * Copyright (C) 2005-2013 ManyDesigns srl.  All rights reserved.
 * http://www.manydesigns.com/
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package com.manydesigns.portofino.shiro;

import com.manydesigns.portofino.application.AppProperties;
import com.manydesigns.portofino.application.Application;
import com.manydesigns.portofino.shiro.openid.OpenIDToken;
import org.apache.commons.configuration.Configuration;
import org.apache.shiro.authc.*;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.subject.PrincipalCollection;
import org.openid4java.consumer.VerificationResult;
import org.openid4java.discovery.Identifier;

import java.util.*;

/**
 * Default implementation of ApplicationRealmDelegate. Provides convenient implementations of the interface methods.
 *
 * @author Paolo Predonzani     - paolo.predonzani@manydesigns.com
 * @author Angelo Lupo          - angelo.lupo@manydesigns.com
 * @author Giampiero Granatella - giampiero.granatella@manydesigns.com
 * @author Alessio Stalla       - alessio.stalla@manydesigns.com
 */
public abstract class AbstractApplicationRealmDelegate implements ApplicationRealmDelegate {
    public static final String copyright =
            "Copyright (c) 2005-2013, ManyDesigns srl";

    /**
     * Handles the typical login with a username and a password. This method is called by the default implementation
     * of getAuthenticationInfo when the authentication token is a UsernamePasswordToken.
     * @param realm the ApplicationRealm that is delegating to this object.
     * @param userName the username.
     * @param password the password.
     * @return
     */
    protected abstract AuthenticationInfo getAuthenticationInfo(
            ApplicationRealm realm, String userName, String password);

    /**
     * Performs authentication. This method knows how to handle typical authentication tokens, validating them
     * and then handling them to the appropriate getAuthenticationInfo overload.<br />
     * Handling of unknown token types is delegated to doGetAuthenticationToken.
     *
     * @param realm the ApplicationRealm that is calling this method.
     * @param token the authentication token. Typical examples are a UsernamePasswordToken or a OpenIDToken.
     * @return
     */
    public AuthenticationInfo getAuthenticationInfo(ApplicationRealm realm, AuthenticationToken token) {
        if(token instanceof UsernamePasswordToken) {
            UsernamePasswordToken upToken = (UsernamePasswordToken) token;

            if(upToken.getUsername() == null || upToken.getPassword() == null) {
                throw new IncorrectCredentialsException("Username or password is null");
            }

            String username = upToken.getUsername();
            String password = new String(upToken.getPassword());
            return getAuthenticationInfo(realm, username, password);
        } else if(token instanceof OpenIDToken) {
            if(realm.isOpenIDEnabled()) {
                return getAuthenticationInfo(realm, ((OpenIDToken) token).getPrincipal());
            } else {
                throw new UnsupportedOperationException("OpenID authentication not supported");
            }
        } else if(token instanceof ServletContainerToken) {
            return getAuthenticationInfo(realm, (ServletContainerToken) token);
        } else {
            return doGetAuthenticationInfo(realm, token);
        }
    }

    /**
     * {@inheritDoc}
     * <p>This default implementation handles built-in groups (all, anonymous, registered, etc.), delegating
     * to loadAuthorizationInfo methods the actual loading of application-specific groups.</p>
     *
     * @param realm the ApplicationRealm that is calling this method.
     * @param principal
     * @return
     */
    public AuthorizationInfo getAuthorizationInfo(ApplicationRealm realm, Object principal) {
        Application application = realm.getApplication();
        Set<String> groups = new HashSet<String>();
        Configuration conf = application.getConfiguration();
        groups.add(conf.getString(AppProperties.GROUP_ALL));
        if (principal == null) {
            groups.add(conf.getString(AppProperties.GROUP_ANONYMOUS));
        } else if(principal instanceof PrincipalCollection) {
            groups.add(conf.getString(AppProperties.GROUP_REGISTERED));
            groups.addAll(loadAuthorizationInfo(realm, (PrincipalCollection) principal));
        } else if(principal instanceof String) {
            groups.add(conf.getString(AppProperties.GROUP_REGISTERED));
            groups.addAll(loadAuthorizationInfo(realm, (String) principal));
        } else if(realm.isOpenIDEnabled() && (principal instanceof Identifier)) {
            groups.add(conf.getString(AppProperties.GROUP_EXTERNALLY_AUTHENTICATED));
            groups.addAll(loadAuthorizationInfo(realm, (Identifier) principal));
        } else {
            groups.addAll(loadAuthorizationInfo(realm, principal));
        }

        SimpleAuthorizationInfo info = new SimpleAuthorizationInfo(groups);
        if(groups.contains(getAdministratorsGroup(realm))) {
            info.addStringPermission("*");
        }
        Permission permission = new GroupPermission(groups);
        info.setObjectPermissions(Collections.singleton(permission));
        return info;
    }

    /**
     * Loads the groups associated to a PrincipalCollection. This might include, besides the username, other
     * principals like an internal user ID.
     * @param realm
     * @param principalCollection
     * @return
     */
    protected Collection<String> loadAuthorizationInfo(
            ApplicationRealm realm, PrincipalCollection principalCollection) {
        return Collections.emptySet();
    }

    /**
     * Loads the groups associated to a given username.
     * @param realm
     * @param principal
     * @return
     */
    protected Collection<String> loadAuthorizationInfo(ApplicationRealm realm, String principal) {
        return Collections.emptySet();
    }

    /**
     * Loads the groups associated to an OpenID identifier.
     * @param realm
     * @param principal
     * @return
     */
    protected Collection<String> loadAuthorizationInfo(ApplicationRealm realm, Identifier principal) {
        return Collections.emptySet();
    }

    /**
     * Loads the groups associated to a principal of an unknown type. This implementation throws an
     * AuthorizationException, but you can override it to handle your custom principal type.
     * @param realm
     * @param principal
     * @return
     */
    protected Collection<String> loadAuthorizationInfo(ApplicationRealm realm, Object principal) {
        throw new AuthorizationException("Invalid principal: " + principal);
    }

    /**
     * Returns the name of the administrators group as defined in app.properties.
     * @param realm
     * @return
     */
    protected String getAdministratorsGroup(ApplicationRealm realm) {
        return realm.getApplication().getConfiguration().getString(AppProperties.GROUP_ADMINISTRATORS);
    }

    /**
     * Returns the AuthenticationInfo for a user who logged in with OpenID. You can override this method if you want
     * to associate OpenID users with internal users with more information than just a identifier URL.
     * @param realm
     * @param principal
     * @return
     */
    protected AuthenticationInfo getAuthenticationInfo(ApplicationRealm realm, VerificationResult principal) {
        return new SimpleAuthenticationInfo(
                principal.getVerifiedId(), OpenIDToken.NO_CREDENTIALS, realm.getName());
    }

    /**
     * Returns the AuthenticationInfo for a user who was logged in by the container. You can override this method if you
     * want to associate the user with more information than just a username.
     * @param realm
     * @param token
     * @return
     */
    protected AuthenticationInfo getAuthenticationInfo(ApplicationRealm realm, ServletContainerToken token) {
        return new SimpleAuthenticationInfo(token.getPrincipal().getName(), token.getCredentials(), realm.getName());
    }

    /**
     * Authenticates a user with a token of unknown type. This method throws an AuthenticationException, but
     * you can override it to handle your custom tokens.
     * @param realm
     * @param token
     * @return
     */
    protected AuthenticationInfo doGetAuthenticationInfo(ApplicationRealm realm, AuthenticationToken token) {
        throw new AuthenticationException("Authentication token " + token + " not supported");
    }

    /**
     * {@inheritDoc}
     * <p>This default implementation returns the built-in groups
     * (all, anonymous, registered, administrators, externally authenticated).
     * You can override it to add custom groups for your application.</p>
     * @param realm the ApplicationRealm that is calling this method.
     * @return
     */
    public Set<String> getGroups(ApplicationRealm realm) {
        Application application = realm.getApplication();
        Set<String> groups = new LinkedHashSet<String>();
        Configuration conf = application.getConfiguration();
        String group = conf.getString(AppProperties.GROUP_ALL);
        groups.add(group);
        group = conf.getString(AppProperties.GROUP_ANONYMOUS);
        groups.add(group);
        group = conf.getString(AppProperties.GROUP_REGISTERED);
        groups.add(group);
        group = conf.getString(AppProperties.GROUP_ADMINISTRATORS);
        groups.add(group);
        if(realm.isOpenIDEnabled()) {
            groups.add(conf.getString(AppProperties.GROUP_EXTERNALLY_AUTHENTICATED));
        }
        return groups;
    }

    @Override
    public void verifyUser(ApplicationRealm realm, Object user) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void approveUser(ApplicationRealm realm, Object user) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void rejectUser(ApplicationRealm realm, Object user) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void lockUser(ApplicationRealm realm, Object user) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void unlockUser(ApplicationRealm realm, Object user) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void validateToken(ApplicationRealm realm, Object user, AuthenticationToken token) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void changePassword(ApplicationRealm realm, Object user, String newPassword) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String generateOneTimeToken(ApplicationRealm realm, Object user) {
        throw new UnsupportedOperationException();
    }
}
