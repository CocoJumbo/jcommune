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
package org.jtalks.jcommune.model.dao.hibernate;

import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.joda.time.DateTime;
import org.jtalks.jcommune.model.PersistedObjectsFactory;
import org.jtalks.jcommune.model.dao.LastReadPostDao;
import org.jtalks.jcommune.model.entity.JCUser;
import org.jtalks.jcommune.model.entity.LastReadPost;
import org.jtalks.jcommune.model.entity.Topic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng
        .AbstractTransactionalTestNGSpringContextTests;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.*;

import static org.testng.Assert.*;

/**
 * @author Evgeniy Naumenko
 * @author Anuar Nurmakanov
 */
@ContextConfiguration(locations = {"classpath:/org/jtalks/jcommune/model/entity/applicationContext-dao.xml"})
@TransactionConfiguration(transactionManager = "transactionManager", defaultRollback = true)
@Transactional
public class LastReadPostHibernateDaoTest extends AbstractTransactionalTestNGSpringContextTests {
    @Autowired
    private LastReadPostDao lastReadPostDao;
    @Autowired
    private SessionFactory sessionFactory;
    private Session session;

    @BeforeMethod
    public void setUp() {
        session = sessionFactory.getCurrentSession();
        PersistedObjectsFactory.setSession(session);
    }

    /*===== Common methods =====*/
    @Test
    public void testGet() {
        LastReadPost expected = PersistedObjectsFactory.getDefaultLastReadPost();
        session.save(expected);

        LastReadPost actual = lastReadPostDao.get(expected.getId());

        assertNotNull(actual, "Get returns null.");
        assertEquals(actual.getId(), expected.getId(),
                "Get return incorrect object");
    }

    @Test
    public void dateOfTheLastReadPostShouldBeUpdated() {
        LastReadPost post = PersistedObjectsFactory.getDefaultLastReadPost();
        post.setPostDate(new DateTime());
        session.save(post);
        DateTime newPostDate = post.getPostDate().plusMinutes(34);
        post.setPostDate(newPostDate);

        lastReadPostDao.saveOrUpdate(post);
        LastReadPost updatedPost = (LastReadPost) session.get(LastReadPost.class, post.getId());

        assertEquals(updatedPost.getPostDate(), newPostDate,
                "Update doesn't work, because field value didn't change.");
    }

    @Test
    public void testMarkAsReadTopicsToUser() {
        List<Topic> topics = PersistedObjectsFactory.createAndSaveTopicListWithPosts(10);
        JCUser user = PersistedObjectsFactory.getDefaultUser();

        //records of posts in topics
        Map<Long, Integer> listCountPostsToTopics = markAllTopicsASRead(topics, user);
        //records in database about read topic
        Map<Long, Integer> actualCountPostsToTopics = getActualListCountPostsToTopics(topics, user);

        assertEquals(actualCountPostsToTopics, listCountPostsToTopics);
    }

    @Test
    public void testDeleteMarksTopicsToUser() {
        List<Topic> topics = PersistedObjectsFactory.createAndSaveTopicListWithPosts(10);
        JCUser user = PersistedObjectsFactory.getDefaultUser();
        SQLQuery deletedEntities = (SQLQuery) session.getNamedQuery("deleteAllMarksReadToUser");
        deletedEntities
                .addSynchronizedEntityClass(LastReadPost.class)
                .setParameter("user", user.getId())
                .setParameter("branch", topics.get(0).getBranch().getId())
                .setCacheable(false)
                .executeUpdate();
        SQLQuery checkDelete = (SQLQuery) session.createSQLQuery("select TOPIC_ID, " +
                "LAST_READ_POST_INDEX FROM LAST_READ_POSTS where TOPIC_ID IN (select TOPIC_ID from TOPIC where " +
                "BRANCH_ID=:branch) and USER_ID = :user");
        checkDelete.setParameter("user", user.getId());
        checkDelete.setParameter("branch", topics.get(0).getBranch().getId());
        //check delete record about read posts for user
        assertTrue(checkDelete.list().isEmpty());
    }

