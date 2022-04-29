package com.app.transportation.data

import android.content.SharedPreferences
import androidx.core.content.edit
import com.app.transportation.data.api.*
import com.app.transportation.data.database.MainDao
import com.app.transportation.data.database.entities.AdvertisementCategory
import com.app.transportation.data.database.entities.Profile
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named

class Repository(private val dao: MainDao) : KoinComponent {

    private val client: HttpClient by inject()

    private val prefs: SharedPreferences by inject(named("MainSettings"))

    private var authToken: String?
        get() = prefs.getString("authToken", null).takeIf { it != "" }
        set(value) = prefs.edit(true) { putString("authToken", value ?: "") }

    fun profileFlow() = dao.profilesFlow()
        .filterNotNull()
        .filter { it.isNotEmpty() }
        .map { it.firstOrNull() }

    private fun profile() = dao.profile(0)

    suspend fun saveLoginInProfile(login: String) {
        (profile() ?: Profile())
            .let { profile ->
                profile.login = login
                if ('@' in login)
                    profile.email = login
                else
                    profile.telNumber = login
                dao.insert(profile)
            }
    }

    suspend fun editProfile(
        name: String = "",
        telNumber: String = "",
        email: String = "",
        cityArea: String = "",
        paymentCard: String = ""
    ) = kotlin.runCatching {
        val token = authToken ?: return@runCatching
        client.submitForm(
            url = "http://api-transport.mvp-pro.top/api/v1/profile",
            formParameters = Parameters.build {
                append("first_name", name)
                append("phone", telNumber)
                append("email", email)
                append("card", paymentCard)
                append("location", cityArea)
                //append("avatar", name)
                //append("status", name)
            }
        ) {
            headers { append("X-Access-Token", token) }
        }
        //TODO status and avatar
    }

    suspend fun updateProfile(): UpdateProfileResponse = kotlin.runCatching {
        val token = authToken ?: return@runCatching UpdateProfileResponse.Failure("token is null")
        val response: HttpResponse = client.get("http://api-transport.mvp-pro.top/api/v1/profile") {
            headers {
                append("X-Access-Token", token)
            }
        }
        val responseBody: String = response.receive()
        val json = Json.Default
        kotlin.runCatching {
            json.decodeFromString<UpdateProfileResponse.Success>(responseBody)
                .apply {
                    val oldProfile = profile()
                    val profile = Profile(
                        login = oldProfile?.login ?: "",
                        name = firstName,
                        telNumber = phone.takeIf { it.isNotBlank() } ?: oldProfile?.telNumber ?: "",
                        email = email.takeIf { it.isNotBlank() } ?: oldProfile?.email ?: "",
                        paymentCard = card,
                        cityArea = location,
                        specialization = status,
                        avatar = avatar
                    )
                    dao.insert(profile)
                    println("profileResponse = $this")
                }
        }.getOrElse {
            json.decodeFromString<UpdateProfileResponse.Failure>(responseBody)
        }
    }.getOrElse { UpdateProfileResponse.Failure(it.stackTraceToString()) }

    suspend fun logout() = coroutineScope {
        val clearProfileJob = launch { dao.clearProfile() }

        kotlin.runCatching {
            val authToken = authToken ?: return@runCatching
            client.get("http://api-transport.mvp-pro.top/api/v1/logout") {
                headers {
                    append("X-Access-Token", authToken)
                }
            }
        }

        clearProfileJob.join()
        authToken = null
    }

    fun advertCategoriesFlow() = dao.advertCategoriesFlow().map { list -> list.sortedBy { it.id } }

    private suspend fun categoriesRequest(token: String, type: String, id: String = "0"): String =
        client.get<HttpResponse>("http://api-transport.mvp-pro.top/api/v1/datas_category") {
            parameter("type", type)
            parameter("id", id)
            headers {
                append("X-Access-Token", token)
            }
        }.receive()

