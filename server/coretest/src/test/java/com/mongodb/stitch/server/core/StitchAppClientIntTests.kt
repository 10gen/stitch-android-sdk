package com.mongodb.stitch.server.core

import com.mongodb.stitch.core.admin.authProviders.ProviderConfigs
import com.mongodb.stitch.core.admin.create
import com.mongodb.stitch.core.admin.functions.FunctionCreator
import com.mongodb.stitch.core.admin.userRegistrations.sendConfirmation
import com.mongodb.stitch.core.auth.UserType
import com.mongodb.stitch.core.auth.providers.anonymous.AnonymousAuthProvider
import com.mongodb.stitch.core.auth.providers.anonymous.AnonymousCredential
import com.mongodb.stitch.core.auth.providers.custom.CustomAuthProvider
import com.mongodb.stitch.core.auth.providers.custom.CustomCredential
import com.mongodb.stitch.core.auth.providers.userpassword.UserPasswordAuthProvider
import com.mongodb.stitch.core.auth.providers.userpassword.UserPasswordCredential
import com.mongodb.stitch.core.internal.common.MemoryStorage
import com.mongodb.stitch.server.core.auth.providers.userpassword.UserPasswordAuthProviderClient
import com.mongodb.stitch.server.testutils.BaseStitchServerIntTest
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import org.bson.Document
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Arrays
import java.util.Calendar
import java.util.Date

class StitchAppClientIntTests : BaseStitchServerIntTest() {

    @Test
    fun testCustomAuthLogin() {
        val app = createApp()
        val signingKey = "abcdefghijklmnopqrstuvwxyz1234567890"
        addProvider(app.second, ProviderConfigs.Custom(signingKey))

        val client = getAppClient(app.first)
        val jwt = Jwts.builder()
                .setHeader(
                        mapOf(
                                "alg" to "H   S256",
                                "typ" to "JWT"
                        ))
                .claim("stitch_meta",
                        mapOf(
                                "email" to "name@example.com",
                                "name" to "Joe Bloggs",
                                "picture" to "https://goo.gl/xqR6Jd"
                        ))
                .setIssuedAt(Date())
                .setNotBefore(Date())
                .setAudience(app.first.clientAppId)
                .setSubject("uniqueUserID")
                .setExpiration(Date(((Calendar.getInstance().timeInMillis + (5 * 60 * 1000)))))
                .signWith(SignatureAlgorithm.HS256, signingKey.toByteArray())
                .compact()

        val user = client.auth.loginWithCredential(CustomCredential(jwt))
        assertNotNull(user)
        assertTrue(user.id.isNotEmpty())
        assertEquals(CustomAuthProvider.DEFAULT_NAME, user.loggedInProviderName)
        assertEquals(CustomAuthProvider.TYPE, user.loggedInProviderType)
        assertEquals(UserType.NORMAL, user.userType)
        assertTrue(user.identities[0].id.isNotEmpty())
        assertEquals(CustomAuthProvider.TYPE, user.identities[0].providerType)
        assertTrue(client.auth.isLoggedIn)
    }

