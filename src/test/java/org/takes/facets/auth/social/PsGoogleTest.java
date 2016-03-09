/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Yegor Bugayenko
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.takes.facets.auth.social;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import javax.json.Json;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.takes.Request;
import org.takes.Response;
import org.takes.Take;
import org.takes.facets.auth.Identity;
import org.takes.facets.fork.FkRegex;
import org.takes.facets.fork.TkFork;
import org.takes.http.FtRemote;
import org.takes.rq.RqFake;
import org.takes.rq.RqForm;
import org.takes.rq.RqGreedy;
import org.takes.rq.RqHref;
import org.takes.rq.RqPrint;
import org.takes.rs.RsJSON;

/**
 * Test case for {@link PsGoogle}.
 *
 * <p>The class is immutable and thread-safe.
 * @author Dmitry Zaytsev (dmitry.zaytsev@gmail.com)
 * @version $Id$
 * @since 0.16.3
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.TooManyMethods")
public final class PsGoogleTest {

    /**
     * Code parameter.
     */
    private static final String CODE = "code";
    /**
     * Google token parameter.
     */
    private static final String GOOGLE_TOKEN = "GoogleToken";
    /**
     * Avatar google url.
     */
    private static final String AVATAR = "https://google.com/img/avatar.gif";
    /**
     * XPath access_token string.
     */
    private static final String ACCESS_TOKEN = "access_token";
    /**
     * Account url.
     */
    private static final String ACCOUNT = "http://localhost/account";
    /**
     * Key request parameter.
     */
    private static final String KEY = "key";
    /**
     * App request parameter.
     */
    private static final String APP = "app";
    /**
     * PsGoogle login.
     * @checkstyle MultipleStringLiteralsCheck (100 lines)
     * @throws Exception If some problem inside
     */
    @Test
    public void logsIn() throws Exception {
        final Take take = new TkFork(
            this.requestToken(),
            new FkRegex(
                "/plus/v1/people/me",
                // @checkstyle AnonInnerLengthCheck (1 line)
                new Take() {
                    @Override
                    public Response act(final Request req) throws IOException {
                        MatcherAssert.assertThat(
                            new RqPrint(req).printHead(),
                            Matchers.containsString(
                                "GET /plus/v1/people/me"
                            )
                        );
                        MatcherAssert.assertThat(
                            new RqHref.Base(req).href()
                                .param(PsGoogleTest.ACCESS_TOKEN)
                                .iterator().next(),
                            Matchers.containsString(PsGoogleTest.GOOGLE_TOKEN)
                        );
                        return new RsJSON(
                            Json.createObjectBuilder()
                                .add("displayName", "octocat")
                                .add("id", "1")
                                .add(
                                    "image",
                                    Json.createObjectBuilder()
                                        .add(
                                            "url",
                                            PsGoogleTest.AVATAR
                                        )
                                )
                                .build()
                        );
                    }
                }
            )
        );
        new FtRemote(take).exec(
            // @checkstyle AnonInnerLengthCheck (100 lines)
            new FtRemote.Script() {
                @Override
                public void exec(final URI home) throws IOException {
                    final Identity identity = new PsGoogle(
                        PsGoogleTest.APP,
                        PsGoogleTest.KEY,
                        PsGoogleTest.ACCOUNT,
                        home.toString(),
                        home.toString()
                    ).enter(new RqFake("GET", "?code=code")).get();
                    MatcherAssert.assertThat(
                        identity.urn(),
                        Matchers.equalTo("urn:google:1")
                    );
                    MatcherAssert.assertThat(
                        identity.properties().get("name"),
                        Matchers.equalTo("octocat")
                    );
                    MatcherAssert.assertThat(
                        identity.properties().get("picture"),
                        Matchers.equalTo(PsGoogleTest.AVATAR)
                    );
                }
            }
        );
    }
    /**
     * PsGoogle login with fail due a bad response from google.
     * @checkstyle MultipleStringLiteralsCheck (100 lines)
     * @throws Exception If some problem inside
     */
    @Test(expected = IOException.class)
    public void badGoogleResponse() throws Exception {
        final Take take = new TkFork(
            this.requestToken(),
            new FkRegex(
                "/plus/v1/people/me",
                // @checkstyle AnonInnerLengthCheck (1 line)
                new Take() {
                    @Override
                    public Response act(final Request req) throws IOException {
                        MatcherAssert.assertThat(
                            new RqPrint(req).printHead(),
                            Matchers.containsString(
                                "GET /plus/v1/people/me"
                            )
                        );
                        MatcherAssert.assertThat(
                            new RqHref.Base(req).href()
                                .param(PsGoogleTest.ACCESS_TOKEN)
                                .iterator().next(),
                            Matchers.containsString(PsGoogleTest.GOOGLE_TOKEN)
                        );
                        return createErrorJson();
                    }
                }
            )
        );
        new FtRemote(take).exec(
            // @checkstyle AnonInnerLengthCheck (100 lines)
            new FtRemote.Script() {
                @Override
                public void exec(final URI home) throws IOException {
                    new PsGoogle(
                        PsGoogleTest.APP,
                        PsGoogleTest.KEY,
                        PsGoogleTest.ACCOUNT,
                        home.toString(),
                        home.toString()
                    ).enter(new RqFake("GET", "?code=code")).get();
                }
            }
        );
    }
    /**
     * Test a google response without the displayName property.
     * @checkstyle MultipleStringLiteralsCheck (100 lines)
     * @throws Exception If some problem inside
     */
    @Test
    public void noDisplayNameResponse() throws Exception {
        final Take take = new TkFork(
            this.requestToken(),
            new FkRegex(
                "/plus/v1/people/me",
                // @checkstyle AnonInnerLengthCheck (1 line)
                new Take() {
                    @Override
                    public Response act(final Request req) throws IOException {
                        MatcherAssert.assertThat(
                            new RqPrint(req).printHead(),
                            Matchers.containsString(
                                "GET /plus/v1/people/me"
                            )
                        );
                        MatcherAssert.assertThat(
                            new RqHref.Base(req).href()
                                .param(PsGoogleTest.ACCESS_TOKEN)
                                .iterator().next(),
                            Matchers.containsString(PsGoogleTest.GOOGLE_TOKEN)
                        );
                        return new RsJSON(
                            Json.createObjectBuilder()
                                .add("id", "1")
                                .add(
                                    "image",
                                    Json.createObjectBuilder()
                                        .add(
                                            "url",
                                            PsGoogleTest.AVATAR
                                        )
                                )
                                .build()
                        );
                    }
                }
            )
        );
        new FtRemote(take).exec(
            // @checkstyle AnonInnerLengthCheck (100 lines)
            new FtRemote.Script() {
                @Override
                public void exec(final URI home) throws IOException {
                    final Identity identity = new PsGoogle(
                        PsGoogleTest.APP,
                        PsGoogleTest.KEY,
                        PsGoogleTest.ACCOUNT,
                        home.toString(),
                        home.toString()
                    ).enter(new RqFake("GET", "?code=code")).get();
                    MatcherAssert.assertThat(
                        identity.urn(),
                        Matchers.equalTo("urn:google:1")
                    );
                    MatcherAssert.assertThat(
                        identity.properties().get("name"),
                        Matchers.equalTo("unknown")
                    );
                    MatcherAssert.assertThat(
                        identity.properties().get("picture"),
                        Matchers.equalTo(PsGoogleTest.AVATAR)
                    );
                }
            }
        );
    }
    /**
     * Build a token request fork.
     * @return Returns the token request
     */
    private FkRegex requestToken() {
        return new FkRegex(
            "/o/oauth2/token",
            // @checkstyle AnonInnerLengthCheck (1 line)
            new Take() {
                @Override
                public Response act(final Request req) throws IOException {
                    MatcherAssert.assertThat(
                        new RqPrint(req).printHead(),
                        Matchers.containsString("POST /o/oauth2/token")
                    );
                    final Request greq = new RqGreedy(req);
                    PsGoogleTest.assertParam(
                        greq,
                        "client_id",
                        PsGoogleTest.APP
                    );
                    PsGoogleTest.assertParam(
                        greq,
                        "redirect_uri",
                        PsGoogleTest.ACCOUNT
                    );
                    PsGoogleTest.assertParam(
                        greq,
                        "client_secret",
                        PsGoogleTest.KEY
                    );
                    PsGoogleTest.assertParam(
                        greq,
                        "grant_type",
                        "authorization_code"
                    );
                    PsGoogleTest.assertParam(
                        greq,
                        PsGoogleTest.CODE,
                        PsGoogleTest.CODE
                    );
                    return new RsJSON(
                        Json.createObjectBuilder()
                            .add(
                                PsGoogleTest.ACCESS_TOKEN,
                                PsGoogleTest.GOOGLE_TOKEN
                             )
                            .add("expires_in", 1)
                            .add("token_type", "Bearer")
                            .build()
                    );
                }
            }
        );
    }

    /**
     * Checks the parameter value for the expected value.
     * @param req Request
     * @param param Parameter name
     * @param value Parameter value
     * @throws IOException If some problem inside
     */
    private static void assertParam(final Request req,
        final CharSequence param, final String value) throws IOException {
        MatcherAssert.assertThat(
            new RqForm.Smart(new RqForm.Base(req)).single(param),
            Matchers.equalTo(value)
        );
    }
    /**
     * Construct a error response with google json syntax for errors.
     * @return Json with error.
     * @throws IOException If some problem inside
     */
    private static RsJSON createErrorJson() throws IOException {
        final String message = "message";
        return new RsJSON(
            Json.createObjectBuilder()
                .add(
                    "error",
                    Json.createObjectBuilder()
                        .add(
                            "errors",
                            Json.createArrayBuilder()
                                .add(
                                    Json.createObjectBuilder()
                                        .add("domain", "usageLimits")
                                        .add("reason", "accessNotConfigured")
                                        .add(
                                            "extendedHelp",
                                            "https://developers.google.com"
                                        )
                                )
                            )
                   )
                  .add(PsGoogleTest.CODE, HttpURLConnection.HTTP_BAD_REQUEST)
                  .add(message, "Access Not Configured.")
                  .build()
        );
    }
}
