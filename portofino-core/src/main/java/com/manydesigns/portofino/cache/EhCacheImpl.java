/*
* Copyright (C) 2005-2012 ManyDesigns srl.  All rights reserved.
* http://www.manydesigns.com/
*
* Unless you have purchased a commercial license agreement from ManyDesigns srl,
* the following license terms apply:
*
* This program is free software; you can redistribute it and/or modify
* it under the terms of the GNU General Public License version 3 as published by
* the Free Software Foundation.
*
* There are special exceptions to the terms and conditions of the GPL
* as it is applied to this software. View the full text of the
* exception in file OPEN-SOURCE-LICENSE.txt in the directory of this
* software distribution.
*
* This program is distributed WITHOUT ANY WARRANTY; and without the
* implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
* See the GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program; if not, see http://www.gnu.org/licenses/gpl.txt
* or write to:
* Free Software Foundation, Inc.,
* 59 Temple Place - Suite 330,
* Boston, MA  02111-1307  USA
*
*/

package com.manydesigns.portofino.cache;

import com.manydesigns.portofino.application.Application;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;

import java.io.Serializable;

/**
 * @author Paolo Predonzani     - paolo.predonzani@manydesigns.com
 * @author Angelo Lupo          - angelo.lupo@manydesigns.com
 * @author Giampiero Granatella - giampiero.granatella@manydesigns.com
 * @author Alessio Stalla       - alessio.stalla@manydesigns.com
 */
public class EhCacheImpl implements Cache {
    public static final String copyright =
            "Copyright (c) 2005-2012, ManyDesigns srl";

    protected final CacheManager cacheManager;

    public EhCacheImpl() {
        cacheManager = CacheManager.newInstance();
    }

    public <T extends Serializable> T get(Application application, Serializable key) {
        Ehcache cache = cacheManager.addCacheIfAbsent(application.getAppId());
        Element element = cache.get(key);
        return element != null ? (T) element.getValue() : null;
    }

    public void put(Application application, Serializable key, Serializable value) {
        Ehcache cache = cacheManager.addCacheIfAbsent(application.getAppId());
        cache.put(new Element(key, value));
    }

    public void shutdown() {
        cacheManager.shutdown();
    }

}