    @Test
    fun testMultipleLoginSemantics() {
        val app = createApp()
        addProvider(app.second, ProviderConfigs.Anon)
        addProvider(app.second, config = ProviderConfigs.Userpass(
                emailConfirmationUrl = "http://emailConfirmURL.com",
                resetPasswordUrl = "http://resetPasswordURL.com",
                confirmEmailSubject = "email subject",
                resetPasswordSubject = "password subject")
        )

        val storage = MemoryStorage()
        var client = getAppClient(app.first, storage)

        // check storage
        assertFalse(client.auth.isLoggedIn)
        assertNull(client.auth.user)

        // login anonymously
        val anonUser = client.auth.loginWithCredential(AnonymousCredential())
        assertNotNull(anonUser)

        // check storage
        assertTrue(client.auth.isLoggedIn)
        assertEquals(anonUser.loggedInProviderType, AnonymousAuthProvider.TYPE)
        assertNotNull(client.auth.user!!.lastAuthActivity)

        // login anonymously again and make sure user ID is the same
        assertEquals(anonUser.id, client.auth.loginWithCredential(AnonymousCredential()).id)

        // check storage
        assertTrue(client.auth.isLoggedIn)
        assertEquals(client.auth.user!!.loggedInProviderType, AnonymousAuthProvider.TYPE)

        // login with email provider and make sure user ID is updated
        val emailUserId = registerAndLoginWithUserPass(app.second, client, "test@10gen.com", "hunter1")
        assertNotSame(emailUserId, anonUser.id)

        // check storage
        assertTrue(client.auth.isLoggedIn)
        assertEquals(client.auth.user!!.loggedInProviderType, UserPasswordAuthProvider.TYPE)

        // login with email provider under different user and make sure user ID is updated
        val id2 = registerAndLoginWithUserPass(app.second, client, "test2@10gen.com", "hunter2")
        assertNotSame(emailUserId, id2)

        // check storage
        assertTrue(client.auth.isLoggedIn)
        assertEquals(client.auth.user!!.loggedInProviderType, UserPasswordAuthProvider.TYPE)

        // verify that logout clears storage
        client.auth.logout()
        assertFalse(client.auth.isLoggedIn)
        assertNull(client.auth.user)

        // log back into the last user
        client.auth.loginWithCredential(UserPasswordCredential("test2@10gen.com", "hunter2"))

        assertTrue(client.auth.isLoggedIn)
        assertEquals(client.auth.user!!.loggedInProviderType, UserPasswordAuthProvider.TYPE)
        assertEquals(client.auth.user?.id, id2)

        // verify orderingcore
        assertEquals(client.auth.listUsers().size, 3)
        assertEquals(client.auth.listUsers()[0].id, anonUser.id)
        assertEquals(client.auth.listUsers()[1].id, emailUserId)
        assertEquals(client.auth.listUsers()[2].id, id2)

        // imitate an app restart
        Stitch.clearApps()

        // check everything is as it was
        client = getAppClient(app.first, storage)
        assertTrue(client.auth.isLoggedIn)
        assertEquals(client.auth.user!!.loggedInProviderType, UserPasswordAuthProvider.TYPE)
        assertEquals(client.auth.user?.id, id2)
        assertNotNull(client.auth.user!!.lastAuthActivity)


        // verify ordering is preserved
        assertEquals(client.auth.listUsers().size, 3)
        assertEquals(client.auth.listUsers()[0].id, anonUser.id)
        assertEquals(client.auth.listUsers()[1].id, emailUserId)
        assertEquals(client.auth.listUsers()[2].id, id2)

        // verify that removing the user with id2 also logs out the active user
        client.auth.logoutUserWithId(id2)
        // Assert that we're no longer logged in
        assertFalse(client.auth.isLoggedIn)

        // and assert you can remove a user even if you're not logged in
        client.auth.removeUserWithId(id2)

        assertEquals(client.auth.listUsers().size, 2)

        // switch to the user with emailUserId and verify that is the user switched to
        client.auth.switchToUserWithId(emailUserId)

        assertEquals(client.auth.user!!.loggedInProviderType, UserPasswordAuthProvider.TYPE)
        assertEquals(client.auth.user?.id, emailUserId)

        assertEquals(client.auth.listUsers().size, 2)
        assertNotNull(client.auth.listUsers().firstOrNull { it.id == emailUserId })
        assertNull(client.auth.listUsers().firstOrNull { it.id == id2 })
        assertNotNull(client.auth.listUsers().firstOrNull { it.id == anonUser.id })

        // imitate an app restart
        Stitch.clearApps()
        client = getAppClient(app.first, storage)

        // Assert that we're still logged in
        assertTrue(client.auth.isLoggedIn)
        // Assert that the next user is up
        assertEquals(client.auth.user!!.loggedInProviderType, UserPasswordAuthProvider.TYPE)
        assertEquals(client.auth.user?.id, emailUserId)

        assertEquals(client.auth.listUsers().size, 2)
        assertNotNull(client.auth.listUsers().firstOrNull { it.id == emailUserId })
        assertNull(client.auth.listUsers().firstOrNull { it.id == id2 })
        assertNotNull(client.auth.listUsers().firstOrNull { it.id == anonUser.id })

        // assert that removing the active user just leaves the anon user
        client.auth.removeUser()

        client.auth.switchToUserWithId(anonUser.id)

        // Assert that the next user is up
        assertEquals(client.auth.user!!.loggedInProviderType, AnonymousAuthProvider.TYPE)
        assertEquals(client.auth.user?.id, anonUser.id)

        assertEquals(client.auth.listUsers().size, 1)
        assertNull(client.auth.listUsers().firstOrNull { it.id == emailUserId })
        assertNull(client.auth.listUsers().firstOrNull { it.id == id2 })
        assertNotNull(client.auth.listUsers().firstOrNull { it.id == anonUser.id })

        // assert that logging out of the anonymous user removes it as well
        client.auth.logout()

        assertFalse(client.auth.isLoggedIn)
        assertEquals(client.auth.listUsers().size, 0)
        assertNull(client.auth.user)
    }

