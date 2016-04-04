/*
 *  [2012] - [2016] Codenvy, S.A.
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */
package com.codenvy.api.dao.mongo;

import com.codenvy.api.event.user.RemoveAccountEvent;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoException;

import org.eclipse.che.api.account.server.dao.Account;
import org.eclipse.che.api.account.server.dao.AccountDao;
import org.eclipse.che.api.account.server.dao.Member;
import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.model.workspace.Workspace;
import org.eclipse.che.api.core.notification.EventService;
import org.eclipse.che.api.workspace.server.spi.WorkspaceDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import static com.codenvy.api.dao.mongo.MongoUtil.asDBList;
import static com.codenvy.api.dao.mongo.MongoUtil.asMap;
import static com.codenvy.api.dao.mongo.MongoUtil.asStringList;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

/**
 * Implementation of {@link AccountDao} based on MongoDB storage.
 * <pre>
 *  Account members collection document schema:
 *
 * {
 *     "_id" : "userId...",
 *     "members" : [
 *          ...
 *          {
 *              "userId" : "userId...",
 *              "accountId" : "accountId...",
 *              "roles" : [
 *                  "role1...",
 *                  "role2..."
 *              ]
 *          }
 *          ...
 *     ]
 * }
 *
 * Account collection document schema:
 *
 * {
 *      "id" : "accountId...",
 *      "name" : "name...",
 *      "attributes" : [
 *          ...
 *          {
 *              "name" : "key...",
 *              "value" : "value..."
 *          }
 *          ...
 *      ],
 *      "workspaces" : [ "workspace123", "workspace234" ]
 * }
 *
 *
 * </pre>
 *
 * @author Max Shaposhnik
 * @author Eugene Voevodin
 * @author Alexander Garagatyi
 */
@Singleton
public class AccountDaoImpl implements AccountDao {

    private static final Logger LOG                = LoggerFactory.getLogger(AccountDaoImpl.class);
    private static final String ACCOUNT_COLLECTION = "organization.storage.db.account.collection";
    private static final String MEMBER_COLLECTION  = "organization.storage.db.acc.member.collection";

    private final DBCollection accountCollection;
    private final DBCollection memberCollection;
    private final WorkspaceDao workspaceDao;
    private final EventService eventService;

    @Inject
    public AccountDaoImpl(@Named("mongo.db.organization") DB db,
                          WorkspaceDao workspaceDao,
                          @Named(ACCOUNT_COLLECTION) String accountCollectionName,
                          @Named(MEMBER_COLLECTION) String memberCollectionName,
                          EventService eventService) {
        this.eventService = eventService;
        this.workspaceDao = workspaceDao;
        accountCollection = db.getCollection(accountCollectionName);
        accountCollection.createIndex(new BasicDBObject("id", 1), new BasicDBObject("unique", true));
        accountCollection.createIndex(new BasicDBObject("name", 1));
        accountCollection.createIndex(new BasicDBObject("attributes.name", 1).append("attributes.value", 1));
        accountCollection.createIndex(new BasicDBObject("workspaces", 1), new BasicDBObject("unique", true));
        memberCollection = db.getCollection(memberCollectionName);
        memberCollection.createIndex(new BasicDBObject("members.accountId", 1));
    }

    @Override
    public void create(Account account) throws ConflictException, ServerException {
        try {
            accountCollection.save(toDBObject(account));
        } catch (MongoException me) {
            LOG.error(me.getMessage(), me);
            throw new ServerException("It is not possible to create account");
        }
    }

    @Override
    public Account getById(String id) throws NotFoundException, ServerException {
        final DBObject accountDocument;
        try {
            accountDocument = accountCollection.findOne(new BasicDBObject("id", id));
        } catch (MongoException me) {
            LOG.error(me.getMessage(), me);
            throw new ServerException("It is not possible to retrieve account");
        }
        if (accountDocument == null) {
            throw new NotFoundException(format("Account with id %s was not found", id));
        }
        return toAccount(accountDocument);
    }

    @Override
    public Account getByName(String name) throws NotFoundException, ServerException {
        final DBObject accountDocument;
        try {
            accountDocument = accountCollection.findOne(new BasicDBObject("name", name));
        } catch (MongoException me) {
            LOG.error(me.getMessage(), me);
            throw new ServerException("It is not possible to retrieve account");
        }
        if (accountDocument == null) {
            throw new NotFoundException(format("Account with name %s was not found", name));
        }
        return toAccount(accountDocument);
    }

