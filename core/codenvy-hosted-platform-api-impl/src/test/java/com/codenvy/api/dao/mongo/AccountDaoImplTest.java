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
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

import org.eclipse.che.api.account.server.dao.Account;
import org.eclipse.che.api.account.server.dao.Member;
import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.notification.EventService;
import org.eclipse.che.api.workspace.server.model.impl.WorkspaceImpl;
import org.eclipse.che.api.workspace.server.spi.WorkspaceDao;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

/**
 * Tests for {@link AccountDaoImpl}
 *
 * @author Max Shaposhnik
 * @author Eugene Voevodin
 */
@Listeners(value = {MockitoTestNGListener.class})
public class AccountDaoImplTest extends BaseDaoTest {
    private static final String ACC_COLL_NAME    = "accounts";
    private static final String MEMBER_COLL_NAME = "members";

    @Mock
    private WorkspaceDao workspaceDao;
    @Mock
    private EventService eventService;

    private AccountDaoImpl accountDao;
    private DBCollection   membersCollection;

    @BeforeMethod
    public void setUp() throws Exception {
        super.setUp(ACC_COLL_NAME);
        db = spy(db);
        collection = spy(db.getCollection(ACC_COLL_NAME));
        membersCollection = spy(db.getCollection(MEMBER_COLL_NAME));
        when(db.getCollection(ACC_COLL_NAME)).thenReturn(collection);
        when(db.getCollection(MEMBER_COLL_NAME)).thenReturn(membersCollection);
        accountDao = new AccountDaoImpl(db,
                                        workspaceDao,
                                        ACC_COLL_NAME,
                                        MEMBER_COLL_NAME,
                                        eventService);
    }

    @Test
    public void shouldBeAbleToCreateAccount() throws Exception {
        final Account account = createAccount();

        accountDao.create(account);

        final DBObject accountDocument = collection.findOne(new BasicDBObject("id", account.getId()));
        assertNotNull(accountDocument);
        assertEquals(accountDao.toAccount(accountDocument), account);
    }

    @Test
    public void shouldBeAbleToGetAccountById() throws Exception {
        final Account account = createAccount();
        insertAccounts(account);

        final Account actual = accountDao.getById(account.getId());

        assertEquals(actual, account);
    }

    @Test
    public void shouldBeAbleToGetAccountByName() throws Exception {
        final Account account = createAccount();
        insertAccounts(account);

        final Account actual = accountDao.getByName(account.getName());

        assertEquals(actual, account);
    }

    @Test(expectedExceptions = NotFoundException.class)
    public void shouldThrowNotFoundExceptionIfAccountWithGivenNameDoesNotExist() throws Exception {
        final Account account = createAccount();
        insertAccounts(account);

        assertNull(accountDao.getByName(account.getName() + "suffix"));
    }

    @Test
    public void shouldBeAbleToGetAccountByOwner() throws Exception {
        final Account account = createAccount();
        final Member member = new Member().withAccountId(account.getId())
                                          .withRoles(asList("account/owner"))
                                          .withUserId("test_user_id");
        insertAccounts(account);
        insertMembers(member);

        final List<Account> result = accountDao.getByOwner(member.getUserId());

        assertEquals(result.size(), 1);
        assertEquals(result.get(0), account);
    }

    @Test
    public void shouldBeAbleToUpdateAccount() throws Exception {
        final Account account = createAccount();
        insertAccounts(account);
        //prepare update
        account.setName(account.getName() + "new_name_suffix");
        WorkspaceImpl workspace2 = mock(WorkspaceImpl.class);
        when(workspace2.getId()).thenReturn("workspace345");
        when(workspaceDao.get(workspace2.getId())).thenReturn(workspace2);
        account.getWorkspaces().add(workspace2);

        accountDao.update(account);

        final DBObject accountDocument = collection.findOne(new BasicDBObject("id", account.getId()));
        assertNotNull(accountDocument);
        assertEquals(accountDao.toAccount(accountDocument), account);
    }

    @Test
    public void shouldBeAbleToRemoveAccount() throws Exception {
        final Account account = createAccount();
        account.setWorkspaces(emptyList());
        final Member member1 = new Member().withUserId("test_user_1")
                                           .withAccountId(account.getId())
                                           .withRoles(asList("account/owner"));
        final Member member2 = new Member().withUserId("test_user_2")
                                           .withAccountId(account.getId())
                                           .withRoles(asList("account/member"));
        insertMembers(member1, member2);
        insertAccounts(account);

        accountDao.remove(account.getId());

        verify(eventService).publish(argThat(new ArgumentMatcher<Object>() {
            @Override
            public boolean matches(Object o) {
                final RemoveAccountEvent accountEvent = (RemoveAccountEvent)o;
                return "test_account_id".equals(accountEvent.getAccountId());
            }
        }));
        assertNull(collection.findOne(new BasicDBObject("id", account.getId())));
        assertFalse(membersCollection.find(new BasicDBObject("members.accountId", account.getId())).hasNext());
    }