    suspend fun updateAdvertCategories() = runCatching<Throwable?> {
        val token = authToken ?: return@runCatching AuthTokenNotFoundException()
        val json = Json
        val firstCategoriesJson = categoriesRequest(token, "1", "1")
        val firstLevelCategoriesResponse = kotlin.runCatching {
            json.decodeFromString<AdvertDataResponse.Success>(firstCategoriesJson)
        }.getOrElse {
            json.decodeFromString<AdvertDataResponse.Failure>(firstCategoriesJson)
        }
        (firstLevelCategoriesResponse as? AdvertDataResponse.Success)?.categories?.let { cats ->
            val secondCategoryResponses = mutableListOf<AdvertDataResponse>()
            val thirdCategoryResponses = mutableListOf<AdvertDataResponse>()
            val fourthCategoryResponses = mutableListOf<AdvertDataResponse>()
            cats.keys.forEach { firstCategoryId ->
                val secondCategoriesJson = categoriesRequest(token, "2", firstCategoryId)
                val secondCategory = kotlin.runCatching {
                    json.decodeFromString<AdvertDataResponse.Success>(secondCategoriesJson)
                }.getOrElse {
                    json.decodeFromString<AdvertDataResponse.Failure>(secondCategoriesJson)
                }
                secondCategoryResponses.add(secondCategory)
                (secondCategory as? AdvertDataResponse.Success)?.let { it ->
                    it.categories.keys.forEach { secondCategoryId ->
                        val thirdCategoriesJson = categoriesRequest(token, "3", secondCategoryId)
                        val thirdCategory = kotlin.runCatching {
                            json.decodeFromString<AdvertDataResponse.Success>(thirdCategoriesJson)
                        }.getOrElse {
                            json.decodeFromString<AdvertDataResponse.Failure>(thirdCategoriesJson)
                        }
                        thirdCategoryResponses.add(thirdCategory)
                        //(thirdCategoryResponses as? AdvertDataResponse.Success)?.let {ADR ->
                            //ADR.categories.keys.forEach { thirdCategoryId ->
                        thirdCategoryResponses.forEach { thirdid ->
                            val itSuccess = thirdid as AdvertDataResponse.Success
                            itSuccess.categories.forEach {
                                try {
                                    val fourthCategoriesJson = categoriesRequest(token, "3", it.key)
                                    val fourthCategory = kotlin.runCatching {
                                        json.decodeFromString<AdvertDataResponse.Success>(fourthCategoriesJson)
                                    }.getOrElse {
                                        json.decodeFromString<AdvertDataResponse.Failure>(fourthCategoriesJson)
                                    }
                                    fourthCategoryResponses.add(fourthCategory)
                                }
                                catch (ex : Exception){}
                            }
                        }
                            //}
                        //}
                    }
                }
            }
            val secondLevelCategories = mutableListOf<AdvertisementCategory>()
            secondCategoryResponses.forEach { adr ->
                val itSuccess = adr as AdvertDataResponse.Success
                itSuccess.categories.forEach {
                    secondLevelCategories.add(
                        AdvertisementCategory(
                            id = it.key.toInt(),
                            level = 2,
                            name = it.value.name,
                            parentId = it.value.parentId.toInt()
                        )
                    )
                }
            }
            val thirdLevelCategories = mutableListOf<AdvertisementCategory>()
            thirdCategoryResponses.forEach { adr ->
                val itSuccess = adr as AdvertDataResponse.Success
                itSuccess.categories.forEach {
                    thirdLevelCategories.add(
                        AdvertisementCategory(
                            id = it.key.toInt(),
                            level = 3,
                            name = it.value.name,
                            parentId = it.value.parentId.toInt()
                        )
                    )
                }
            }
            val fourthLevelCategories = mutableListOf<AdvertisementCategory>()
            fourthCategoryResponses.forEach { adr ->
                val itSuccess = adr as AdvertDataResponse.Success
                itSuccess.categories.forEach {
                    fourthLevelCategories.add(
                        AdvertisementCategory(
                            id = it.key.toInt(),
                            level = 4,
                            name = it.value.name,
                            parentId = it.value.parentId.toInt()
                        )
                    )
                }
            }
            val unsortedAllCategories = cats.map {
                AdvertisementCategory(
                    id = it.key.toInt(),
                    level = 1,
                    name = it.value.name,
                    parentId = it.value.parentId.toInt()
                )
            } + secondLevelCategories + thirdLevelCategories + fourthLevelCategories
            val allCategories = unsortedAllCategories.sortedBy { it.id }
            if (allCategories != dao.advertCategoriesFlow().first()) {
                dao.clearAdvertCategories()
                dao.insert(allCategories)
            }
        }
        null
    }.getOrElse { it }

    suspend fun getAdvertCategories(type: String, id: String = "0"): AdvertDataResponse =
        kotlin.runCatching {
            val token = authToken ?: return@runCatching AdvertDataResponse.Failure("token is null")
            val response: HttpResponse =
                client.get("http://api-transport.mvp-pro.top/api/v1/datas_category") {
                    parameter("type", type)
                    parameter("id", id)
                    headers {
                        append("X-Access-Token", token)
                    }
                }
            val responseBody: String = response.receive()
            val json = Json.Default

            kotlin.runCatching {
                json.decodeFromString<AdvertDataResponse.Success>(responseBody)
            }.getOrElse {
                json.decodeFromString<AdvertDataResponse.Failure>(responseBody)
            }
        }.getOrElse { AdvertDataResponse.Failure(it.stackTraceToString()) }

