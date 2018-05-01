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

package com.mongodb.stitch.android.core.auth.providers.internal.userpassword;

import android.support.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.mongodb.stitch.core.auth.providers.userpass.UserPasswordCredential;

public interface UserPasswordAuthProviderClient {

  /** Gets a credential for a user from the given username and password. */
  UserPasswordCredential getCredential(
      @NonNull final String username, @NonNull final String password);

  /**
   * Registers a new user with the given email and password.
   *
   * @return A {@link Task} that completes when registration completes/fails.
   */
  Task<Void> registerWithEmail(@NonNull final String email, @NonNull final String password);

  /**
   * Confirms a user with the given token and token id.
   *
   * @return A {@link Task} that completes when confirmation completes/fails.
   */
  Task<Void> confirmUser(@NonNull final String token, @NonNull final String tokenId);

  /**
   * Resend the confirmation for a user to the given email.
   *
   * @return A {@link Task} that completes when the resend request completes/fails.
   */
  Task<Void> resendConfirmationEmail(@NonNull final String email);

  /**
   * Reset the password of a user with the given token, token id, and new password.
   *
   * @return A {@link Task} that completes when the password reset completes/fails.
   */
  Task<Void> resetPassword(
      @NonNull final String token, @NonNull final String tokenId, @NonNull final String password);

  /**
   * Sends a user a password reset email for the given email.
   *
   * @return A {@link Task} that completes when the reqest request completes/fails.
   */
  Task<Void> sendResetPasswordEmail(@NonNull final String email);
}