    @Test(expectedExceptions = ConflictException.class,
          expectedExceptionsMessageRegExp = "Impossible to remove account with associated workspaces")
    public void shouldNotBeAbleToRemoveAccountWithAssociatedWorkspace() throws Exception {
        final Account account = createAccount();
        insertAccounts(account);

        accountDao.remove(account.getId());
    }

    @Test
    public void shouldBeAbleToAddMember() throws Exception {
        final Account account = createAccount();
        final Member member = new Member().withUserId("test_user_id")
                                          .withAccountId(account.getId())
                                          .withRoles(asList("account/admin", "account/manager"));
        insertAccounts(account);

        accountDao.addMember(member);

        final DBObject membersDocument = membersCollection.findOne(new BasicDBObject("_id", member.getUserId()));
        assertNotNull(membersDocument);
        final BasicDBList actualMembers = (BasicDBList)membersDocument.get("members");
        assertEquals(actualMembers.size(), 1);
        assertEquals(accountDao.toMember(actualMembers.get(0)), member);
    }

    @Test
    public void shouldBeAbleToGetMembers() throws Exception {
        final Account account = createAccount();
        final Member member1 = new Member().withUserId("test_user_id1")
                                           .withAccountId(account.getId())
                                           .withRoles(asList("account/owner"));
        final Member member2 = new Member().withUserId("test_user_id2")
                                           .withAccountId(account.getId())
                                           .withRoles(asList("account/member"));
        insertMembers(member1, member2);

        final List<Member> actualMembers = accountDao.getMembers(account.getId());

        assertEquals(new HashSet<>(actualMembers), new HashSet<>(asList(member1, member2)));
    }

    @Test
    public void shouldBeAbleToRemoveMember() throws Exception {
        final Account account = createAccount();
        final Member member1 = new Member().withUserId("test_user_1")
                                           .withAccountId(account.getId())
                                           .withRoles(asList("account/owner"));
        final Member member2 = new Member().withUserId("test_user_2")
                                           .withAccountId(account.getId())
                                           .withRoles(asList("account/member"));
        insertMembers(member1, member2);

        accountDao.removeMember(member2);

        assertNull(membersCollection.findOne(new BasicDBObject("_id", member2.getUserId())));
        assertNotNull(membersCollection.findOne(new BasicDBObject("_id", member1.getUserId())));
    }

    @Test
    public void shouldBeAbleToGetAccountMembershipsByMember() throws NotFoundException, ServerException {
        final Account account = createAccount();
        final Member member = new Member().withAccountId(account.getId())
                                          .withUserId("test_user_id")
                                          .withRoles(asList("account/owner"));
        insertMembers(member);

        final List<Member> actualMembers = accountDao.getByMember(member.getUserId());

        assertEquals(actualMembers.size(), 1);
        assertEquals(actualMembers.get(0), member);
    }

    @Test
    public void shouldBeAbleToGetAccountByWorkspace() throws Exception {
        Account account = createAccount();
        insertAccounts(account);

        Account result = accountDao.getByWorkspace(account.getWorkspaces().get(0).getId());

        assertEquals(account, result);
    }

    @Test(expectedExceptions = NotFoundException.class,
          expectedExceptionsMessageRegExp = "Account with workspace 'workspace123' was not found")
    public void shouldFailWhenGettingAccountByWorkspaceAndAccountDoesNotExist() throws Exception {
        accountDao.getByWorkspace("workspace123");
    }

    @Test
    public void shouldConvertDBObjectToAccountWithNullWorkspaces() throws Exception {
        BasicDBObject dbObject = new BasicDBObject().append("id", "account123")
                                                    .append("name", "account-name")
                                                    .append("attributes", new BasicDBList());

        Account result = accountDao.toAccount(dbObject);

        assertTrue(result.getWorkspaces().isEmpty());
    }

    private void insertAccounts(Account... accounts) {
        for (Account account : accounts) {
            collection.insert(accountDao.toDBObject(account));
        }
    }

    private void insertMembers(Member... members) {
        final Map<String, BasicDBList> membersMap = new HashMap<>();
        for (Member member : members) {
            if (!membersMap.containsKey(member.getUserId())) {
                membersMap.put(member.getUserId(), new BasicDBList());
            }
            membersMap.get(member.getUserId()).add(accountDao.toDBObject(member));
        }
        for (Map.Entry<String, BasicDBList> entry : membersMap.entrySet()) {
            membersCollection.insert(new BasicDBObject("_id", entry.getKey()).append("members", entry.getValue()));
        }
    }

    private Account createAccount() throws NotFoundException, ServerException {
        final Map<String, String> attributes = new HashMap<>(8);
        attributes.put("attr1", "value1");
        attributes.put("attr2", "value2");
        attributes.put("attr3", "value3");
        WorkspaceImpl workspace = mock(WorkspaceImpl.class);
        when(workspace.getId()).thenReturn("workspace123");
        when(workspaceDao.get("workspace123")).thenReturn(workspace);
        return new Account("test_account_id", "test_account_name", new ArrayList<>(singletonList(workspace)), attributes);
    }
}