    @Override
    public List<Account> getByOwner(String owner) throws ServerException, NotFoundException {
        final List<Account> accounts = new LinkedList<>();
        try {
            final DBObject membersDocument = memberCollection.findOne(owner);
            if (membersDocument != null) {
                final BasicDBList members = (BasicDBList)membersDocument.get("members");
                for (Object memberObj : members) {
                    final Member member = toMember(memberObj);
                    if (member.getRoles().contains("account/owner")) {
                        accounts.add(getById(member.getAccountId()));
                    }
                }
            }
        } catch (MongoException me) {
            LOG.error(me.getMessage(), me);
            throw new ServerException("It is not possible to retrieve accounts");
        }
        return accounts;
    }

    @Override
    public void update(Account account) throws NotFoundException, ServerException {
        final DBObject query = new BasicDBObject("id", account.getId());
        try {
            checkAccountExists(account.getId());
            accountCollection.update(query, toDBObject(account));
        } catch (MongoException me) {
            LOG.error(me.getMessage(), me);
            throw new ServerException("It is not possible to update account");
        }
    }

    @Override
    public void remove(String id) throws ConflictException, NotFoundException, ServerException {
        //check account doesn't have associated workspaces
        if (!getById(id).getWorkspaces().isEmpty()) {
            throw new ConflictException("Impossible to remove account with associated workspaces");
        }
        try {
            //Removing members
            for (Member member : getMembers(id)) {
                removeMember(member);
            }
            // Removing account itself
            accountCollection.remove(new BasicDBObject("id", id));
            eventService.publish(new RemoveAccountEvent(id));
        } catch (MongoException me) {
            LOG.error(me.getMessage(), me);
            throw new ServerException("It is not possible to remove account");
        }
    }

    @Override
    public List<Member> getMembers(String accountId) throws ServerException {
        final List<Member> result = new ArrayList<>();
        try (DBCursor membersCursor = memberCollection.find(new BasicDBObject("members.accountId", accountId))) {
            for (DBObject memberDocument : membersCursor) {
                final BasicDBList members = (BasicDBList)memberDocument.get("members");
                result.add(retrieveMember(accountId, members));
            }
        } catch (MongoException me) {
            LOG.error(me.getMessage(), me);
            throw new ServerException("It is not possible to retrieve account members");
        }
        return result;
    }

    @Override
    public List<Member> getByMember(String userId) throws NotFoundException, ServerException {
        final List<Member> result = new ArrayList<>();
        try {
            final DBObject membersDocument = memberCollection.findOne(userId);
            if (membersDocument != null) {
                final BasicDBList members = (BasicDBList)membersDocument.get("members");
                for (Object memberObj : members) {
                    result.add(toMember(memberObj));
                }
            }
        } catch (MongoException me) {
            LOG.error(me.getMessage(), me);
            throw new ServerException("It is not possible to retrieve members");
        }
        return result;
    }

    @Override
    public Account getByWorkspace(String workspaceId) throws NotFoundException, ServerException {
        DBObject document = accountCollection.findOne(new BasicDBObject("workspaces", workspaceId));
        if (document == null) {
            throw new NotFoundException("Account with workspace '" + workspaceId + "' was not found");
        }
        return toAccount(document);
    }

    @Override
    public void addMember(Member member) throws NotFoundException, ConflictException, ServerException {
        try {
            checkAccountExists(member.getAccountId());
            final DBObject membersDocument = documentFor(member);
            final BasicDBList members = (BasicDBList)membersDocument.get("members");
            checkMemberIsAbsent(member, members);
            //member doesn't exist so we can create and save it
            members.add(toDBObject(member));
            memberCollection.save(membersDocument);
        } catch (MongoException me) {
            LOG.error(me.getMessage(), me);
            throw new ServerException("It is not possible to persist member");
        }
    }