    suspend fun createAdvert(
        title: String,
        price: String,
        description: String,
        categoryId: String,
        photos: List<String>
    ): AdvertCreateResponse = kotlin.runCatching {
        val token = authToken ?: return@runCatching AdvertCreateResponse.Failure("token is null")
        val response: HttpResponse =
            client.submitForm(
                url = "http://api-transport.mvp-pro.top/api/v1/advert_create",
                formParameters = Parameters.build {
                    append("title", title)
                    append("price", price)
                    append("description", description)
                    append("category", categoryId)
                    append("image", "[${photos.joinToString()}]")
                }
            ) {
                headers { append("X-Access-Token", token) }
            }

        val responseBody: String = response.receive()
        val json = Json.Default

        println("responseBody = $responseBody")

        json.decodeFromString<AdvertCreateResponse.Success>(responseBody)
    }.getOrElse { AdvertCreateResponse.Failure(it.stackTraceToString()) }.also { println("it= $it") }

    suspend fun editAdvert(
        title: String,
        price: String,
        description: String,
        categoryId: String,
        photos: List<String>
    ): AdvertCreateResponse = kotlin.runCatching {
        val token = authToken ?: return@runCatching AdvertCreateResponse.Failure("token is null")
        val response: HttpResponse =
            client.submitForm(
                url = "http://api-transport.mvp-pro.top/api/v1/advert_update",
                formParameters = Parameters.build {
                    if (title!=null)append("title", title)
                    if (price!=null)append("price", price)
                    if (description!=null)append("description", description)
                    if (categoryId!=null)append("category", categoryId)
                    if (photos!=null)append("image", "[${photos.joinToString()}]")
                }
            ) {
                headers { append("X-Access-Token", token) }
            }

        val responseBody: String = response.receive()
        val json = Json.Default

        println("responseBody = $responseBody")

        json.decodeFromString<AdvertCreateResponse.Success>(responseBody)
    }.getOrElse { AdvertCreateResponse.Failure(it.stackTraceToString()) }.also { println("it= $it") }

    suspend fun getAdvertList(category: String? = null): AdvertListResponse = kotlin.runCatching {
        val token = authToken ?: return@runCatching AdvertListResponse.Failure("token is null")
        val response: HttpResponse =
            client.submitForm(
                url = "http://api-transport.mvp-pro.top/api/v1/advert_list",
                formParameters = Parameters.build {
                    if (category != null)
                        append("category", category)
                }
            ) {
                headers { append("X-Access-Token", token) }
            }
        val responseBody: String = response.receive()
        val json = Json.Default

        println("rb = $responseBody")

        kotlin.runCatching {
            val map = json.decodeFromString<Map<String, AdvertDTO>>(responseBody)
            println("map = $map")
            AdvertListResponse.Success(map)
        }.getOrElse {
            json.decodeFromString<AdvertListResponse.Failure>(responseBody)
        }
    }.getOrElse {
        AdvertListResponse.Failure(it.stackTraceToString())
    }

    suspend fun getOrderList(category: String? = null): OrderListResponse = kotlin.runCatching {
        val token = authToken ?: return@runCatching OrderListResponse.Failure("token is null")
        val response: HttpResponse =
            client.submitForm(
                url = "http://api-transport.mvp-pro.top/api/v1/order_list",
                formParameters = Parameters.build {
                    if (category != null)
                        append("category", category)
                }
            ) {
                headers { append("X-Access-Token", token) }
            }
        val responseBody: String = response.receive()
        val json = Json.Default

        kotlin.runCatching {
            val map = json.decodeFromString<Map<String, OrderDTO>>(responseBody)
            OrderListResponse.Success(map)
        }.getOrElse {
            println("it = $it")
            json.decodeFromString<OrderListResponse.Failure>(responseBody)
        }
    }.getOrElse { OrderListResponse.Failure(it.stackTraceToString()) }

    suspend fun getAdvertInfo(id: String): AdvertInfoResponse = kotlin.runCatching {
        val token = authToken ?: return@runCatching AdvertInfoResponse.Failure("token is null")
        val response: HttpResponse =
            client.submitForm(
                url = "http://api-transport.mvp-pro.top/api/v1/advert_info",
                formParameters = Parameters.build {
                    append("id", id)
                }
            ) {
                headers { append("X-Access-Token", token) }
            }
        val responseBody: String = response.receive()
        val json = Json.Default

        kotlin.runCatching {
            json.decodeFromString<AdvertInfoResponse.Success>(responseBody)
        }.getOrElse {
            json.decodeFromString<AdvertInfoResponse.Failure>(responseBody)
        }
    }.getOrElse { AdvertInfoResponse.Failure(it.stackTraceToString()) }

