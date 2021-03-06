/**
 * Copyright (c) 2007-2014 Encore Research Group, University of Toronto
 *
 * This software is distributed under the GNU General Public License, v3,
 * or (at your option) any later version.
 * 
 * Permission is hereby granted, without written agreement and without license
 * or royalty fees, to use, copy, modify, and distribute this software and its
 * documentation for any purpose, provided that the above copyright notice and
 * the following two paragraphs appear in all copies of this software.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Public License for more details.
 *
 * You should have received a copy of the GNU Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package org.wise.portal.domain.authentication.impl;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.Version;

import org.wise.portal.domain.authentication.MutableGrantedAuthority;

/**
 * Implementation class of <code>MutableGrantedAuthority</code> that uses an
 * EJB3 compliant object persistence mechanism.
 * 
 * @author Cynick Young
 * 
 * @version $Id$
 * 
 */
@Entity
@Table(name = PersistentGrantedAuthority.DATA_STORE_NAME)
public class PersistentGrantedAuthority implements MutableGrantedAuthority {

    @Transient
    public static final String DATA_STORE_NAME = "granted_authorities";

    @Transient
    public static final String COLUMN_NAME_ROLE = "authority";

    @Transient
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Version
    @Column(name = "OPTLOCK")
    private Integer version;

    @Column(name = PersistentGrantedAuthority.COLUMN_NAME_ROLE, unique = true, nullable = false)
    private String authority;

    /**
     * Default Constructor
     * @param role
     */
    public PersistentGrantedAuthority() {
    }
    
    /**
     * Constructor
     * @param role
     */
    public PersistentGrantedAuthority(String role) {
        this.authority = role;
    }
    
    /**
     * @see net.sf.sail.webapp.domain.authentication.MutableGrantedAuthority#setAuthority(java.lang.String)
     */
    public void setAuthority(String role) {
        this.authority = role;
    }

    /**
     * @see org.acegisecurity.GrantedAuthority#getAuthority()
     */
    public String getAuthority() {
        return this.authority;
    }

    /**
     * @see net.sf.sail.webapp.domain.Persistable#getId()
     */
    public Long getId() {
        return id;
    }

    @SuppressWarnings("unused")
    private void setId(Long id) {
        this.id = id;
    }

    @SuppressWarnings("unused")
    private Integer getVersion() {
        return version;
    }

    @SuppressWarnings("unused")
    private void setVersion(Integer version) {
        this.version = version;
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result
                + ((this.authority == null) ? 0 : this.authority.hashCode());
        return result;
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final PersistentGrantedAuthority other = (PersistentGrantedAuthority) obj;
        if (this.authority == null) {
            if (other.authority != null)
                return false;
        } else if (!this.authority.equals(other.authority))
            return false;
        return true;
    }
}