    @Override
    public void removeMember(Member member) throws NotFoundException, ServerException, ConflictException {
        final DBObject query = new BasicDBObject("_id", member.getUserId());
        try {
            final DBObject membersDocument = memberCollection.findOne(query);
            if (membersDocument == null) {
                throw new NotFoundException(format("User %s doesn't have account memberships", member.getUserId()));
            }
            final BasicDBList members = (BasicDBList)membersDocument.get("members");
            //remove member from members list
            if (!remove(member.getAccountId(), members)) {
                throw new NotFoundException(format("Membership between %s and %s not found", member.getUserId(), member.getAccountId()));
            }
            //if user doesn't have memberships then remove document
            if (!members.isEmpty()) {
                memberCollection.update(query, membersDocument);
            } else {
                memberCollection.remove(query);
            }
        } catch (MongoException me) {
            LOG.error(me.getMessage(), me);
            throw new ServerException("It is not possible to remove member");
        }
    }

    private boolean remove(String accountId, BasicDBList src) {
        boolean found = false;
        final Iterator it = src.iterator();
        while (!found && it.hasNext()) {
            final Member member = toMember(it.next());
            if (member.getAccountId().equals(accountId)) {
                found = true;
                it.remove();
            }
        }
        return found;
    }

    private Member retrieveMember(String accountId, BasicDBList src) {
        for (Object dbMember : src) {
            final Member member = toMember(dbMember);
            if (accountId.equals(member.getAccountId())) {
                return member;
            }
        }
        return null;
    }

    private DBObject documentFor(Member member) {
        DBObject membersDocument = memberCollection.findOne(new BasicDBObject("_id", member.getUserId()));
        if (membersDocument == null) {
            membersDocument = new BasicDBObject("_id", member.getUserId());
            membersDocument.put("members", new BasicDBList());
        }
        return membersDocument;
    }

    private void checkAccountExists(String id) throws NotFoundException {
        if (accountCollection.findOne(new BasicDBObject("id", id)) == null) {
            throw new NotFoundException(format("Account with id %s was not found", id));
        }
    }

    private void checkMemberIsAbsent(Member target, BasicDBList members) throws ConflictException {
        for (Object dbMember : members) {
            final Member member = toMember(dbMember);
            if (target.getAccountId().equals(member.getAccountId())) {
                throw new ConflictException(format("Account %s already contains member %s", target.getAccountId(), target.getUserId()));
            }
        }
    }

    /**
     * Converts member to database ready-to-use object
     */
    DBObject toDBObject(Member member) {
        final BasicDBList dbRoles = new BasicDBList();
        dbRoles.addAll(member.getRoles());
        return new BasicDBObject().append("userId", member.getUserId())
                                  .append("accountId", member.getAccountId())
                                  .append("roles", dbRoles);
    }

    /**
     * Converts database object to account ready-to-use object
     */
    Account toAccount(Object dbObject) throws NotFoundException, ServerException {
        BasicDBObject accountObject = (BasicDBObject)dbObject;
        List<String> workspaceIds;
        if (accountObject.get("workspaces") == null) {
            workspaceIds = Collections.emptyList();
        } else {
            workspaceIds = asStringList(accountObject.get("workspaces"));
        }
        List<Workspace> workspaces = new ArrayList<>(workspaceIds.size());
        for (String workspaceId : workspaceIds) {
            workspaces.add(workspaceDao.get(workspaceId));
        }
        return new Account(accountObject.getString("id"),
                           accountObject.getString("name"),
                           workspaces,
                           asMap(accountObject.get("attributes")));
    }

    /**
     * Converts database object to member read-to-use object
     */
    Member toMember(Object object) {
        final BasicDBObject basicMemberObj = (BasicDBObject)object;
        final BasicDBList basicRoles = (BasicDBList)basicMemberObj.get("roles");
        final List<String> roles = new ArrayList<>(basicRoles.size());
        for (Object role : basicRoles) {
            roles.add(role.toString());
        }
        return new Member().withAccountId(basicMemberObj.getString("accountId"))
                           .withUserId(basicMemberObj.getString("userId"))
                           .withRoles(roles);
    }

    /**
     * Converts account to database ready-to-use object
     */
    DBObject toDBObject(Account account) {
        return new BasicDBObject().append("id", account.getId())
                                  .append("name", account.getName())
                                  .append("workspaces", asDBList(account.getWorkspaces()
                                                                        .stream()
                                                                        .map(Workspace::getId)
                                                                        .collect(toList())))
                                  .append("attributes", asDBList(account.getAttributes()));
    }
}