    @Test
    public void testGetTopicAndLatestPostDateInBranch() {
        List<Topic> topics = PersistedObjectsFactory.createAndSaveTopicListWithPosts(10);
        //records of posts in topics
        Map<Long, DateTime> actualCountOfPosts = getTopicAndLatestPostDateInBranch(topics);
        Map<Long, DateTime> resultOfGetTopics = new HashMap<Long, DateTime>();
        @SuppressWarnings("unchecked")
        List<Object[]> resultCheckGetTopics = session.getNamedQuery("getTopicAndLatestPostDateInBranch")
                .setParameter("branch", topics.get(0).getBranch().getId())
                .setCacheable(false)
                .list();

        for (Object[] record : resultCheckGetTopics) {
            resultOfGetTopics.put(new Long(record[0].toString()), (DateTime)record[1]);
        }

        assertEquals(resultOfGetTopics, actualCountOfPosts);
    }

    @Test
    public void testMarkAllReadToUserInTwoBranches() {
        JCUser user = PersistedObjectsFactory.getDefaultUser();
        List<Topic> topicsOfFirstBranch = PersistedObjectsFactory.createAndSaveTopicListWithPosts(10);
        List<Topic> topicsOfSecondBranch = PersistedObjectsFactory.createAndSaveTopicListWithPosts(10);
        //records of posts in topics
        Map<Long, Integer> listCountPostsToTopicsInFBranch = new HashMap<Long, Integer>();
        Map<Long, Integer> listCountPostsToTopicsInSBranch = new HashMap<Long, Integer>();
        //records in database about read topic
        Map<Long, Integer> actualCountPostsToTopicsInFBranch = new HashMap<Long, Integer>();
        Map<Long, Integer> actualCountPostsToTopicsInSBranch = new HashMap<Long, Integer>();

        listCountPostsToTopicsInFBranch = markAllTopicsASRead(topicsOfFirstBranch, user);
        listCountPostsToTopicsInSBranch = markAllTopicsASRead(topicsOfSecondBranch, user);
        actualCountPostsToTopicsInFBranch = getActualListCountPostsToTopics(topicsOfFirstBranch, user);
        actualCountPostsToTopicsInSBranch = getActualListCountPostsToTopics(topicsOfSecondBranch, user);

        //concatenate  results from first and second branches
        listCountPostsToTopicsInFBranch.putAll(listCountPostsToTopicsInSBranch);
        actualCountPostsToTopicsInFBranch.putAll(actualCountPostsToTopicsInSBranch);

        assertEquals(listCountPostsToTopicsInFBranch, actualCountPostsToTopicsInFBranch);
    }

    /*===== Specific methods =====*/
    @Test
    public void testListLastReadPostsForTopic() {
        LastReadPost post = PersistedObjectsFactory.getDefaultLastReadPost();
        session.save(post);

        List<LastReadPost> lastReadPosts = lastReadPostDao.getLastReadPostsInTopic(post.getTopic());

        assertTrue(lastReadPosts.size() == 1, "Result list has incorrect size");
        assertEquals(lastReadPosts.get(0).getId(), post.getId(),
                "Results contains invalid data.");
    }

    @Test
    public void testGetLastReadPost() {
        LastReadPost expected = PersistedObjectsFactory.getDefaultLastReadPost();
        session.save(expected);

        LastReadPost actual = lastReadPostDao.getLastReadPost(expected.getUser(), expected.getTopic());

        assertEquals(actual.getId(), expected.getId(),
                "Found incorrect last read post.");
    }
    
    @Test
    public void getLastReadPostsForUserInTopicsShouldReturnThem() {
        int topicsSize = 10;
        JCUser user = PersistedObjectsFactory.getDefaultUser();
        List<Topic> userTopics = PersistedObjectsFactory.createAndSaveTopicListWithPosts(topicsSize);
        markAllTopicsASRead(userTopics, user);
        
        List<LastReadPost> lastReadPosts = lastReadPostDao.getLastReadPosts(user, userTopics);
        
        assertEquals(lastReadPosts.size(), topicsSize, 
                "For every passed topic it should return last read post.");
        
    }
    
