/*
 * Copyright 2018-present MongoDB, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mongodb.stitch.server.services.http.internal;

import com.mongodb.stitch.core.services.http.CoreHttpServiceClient;
import com.mongodb.stitch.core.services.http.HttpRequest;
import com.mongodb.stitch.core.services.http.HttpResponse;
import com.mongodb.stitch.server.core.services.StitchService;
import com.mongodb.stitch.server.services.http.HttpServiceClient;

import javax.annotation.Nonnull;

public final class HttpServiceClientImpl extends CoreHttpServiceClient
    implements HttpServiceClient {

  public HttpServiceClientImpl(final StitchService service) {
    super(service);
  }

  /**
   * Executes the given {@link HttpRequest}.
   *
   * @param request The request to execute.
   * @return The response to executing the request.
   */
  public HttpResponse execute(@Nonnull final HttpRequest request) {
    return executeInternal(request);
  }
}
