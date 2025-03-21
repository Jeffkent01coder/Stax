/*
 * Copyright 2022 Stax
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hover.stax.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Builds the user object used to create a user on Stax
 */
@Serializable
data class UserUploadDto(
    @SerialName("stax_user")
    val staxUser: UploadDto
)

@Serializable
data class UploadDto(
    @SerialName("device_id")
    val deviceId: String,
    @SerialName("email")
    val email: String?,
    @SerialName("username")
    val username: String?,
    @SerialName("token")
    val token: String
)