    @Test
    public void getLastReadPostsForUserShouldReturnEmptyListForEmptyListOfTopics() {
        List<Topic> userTopics = Collections.emptyList();
        JCUser user = new JCUser("user", "user@gmail.com", "password");
        
        List<LastReadPost> lastReadPosts = lastReadPostDao.getLastReadPosts(user, userTopics);
        
        assertTrue(lastReadPosts.isEmpty(), "For passed empty list of topics it should return empty list.");
        
    }
    
    @Test
    public void deleteLastReadPostsShouldDeleteAllRecodrsForGivenUser() {
        List<Topic> topics = PersistedObjectsFactory.createAndSaveTopicListWithPosts(10);
        JCUser user = PersistedObjectsFactory.getDefaultUser();
        markAllTopicsASRead(topics, user);
        
        lastReadPostDao.deleteLastReadPostsFor(user);
    
        @SuppressWarnings("unchecked")
        List<LastReadPost> lastReadPostsOfUser = session.getNamedQuery("getAllOfUser")
            .setParameter("user", user).list();
        assertTrue(lastReadPostsOfUser.isEmpty(), "User shouldn't have any records, because they were cleared");
        
    }

    /**
     * Method marks topics as read to user
     *
     * @param topics List of topics to mark
     * @param user   User for which threads are marked as read
     * @return list of count posts for each topic, for verification
     */
    private Map<Long, Integer> markAllTopicsASRead(List<Topic> topics, JCUser user) {
        SQLQuery insertQuery = (SQLQuery) session.getNamedQuery("markAllTopicsRead");
        insertQuery.setCacheable(false);
        Map<Long, Integer> listCountPostsToTopics = new HashMap<Long, Integer>();

        for (Topic tp : topics) {
            Integer indexReadPosts = tp.getPosts().size() - 1;
            insertQuery.setParameter("uuid", UUID.randomUUID().toString())
                    .setParameter("user", user.getId())
                    .setParameter("lastPostIndex", indexReadPosts)
                    .setParameter("lastPostDate", ((DateTime)tp.getLastPost().getCreationDate()).toDate())
                    .setParameter("topic", tp.getId())
                    .executeUpdate();
            listCountPostsToTopics.put(tp.getId(), indexReadPosts);
        }
        return listCountPostsToTopics;
    }

    /**
     * Method returns the data read topics that are stored in the database
     *
     * @param topics List of topics, which marked as read to user
     * @param user   User for which threads are marked as read
     * @return List of count posts for each topic, for verification, stored in the database
     */
    private Map<Long, Integer> getActualListCountPostsToTopics(List<Topic> topics, JCUser user) {
        Map<Long, Integer> listCountPostsToTopics = new HashMap<Long, Integer>();

        SQLQuery checkInsert = (SQLQuery) session.createSQLQuery("select TOPIC_ID, " +
                "LAST_READ_POST_INDEX FROM LAST_READ_POSTS where TOPIC_ID IN (select TOPIC_ID from TOPIC where " +
                "BRANCH_ID=:branch) and USER_ID = :user");
        checkInsert.setParameter("user", user.getId());
        checkInsert.setParameter("branch", topics.get(0).getBranch().getId());
        @SuppressWarnings("unchecked")
        List<Object[]> resultCheckInsert = checkInsert.list();

        for (Object[] record : resultCheckInsert) {
            listCountPostsToTopics.put(new Long(record[0].toString()), new Integer(record[1].toString()));
        }
        return listCountPostsToTopics;
    }

    /**
     * Method returns the data for each topics, which marked as read
     *
     * @param topics List of topics in branch
     * @return List of count posts for each topic
     */
    private Map<Long, DateTime> getTopicAndLatestPostDateInBranch(List<Topic> topics) {
        Map<Long, DateTime> result = new HashMap<Long, DateTime>();
        for (Topic topic : topics) {
            //second parameter it's index of last post
            result.put(topic.getId(), topic.getLastPost().getCreationDate());
        }
        return result;
    }
}