    @Test
    fun testIdentityLinking() {
        val app = createApp()
        addProvider(app.second, ProviderConfigs.Anon)
        addProvider(app.second, config = ProviderConfigs.Userpass(
                emailConfirmationUrl = "http://emailConfirmURL.com",
                resetPasswordUrl = "http://resetPasswordURL.com",
                confirmEmailSubject = "email subject",
                resetPasswordSubject = "password subject")
        )

        val client = getAppClient(app.first)
        val userPassClient = client.auth.getProviderClient(UserPasswordAuthProviderClient.factory)

        val email = "user@10gen.com"
        val password = "password"
        userPassClient.registerWithEmail(email, password)

        val conf = app.second.userRegistrations.sendConfirmation(email)
        userPassClient.confirmUser(conf.token, conf.tokenId)

        val anonUser = client.auth.loginWithCredential(
                AnonymousCredential()
        )
        assertNotNull(anonUser)
        assertEquals(anonUser.loggedInProviderType, AnonymousAuthProvider.TYPE)

        val linkedUser = anonUser.linkWithCredential(UserPasswordCredential(email, password))

        assertEquals(anonUser.id, linkedUser.id)
        assertEquals(linkedUser.loggedInProviderType, UserPasswordAuthProvider.TYPE)

        assertEquals(client.auth.user!!.identities.size, 2)

        client.auth.logout()
        assertFalse(client.auth.isLoggedIn)

        // assert that there is one user in the list, and that it did not get
        // deleted when logging out because the linked user is no longer anon
        assertEquals(client.auth.listUsers().size, 1)
        assertEquals(client.auth.listUsers()[0].id, linkedUser.id)
    }

    @Test
    fun testCallFunction() {
        val app = createApp()
        addProvider(app.second, ProviderConfigs.Anon)
        val client = getAppClient(app.first)

        app.second.functions.create(FunctionCreator(
                "testFunction",
                "exports = function(intArg, stringArg) { " +
                        "return { intValue: intArg, stringValue: stringArg} " +
                        "}",
                null,
                false)
        )

        client.auth.loginWithCredential(AnonymousCredential())

        val resultDoc = client.callFunction(
                "testFunction", Arrays.asList(42, "hello"), Document::class.java
        )

        assertTrue(resultDoc.containsKey("intValue"))
        assertTrue(resultDoc.containsKey("stringValue"))
        assertEquals(42, resultDoc.getInteger("intValue"))
        assertEquals("hello", resultDoc.getString("stringValue"))
    }
}
