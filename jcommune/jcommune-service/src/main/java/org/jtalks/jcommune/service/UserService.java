/**
 * Copyright (C) 2011  jtalks.org Team
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 * Also add information on how to contact you by electronic and paper mail.
 * Creation date: Apr 12, 2011 / 8:05:19 PM
 * The jtalks.org Project
 */
package org.jtalks.jcommune.service;

import org.jtalks.jcommune.model.entity.User;
import org.jtalks.jcommune.service.exceptions.DuplicateEmailException;
import org.jtalks.jcommune.service.exceptions.DuplicateException;
import org.jtalks.jcommune.service.exceptions.DuplicateUserException;
import org.jtalks.jcommune.service.exceptions.NotFoundException;

/**
 * This interface should have methods which give us more abilities in manipulating User persistent entity.
 *
 * @author Osadchuck Eugeny
 * @author Kirill Afonin
 */
public interface UserService extends EntityService<User> {
    /**
     * Get {@link User} by username.
     *
     * @param username username of User
     * @return {@link User} with given username
     * @throws NotFoundException if user not found
     * @see User
     */
    User getByUsername(String username) throws NotFoundException;

    /**
     * Try to register {@link User} with given features.
     *
     * @param user user for register
     * @return registered {@link User}
     * @throws DuplicateException if user with username or email already exist
     * @see User
     */
    User registerUser(User user) throws DuplicateUserException,DuplicateEmailException;

    /**
     * Updates user last login time to current time.
     *
     * @param user user which must be updated
     * @see User
     */
    void updateLastLoginTime(User user);
}
