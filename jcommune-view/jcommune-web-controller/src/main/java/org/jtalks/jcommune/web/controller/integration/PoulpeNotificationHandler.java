/**
 * Copyright (C) 2011  JTalks.org Team
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
 */

package org.jtalks.jcommune.web.controller.integration;

import org.jtalks.common.model.entity.User;
import org.jtalks.jcommune.service.BranchService;
import org.jtalks.jcommune.service.SectionService;
import org.jtalks.jcommune.service.UserService;
import org.jtalks.jcommune.service.exceptions.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.json.MappingJacksonJsonView;

import static com.google.common.base.Preconditions.checkArgument;
import static org.apache.commons.lang.StringUtils.isNotBlank;

/**
 * Controller that handles notifications from Poulpe. Main purpose of this is to support data consistency, e.g. when
 * deleting branch Poulpe can't take care of topics, proper post count, notifications and so on. That is why messages
 * are sent here.
 *
 * @author Vyacheslav Mishcheryakov
 * @author Evgeniy Naumenko
 */
@Controller
public class PoulpeNotificationHandler {
    private static final String ERROR_MESSAGE_PARAMETER = "errorMessage";

    private final BranchService branchService;
    private final SectionService sectionService;
    private final UserService userService;

    @Autowired
    public PoulpeNotificationHandler(BranchService branchService, SectionService sectionService, UserService userService) {
        this.branchService = branchService;
        this.sectionService = sectionService;
        this.userService = userService;
    }

    /**
     * Handles notification about branch deletion. This method deletes all topics in branch. Branch itself is not
     * deleted as Poulpe can cope with it
     *
     * @param branchId branch id
     * @throws NotFoundException is thrown if branch not found
     */
    @RequestMapping(value = "/branches/{branchId}", method = RequestMethod.DELETE)
    @ResponseBody
    public void deleteBranch(@PathVariable("branchId") long branchId,
                             @RequestParam(value = "password") String adminPassword) throws NotFoundException {
        assertAdminPasswordCorrect(adminPassword);
        branchService.deleteAllTopics(branchId);
    }

    /**
     *
     * @param adminPassword
     * @throws NotFoundException
     */
    private void assertAdminPasswordCorrect(String adminPassword) throws NotFoundException {
        checkArgument(isNotBlank(adminPassword), "No password specified while it is required");
        User admin = userService.getCommonUserByUsername("admin");
        checkArgument(adminPassword.equals(admin.getPassword()),
                "Wrong password was specified during removal of branch/section/component.");
    }

    /**
     * Handles notification about section deletion. Removes all topics from section. Sections and branches won't be
     * removed
     *
     * @param sectionId section id
     * @throws NotFoundException is thrown if section not found
     */
    @RequestMapping(value = "/sections/{sectionId}", method = RequestMethod.DELETE)
    @ResponseBody
    public void deleteSection(@PathVariable("sectionId") long sectionId) throws NotFoundException {
        sectionService.deleteAllTopicsInSection(sectionId);
    }

    /**
     * Handles notification about component deletion. As for now it removes all the topics, leaving branches, sections
     * and components untouched.
     *
     * @throws org.jtalks.jcommune.service.exceptions.NotFoundException
     *          if object for deletion has not been found
     */
    @RequestMapping(value = "/component", method = RequestMethod.DELETE)
    @ResponseBody
    public void deleteComponent() throws NotFoundException {
        sectionService.deleteAllTopicsInForum();
    }

    /**
     * Catches all exceptions threw by any method in this controller and returns HTTP status 500 with error message in
     * response body instead.
     *
     * @param exception exception that was thrown
     * @return {@link ModelAndView} object with JSON view and error message as model
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ModelAndView handleAllExceptions(Exception exception) {
        MappingJacksonJsonView jsonView = new MappingJacksonJsonView();
        ModelAndView mav = new ModelAndView(jsonView);
        mav.addObject(ERROR_MESSAGE_PARAMETER, exception.getMessage());
        return mav;
    }

}