    suspend fun updateAdvert(
        title: String? = null,
        price: String? = null,
        description: String? = null
    ): InfoMessageResponse = kotlin.runCatching {
        val token = authToken ?: return@runCatching InfoMessageResponse("token is null")
        val response: HttpResponse =
            client.submitForm(
                url = "http://api-transport.mvp-pro.top/api/v1/advert_update",
                formParameters = Parameters.build {
                    if (title != null)
                        append("title", title)
                    if (price != null)
                        append("price", price)
                    if (description != null)
                        append("description", description)
                }
            ) {
                headers { append("X-Access-Token", token) }
            }
        val responseBody: String = response.receive()
        val json = Json.Default

        json.decodeFromString(responseBody)
    }.getOrElse { InfoMessageResponse(it.stackTraceToString()) }

    suspend fun deleteAdvert(isOrder: Boolean, id: String): InfoMessageResponse = kotlin.runCatching {
        val token = authToken ?: return@runCatching InfoMessageResponse("token is null")
        val urlPart = if (isOrder) "order" else "advert"
        val response: HttpResponse =
            client.submitForm(
                url = "http://api-transport.mvp-pro.top/api/v1/${urlPart}_delete",
                formParameters = Parameters.build {
                    append("id", id)
                }
            ) {
                headers { append("X-Access-Token", token) }
            }
        val responseBody: String = response.receive()
        val json = Json.Default

        json.decodeFromString(responseBody)
    }.getOrElse { InfoMessageResponse(it.stackTraceToString()) }

    suspend fun createOrder(
        category: String,
        fromCity: String,
        fromRegion: String,
        fromPlace: String,
        fromDateTime: String,
        toCity: String,
        toRegion: String,
        toPlace: String,
        description: String,
        name: String,
        phone: String,
        payment: String
    ): AdvertCreateResponse = kotlin.runCatching {
        val token = authToken ?: return@runCatching AdvertCreateResponse.Failure("token is null")
        val response: HttpResponse =
            client.submitForm(
                url = "http://api-transport.mvp-pro.top/api/v1/order_create",
                formParameters = Parameters.build {
                    append("category", category)
                    append("from_city", fromCity)
                    append("from_region", fromRegion)
                    append("from_place", fromPlace)
                    append("from_datetime", fromDateTime)
                    append("to_city", toCity)
                    append("to_region", toRegion)
                    append("to_place", toPlace)
                    append("description", description)
                    append("name", name)
                    append("phone", phone)
                    append("payment", payment)
                }
            ) {
                headers { append("X-Access-Token", token) }
            }
        val responseBody: String = response.receive()
        val json = Json.Default

        json.decodeFromString<AdvertCreateResponse.Success>(responseBody)
    }.getOrElse { AdvertCreateResponse.Failure(it.stackTraceToString()) }

    suspend fun editOrder(
        orderId: String,
        category: String?,
        fromCity: String?,
        fromRegion: String?,
        fromPlace: String?,
        fromDateTime: String?,
        toCity: String?,
        toRegion: String?,
        toPlace: String?,
        description: String?,
        name: String?,
        phone: String?,
        payment: String?
    ): AdvertCreateResponse = kotlin.runCatching {
        val token = authToken ?: return@runCatching AdvertCreateResponse.Failure("token is null")
        val response: HttpResponse =
            client.submitForm(
                url = "http://api-transport.mvp-pro.top/api/v1/order_update",
                formParameters = Parameters.build {
                    append("id", orderId)
                    if(category!=null)append("category", category)
                    if(fromCity!=null)append("from_city", fromCity)
                    if(fromRegion!=null)append("from_region", fromRegion)
                    if(fromPlace!=null)append("from_place", fromPlace)
                    if(fromDateTime!=null)append("from_datetime", fromDateTime)
                    if(toCity!=null)append("to_city", toCity)
                    if(toRegion!=null)append("to_region", toRegion)
                    if(toPlace!=null)append("to_place", toPlace)
                    if(description!=null)append("description", description)
                    if(name!=null)append("name", name)
                    if(phone!=null)append("phone", phone)
                    if(payment!=null)append("payment", payment)
                }
            ) {
                headers { append("X-Access-Token", token) }
            }
        val responseBody: String = response.receive()
        val json = Json.Default

        json.decodeFromString<AdvertCreateResponse.Success>(responseBody)
    }.getOrElse { AdvertCreateResponse.Failure(it.stackTraceToString()) }

}