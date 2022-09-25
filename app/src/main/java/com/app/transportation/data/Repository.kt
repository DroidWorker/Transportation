package com.app.transportation.data

import android.content.Context
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
import java.text.SimpleDateFormat
import java.util.*

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
        paymentCard: String = "",
        avatar: List<String>
    ) = kotlin.runCatching {
        val token = authToken ?: return@runCatching
        client.submitForm(
            url = "http://api-transport.mvp-pro.top/api/v1/profile",
            formParameters = Parameters.build {
                if(name!="") append("first_name", name)
                if(telNumber!="") append("phone", telNumber)
                if(email!="") append("email", email)
                if(paymentCard!="") append("card", paymentCard)
                if(cityArea!="") append("location", cityArea)
                if(avatar.isNotEmpty()) append("avatar", "[${avatar.joinToString()}]")
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
    }.getOrElse {
        UpdateProfileResponse.Failure(it.stackTraceToString()) }

    suspend fun getProfile(id: String): ProfileShortResponse = kotlin.runCatching {
        val token = authToken ?: return@runCatching ProfileShortResponse.Failure("token is null")
        val response: HttpResponse =
            client.get("http://api-transport.mvp-pro.top/api/v1/profile_short?id=$id") {
                headers { append("X-Access-Token", token) }
            }
        var responseBody: String = response.receive()
        val json = Json.Default

        kotlin.runCatching {
            val map = json.decodeFromString<ProfileShortDTO>(responseBody)
            ProfileShortResponse.Success(map)
        }.getOrElse {
            println("profile = $it")
            json.decodeFromString<ProfileShortResponse.Failure>(responseBody)
        }
    }.getOrElse {
            ProfileShortResponse.Failure(it.stackTraceToString())
    }

    suspend fun getNotice(): NoticeResponce = kotlin.runCatching {
        val token = authToken ?: return@runCatching NoticeResponce.Failure("token is null")
        val response: HttpResponse =
            client.get("http://api-transport.mvp-pro.top/api/v1/notice") {
                headers { append("X-Access-Token", token) }
            }
        var responseBody: String = response.receive()
        val json = Json.Default

        kotlin.runCatching {
            val map = json.decodeFromString<List<NoticeDTO>>(responseBody)
            NoticeResponce.Success(map)
        }.getOrElse {
            println("notice = $it")
            json.decodeFromString<NoticeResponce.Failure>(responseBody)
        }
    }.getOrElse {
        NoticeResponce.Failure(it.stackTraceToString())
    }
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

    suspend fun addOrderPing(orderId: String): AddPingResponse = kotlin.runCatching {
        val token = authToken ?: return@runCatching AddPingResponse.Failure("token is null")
        val response: HttpResponse =
            client.submitForm(
                url = "http://api-transport.mvp-pro.top/api/v1/advert_ping_set",
                formParameters = Parameters.build {
                    append("ping_user_id", orderId)
                }
            ) {
                headers { append("X-Access-Token", token) }
            }
        val responseBody: String = response.receive()
        val json = Json.Default

        json.decodeFromString<AddPingResponse.Success>(responseBody)
    }.getOrElse {
        println("pingErrror"+it.stackTraceToString())
        AddPingResponse.Failure(it.stackTraceToString()) }
    suspend fun addAdvertPing(orderId: String): AddPingResponse = kotlin.runCatching {
        val token = authToken ?: return@runCatching AddPingResponse.Failure("token is null")
        val response: HttpResponse =
            client.submitForm(
                url = "http://api-transport.mvp-pro.top/api/v1/order_ping_set",
                formParameters = Parameters.build {
                    append("ping_user_id", orderId)
                }
            ) {
                headers { append("X-Access-Token", token) }
            }
        val responseBody: String = response.receive()
        val json = Json.Default

        json.decodeFromString<AddPingResponse.Success>(responseBody)
    }.getOrElse {
        println("pingErrror"+it.stackTraceToString())
        AddPingResponse.Failure(it.stackTraceToString()) }

    suspend fun deleteAdvertPing(id: String): InfoMessageResponse = kotlin.runCatching {
        val token = authToken ?: return@runCatching InfoMessageResponse("token is null")
        val response: HttpResponse =
            client.submitForm(
                url = "http://api-transport.mvp-pro.top/api/v1/advert_ping_delete",
                formParameters = Parameters.build {
                    append("ping_user_id", id)
                }
            ) {
                headers { append("X-Access-Token", token) }
            }
        val responseBody: String = response.receive()
        val json = Json.Default

        json.decodeFromString(responseBody)
    }.getOrElse { InfoMessageResponse(it.stackTraceToString()) }
    suspend fun deleteOrderPing(id: String): InfoMessageResponse = kotlin.runCatching {
        val token = authToken ?: return@runCatching InfoMessageResponse("token is null")
        val response: HttpResponse =
            client.submitForm(
                url = "http://api-transport.mvp-pro.top/api/v1/order_ping_delete",
                formParameters = Parameters.build {
                    append("ping_user_id", id)
                }
            ) {
                headers { append("X-Access-Token", token) }
            }
        val responseBody: String = response.receive()
        val json = Json.Default

        json.decodeFromString(responseBody)
    }.getOrElse { InfoMessageResponse(it.stackTraceToString()) }

    suspend fun getAdvertPingList(): AdvertPingResponse = kotlin.runCatching {
        val token = authToken ?: return@runCatching AdvertPingResponse.Failure("token is null")
        val response: HttpResponse =
            client.submitForm(
                url = "http://api-transport.mvp-pro.top/api/v1/order_ping_get",
            ) {
                headers { append("X-Access-Token", token) }
            }
        var responseBody: String = response.receive()
        if (responseBody.contains(",\"photo\":[]")) {
            responseBody = responseBody.replace(",\"photo\":[]", ",\"photo\":{}")
        }
        val json = Json.Default

        kotlin.runCatching {
            val map = json.decodeFromString<Map<String, PingAdvertDTO>>(responseBody)
            AdvertPingResponse.Success(map)
        }.getOrElse {
            println("it fav alllllllll = $it")
            if (responseBody.contains("Object not found")){
                var emty : Map<String, PingAdvertDTO> = mutableMapOf("empty" to PingAdvertDTO("","","","","","","", "", "", emptyMap(), emptyList()))
                AdvertPingResponse.Success(emty)
            }
            json.decodeFromString<AdvertPingResponse.Failure>(responseBody)
        }
    }.getOrElse { AdvertPingResponse.Failure(it.stackTraceToString()) }
    suspend fun getOrderPingList(): OrderPingResponse = kotlin.runCatching {
        val token = authToken ?: return@runCatching OrderPingResponse.Failure("token is null")
        val response: HttpResponse =
            client.submitForm(
                url = "http://api-transport.mvp-pro.top/api/v1/advert_ping_get",
            ) {
                headers { append("X-Access-Token", token) }
            }
        var responseBody: String = response.receive()
        if (responseBody.contains(",\"photo\":[]")) {
            responseBody = responseBody.replace(",\"photo\":[]", ",\"photo\":{}")
        }
        val json = Json.Default

        kotlin.runCatching {
            val map = json.decodeFromString<Map<String, PingOrderDTO>>(responseBody)
            OrderPingResponse.Success(map)
        }.getOrElse {
            println("it ping alllllllll = $it")
            if (responseBody.contains("Object not found")){
                var emty : Map<String, PingOrderDTO> = mutableMapOf("empty" to PingOrderDTO("","","","","","","", "","","","","","","","","", "", "",emptyMap(), emptyList()))
                OrderPingResponse.Success(emty)
            }
            else {
                json.decodeFromString<OrderPingResponse.Failure>(responseBody)
            }
        }
    }.getOrElse { OrderPingResponse.Failure(it.stackTraceToString()) }

    suspend fun getAdvertCatgoryList(): StringResponce = kotlin.runCatching {
        val token = authToken ?: return@runCatching StringResponce.Failure("token is null")
        val response: HttpResponse =
            client.submitForm(
                url = "http://api-transport.mvp-pro.top/api/v1/advert_category_list",
            ) {
                headers { append("X-Access-Token", token) }
            }
        var responseBody: String = response.receive()

        kotlin.runCatching {
            val array : List<String>
            if (responseBody.contains("|"))  {
                responseBody = responseBody.split(":")[1]
                responseBody = responseBody.replace("\"", "")
                responseBody = responseBody.replace("}", "")
                array = responseBody.split("|")
            }
            else {throw Exception("No Items")}

            StringResponce.Success(array)
        }.getOrElse {
            if (responseBody.contains("No Items")){
                StringResponce.Failure("No Items")
            }
            else {
                StringResponce.Failure(responseBody)
            }
        }
    }.getOrElse {
        StringResponce.Failure(it.stackTraceToString())
    }

    suspend fun getOrderCountNews(): IntIntResponce = kotlin.runCatching {
        val token = authToken ?: return@runCatching IntIntResponce.Failure("token is null")
        val response: HttpResponse =
            client.submitForm(
                url = "http://api-transport.mvp-pro.top/api/v1/order_count_news",
            ) {
                headers { append("X-Access-Token", token) }
            }
        var responseBody: String = response.receive()
        val json = Json.Default

        kotlin.runCatching {
            if (responseBody.contains("["))  {
                responseBody = responseBody.replace("[", "")
                responseBody = responseBody.replace("]", "")
            }
            else {throw Exception("No Items")}

            val map = json.decodeFromString<Map<Int, Int>>(responseBody)
            IntIntResponce.Success(map)
        }.getOrElse {
            if (responseBody.contains("No Items")){
                IntIntResponce.Failure("No Items")
            }
            else {
                IntIntResponce.Failure(responseBody)
            }
        }
    }.getOrElse {
        IntIntResponce.Failure(it.stackTraceToString())
    }

    suspend fun addAdvertToFavorite(orderId: String): AddFavoriteResponsee = kotlin.runCatching {
        val token = authToken ?: return@runCatching AddFavoriteResponsee.Failure("token is null")
        val response: HttpResponse =
            client.submitForm(
                url = "http://api-transport.mvp-pro.top/api/v1/advert_set_favorite",
                formParameters = Parameters.build {
                    append("order_id", orderId)
                }
            ) {
                headers { append("X-Access-Token", token) }
            }
        val responseBody: String = response.receive()
        val json = Json.Default

        json.decodeFromString<AddFavoriteResponsee.Success>(responseBody)
    }.getOrElse { AddFavoriteResponsee.Failure(it.stackTraceToString()) }
    suspend fun addOrderToFavorite(orderId: String): AddFavoriteResponsee = kotlin.runCatching {
        val token = authToken ?: return@runCatching AddFavoriteResponsee.Failure("token is null")
        val response: HttpResponse =
            client.submitForm(
                url = "http://api-transport.mvp-pro.top/api/v1/order_set_favorite",
                formParameters = Parameters.build {
                    append("advert_id", orderId)
                }
            ) {
                headers { append("X-Access-Token", token) }
            }
        val responseBody: String = response.receive()
        val json = Json.Default

        json.decodeFromString<AddFavoriteResponsee.Success>(responseBody)
    }.getOrElse { AddFavoriteResponsee.Failure(it.stackTraceToString()) }

    suspend fun getAdvertFavoriteList(): AdvertFavResponse = kotlin.runCatching {
        val token = authToken ?: return@runCatching AdvertFavResponse.Failure("token is null")
        val response: HttpResponse =
            client.submitForm(
                url = "http://api-transport.mvp-pro.top/api/v1/advert_get_favorite",
            ) {
                headers { append("X-Access-Token", token) }
            }
        var responseBody: String = response.receive()
        if (responseBody.contains(",\"photo\":[]")) {
            responseBody = responseBody.replace(",\"photo\":[]", ",\"photo\":{}")
        }
        val json = Json.Default

        kotlin.runCatching {
            val map = json.decodeFromString<Map<String, FavAdvertDTO>>(responseBody)
            AdvertFavResponse.Success(map)
        }.getOrElse {
            println("it fav alllllllll = $it")
            if (responseBody.contains("Object not found")){
                var emty : Map<String, FavAdvertDTO> = mutableMapOf("empty" to FavAdvertDTO("","","","","","","", "", "", emptyMap()))
                AdvertFavResponse.Success(emty)
            }
            json.decodeFromString<AdvertFavResponse.Failure>(responseBody)
        }
    }.getOrElse {
        if (it.stackTraceToString().contains("Object not found")){
            var emty : Map<String, FavAdvertDTO> = mutableMapOf("empty" to FavAdvertDTO("","","","","","","", "", "", emptyMap()))
            AdvertFavResponse.Success(emty)
         }
        else
            AdvertFavResponse.Failure(it.stackTraceToString())
    }
    suspend fun getOrderFavoriteList(): OrderFavResponse = kotlin.runCatching {
        val token = authToken ?: return@runCatching OrderFavResponse.Failure("token is null")
        val response: HttpResponse =
            client.submitForm(
                url = "http://api-transport.mvp-pro.top/api/v1/advert_get_favorite",
            ) {
                headers { append("X-Access-Token", token) }
            }
        var responseBody: String = response.receive()
        if (responseBody.contains(",\"photo\":[]")) {
            responseBody = responseBody.replace(",\"photo\":[]", ",\"photo\":{}")
        }
        val json = Json.Default

        kotlin.runCatching {
            val map = json.decodeFromString<Map<String, FavOrderDTO>>(responseBody)
            OrderFavResponse.Success(map)
        }.getOrElse {
            println("it fav alllllllll = $it")
            if (responseBody.contains("Object not found")){
                var emty : Map<String, FavOrderDTO> = mutableMapOf("empty" to FavOrderDTO("","","","","","","", "","","","","", "",  "","","","","", emptyMap()))
                OrderFavResponse.Success(emty)
            }
            else {
                json.decodeFromString<OrderFavResponse.Failure>(responseBody)
            }
        }
    }.getOrElse {
        if (it.stackTraceToString().contains("Object not found")){
            var emty : Map<String, FavOrderDTO> = mutableMapOf("empty" to FavOrderDTO("","","","","","","", "","","","","","","","","","","", emptyMap()))
            OrderFavResponse.Success(emty)
        }
        else
            OrderFavResponse.Failure(it.stackTraceToString())
    }

    suspend fun deleteAdvertFavorite(id: String): InfoMessageResponse = kotlin.runCatching {
        val token = authToken ?: return@runCatching InfoMessageResponse("token is null")
        val response: HttpResponse =
            client.submitForm(
                url = "http://api-transport.mvp-pro.top/api/v1/advert_delete_favorite",
                formParameters = Parameters.build {
                    append("order_id", id)
                }
            ) {
                headers { append("X-Access-Token", token) }
            }
        val responseBody: String = response.receive()
        val json = Json.Default

        json.decodeFromString(responseBody)
    }.getOrElse { InfoMessageResponse(it.stackTraceToString()) }
    suspend fun deleteOrderFavorite(id: String): InfoMessageResponse = kotlin.runCatching {
        val token = authToken ?: return@runCatching InfoMessageResponse("token is null")
        val response: HttpResponse =
            client.submitForm(
                url = "http://api-transport.mvp-pro.top/api/v1/order_delete_favorite",
                formParameters = Parameters.build {
                    append("advert_id", id)
                }
            ) {
                headers { append("X-Access-Token", token) }
            }
        val responseBody: String = response.receive()
        val json = Json.Default

        json.decodeFromString(responseBody)
    }.getOrElse { InfoMessageResponse(it.stackTraceToString()) }

    suspend fun search(str : String): SearchResponse = kotlin.runCatching {
        val token = authToken ?: return@runCatching SearchResponse.Failure("token is null")
        val response: HttpResponse = client.submitForm(url = "http://api-transport.mvp-pro.top/api/v1/search",
            formParameters = Parameters.build {
                append("q", str)
            }
        ) {
            headers { append("X-Access-Token", token) }
        }
        val responseBody: String = response.receive()
        val json = Json.Default
        kotlin.runCatching {
            val map = json.decodeFromString<Map<String, SResultDTO>>(responseBody)
            SearchResponse.Success(map)
        }.getOrElse {
            println("searched = $it")
            json.decodeFromString<SearchResponse.Failure>(responseBody)
        }
        }.getOrElse { SearchResponse.Failure(it.stackTraceToString()) }

    suspend fun getStaticData(type : String): StaticDateResponse = kotlin.runCatching {
        val response: HttpResponse = client.submitForm(url = "http://api-transport.mvp-pro.top/api/v1/datas_text?type=$type")
        val responseBody: String = response.receive()
        val json = Json.Default
        kotlin.runCatching {
            json.decodeFromString<StaticDateResponse.Success>(responseBody)
        }.getOrElse {
            println("Static data = $it")
            json.decodeFromString<StaticDateResponse.Failure>(responseBody)
        }
    }.getOrElse {
        StaticDateResponse.Failure(it.stackTraceToString())
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
                            parentId = it.value.parentId.toInt(),
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
                    thirdLevelCategories.find { tlc->
                        tlc.id.toString() == it.value.parentId
                    }?.childId=it.key.toInt() ?: 0
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

    suspend fun createAdvert(ctx: Context? = null,
        title: String,
        price: String,
        description: String,
        categoryId: String,
        photos: List<String>,
        options: List<String>
    ): AdvertCreateResponse = kotlin.runCatching {
        if (authToken == null) logout()
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
                    append("options", "[${options.joinToString()}]")
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
        id: String,
        title: String,
        price: String,
        description: String,
        photos: List<String>
    ): AdvertCreateResponse = kotlin.runCatching {
        if (authToken == null) logout()
        val token = authToken ?: return@runCatching AdvertCreateResponse.Failure("token is null")
        val response: HttpResponse =
            client.submitForm(
                url = "http://api-transport.mvp-pro.top/api/v1/advert_update",
                formParameters = Parameters.build {
                    if (id!=null)append("id", id)
                    if (title!=null)append("title", title)
                    if (price!=null)append("price", price)
                    if (description!=null)append("description", description)
                    //if (photos!=null)append("image", "[${photos.joinToString()}]")
                }
            ) {
                headers { append("X-Access-Token", token) }
            }

        val responseBody: String = response.receive()
        println("updaaaaate "+responseBody)
        val json = Json.Default

        println("responseBody = $responseBody")

        json.decodeFromString<AdvertCreateResponse.Success>(responseBody)
    }.getOrElse { AdvertCreateResponse.Failure(it.stackTraceToString()) }.also { println("it= $it") }

    suspend fun getAdvertList(category: String? = null): AdvertListResponse = kotlin.runCatching {
        if (authToken == null) logout()
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
        var responseBody: String = response.receive()
        if (responseBody.contains(",\"photo\":[]")) {
            responseBody = responseBody.replace(",\"photo\":[]", ",\"photo\":{}")
        }
        if (responseBody.contains(",\"ping\":[]")) {
            responseBody = responseBody.replace(",\"ping\":[]", ",\"ping\":{}")
        }
        val json = Json.Default
        kotlin.runCatching {
            val map = json.decodeFromString<Map<String, AdvertDTO>>(responseBody)
            AdvertListResponse.Success(map)
        }.getOrElse {
            json.decodeFromString<AdvertListResponse.Failure>(responseBody)
        }
    }.getOrElse {
        AdvertListResponse.Failure(it.stackTraceToString())
    }

    suspend fun getOrderList(category: String? = null): OrderListResponse = kotlin.runCatching {
        if (authToken == null) logout()
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
        var responseBody: String = response.receive()
        if (responseBody.contains(",\"photo\":[]")) {
            responseBody = responseBody.replace(",\"photo\":[]", ",\"photo\":{}")
        }
        if (responseBody.contains(",\"ping\":[]")) {
            responseBody = responseBody.replace(",\"ping\":[]", ",\"ping\":{}")
        }
        val json = Json.Default
        kotlin.runCatching {
            val map = json.decodeFromString<Map<String, OrderDTO>>(responseBody)
            OrderListResponse.Success(map)
        }.getOrElse {
            println("orderlistFailure"+it.stackTraceToString())
            json.decodeFromString<OrderListResponse.Failure>(responseBody)
        }
    }.getOrElse {
        println("error: "+it.stackTraceToString())
        OrderListResponse.Failure(it.stackTraceToString())
    }

    suspend fun getAdvertFullList(): AdvertFullListResponse = kotlin.runCatching {
        val token = authToken ?: return@runCatching AdvertFullListResponse.Failure("token is null")
        val response: HttpResponse =
            client.submitForm(
                url = "http://api-transport.mvp-pro.top/api/v1/advert_list_common",
            ) {
                headers { append("X-Access-Token", token) }
            }
        var responseBody: String = response.receive()
        if (responseBody.contains(",\"photo\":[]")) {
            responseBody = responseBody.replace(",\"photo\":[]", ",\"photo\":{}")
        }
        val json = Json.Default

        kotlin.runCatching {
            val map = json.decodeFromString<Map<String, AdvertFullDTO>>(responseBody)
            AdvertFullListResponse.Success(map)
        }.getOrElse {
            println("it alllllllll = $it")
            if (responseBody.contains("Object not found")){
                var emty : Map<String, AdvertFullDTO> = mutableMapOf("empty" to AdvertFullDTO("", "","","","","","","", "", emptyMap(), emptyList()))
                AdvertFullListResponse.Success(emty)
            }
            json.decodeFromString<AdvertFullListResponse.Failure>(responseBody)
        }
    }.getOrElse { AdvertFullListResponse.Failure(it.stackTraceToString()) }

    suspend fun getOrderFullList(): OrderFullListResponse = kotlin.runCatching {
        val token = authToken ?: return@runCatching OrderFullListResponse.Failure("token is null")
        val response: HttpResponse =
            client.submitForm(
                url = "http://api-transport.mvp-pro.top/api/v1/order_list_common",
            ) {
                headers { append("X-Access-Token", token) }
            }
        var responseBody: String = response.receive()
        if (responseBody.contains(",\"photo\":[]")) {
            responseBody = responseBody.replace(",\"photo\":[]", ",\"photo\":{}")
        }
        val json = Json.Default

        kotlin.runCatching {
            val map = json.decodeFromString<Map<String, OrderFullDTO>>(responseBody)
            OrderFullListResponse.Success(map)
        }.getOrElse {
            println("it alllllllll = $it")
            if (responseBody.contains("Object not found")){
                var emty : Map<String, OrderFullDTO> = mutableMapOf("empty" to OrderFullDTO("", "","","","","","","","","","","","","","","","","", emptyMap()))
                OrderFullListResponse.Success(emty)
            }
            json.decodeFromString<OrderFullListResponse.Failure>(responseBody)
        }
    }.getOrElse { OrderFullListResponse.Failure(it.stackTraceToString()) }

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

    suspend fun deleteAdvert(isOrder: Boolean, id: String): InfoMessageResponse = kotlin.runCatching {
        if (authToken == null) logout()
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

    suspend fun createOrder(ctx: Context? = null,
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
        payment: String,
        photos: List<String>
    ): AdvertCreateResponse = kotlin.runCatching {
        if (authToken == null) logout()
        val token = authToken ?: return@runCatching AdvertCreateResponse.Failure("token is null")
        val date = fromDateTime.replace('/', '.').replace(' ', 'T')
        val response: HttpResponse =
            client.submitForm(
                url = "http://api-transport.mvp-pro.top/api/v1/order_create",
                formParameters = Parameters.build {
                    append("category", category)
                    append("from_city", fromCity)
                    append("from_region", fromRegion)
                    append("from_place", fromPlace)
                    append("from_datetime", "01.01.2022T10:00")
                    append("to_city", toCity)
                    append("to_region", toRegion)
                    append("to_place", toPlace)
                    append("description", description)
                    append("name", name)
                    append("phone", phone)
                    append("payment", payment)
                    append("image", "[${photos.joinToString()}]")
                }
            ) {
                headers { append("X-Access-Token", token) }
            }
        val responseBody: String = response.receive()
        val json = Json.Default

        json.decodeFromString<AdvertCreateResponse.Success>(responseBody)
    }.getOrElse {        AdvertCreateResponse.Failure(it.stackTraceToString()) }

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
        if (authToken == null) logout()
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