package com.app.transportation.ui

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Pair
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.app.transportation.R
import com.app.transportation.data.AuthTokenNotFoundException
import com.app.transportation.data.Repository
import com.app.transportation.data.api.*
import com.app.transportation.data.database.entities.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named
import java.net.UnknownHostException

class MainViewModel(val app: Application) : AndroidViewModel(app), KoinComponent {

    private val repository: Repository by inject()
    var ctx: Context? = null

    val advertCategoriesFlow = repository.advertCategoriesFlow()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val advertsSF = MutableStateFlow(emptyList<Advert>())
    val ordersSF = MutableStateFlow(emptyList<Advert>())

    private val prefs: SharedPreferences by inject(named("MainSettings"))

    private var authToken: String?
        get() = prefs.getString("authToken", null)
        set(value) = prefs.edit { putString("authToken", value) }

    var lastAdvertAdded: Int?
        get() = prefs.getInt("lastAdvertAdded", 1)
        set(value) = prefs.edit {
            if (value != null) {
                putInt("lastAdvertAdded", value)
            }
        }


    val messageEvent = MutableSharedFlow<String>(extraBufferCapacity = 1)

    val logoutSF = MutableSharedFlow<String>(extraBufferCapacity = 1)

    val secondLevelCategoriesFlow = channelFlow {
        advertCategoriesFlow.collectLatest { advertCats ->
            send(
                advertCats.filter { it.level == 2 }
            )
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val cachedSearchResult = MutableStateFlow(emptyList<Advert>())

    val cachedTarif = MutableStateFlow(emptyList<TarifDTO>())
    val cachedNews = MutableStateFlow(emptyList<NewsDTO>())

    val cachedAdvertsSF = MutableStateFlow(emptyList<Advert>())
    val cachedOrdersSF = MutableStateFlow(emptyList<Advert>())
    val cachedFilterCategoriesSF = MutableStateFlow(emptyList<ServiceType>())
    val cachedStatic = MutableStateFlow<String>("loading...")

    val cachedAdvertFavorite = MutableStateFlow(emptyList<Advert>())
    val cachedOrderFavorite = MutableStateFlow(emptyList<Advert>())

    val cachedAdvertPing = MutableStateFlow(emptyList<Advert>())
    val cachedOrderPing = MutableStateFlow(emptyList<Advert>())
    val cachedAdvertFeedbackPing = MutableStateFlow(emptyList<Advert>())
    val cachedOrderFeedbackPing = MutableStateFlow(emptyList<Advert>())

    val cachedAdvertCategories = MutableStateFlow(emptyList<Int>())
    val cachedOrderNews = MutableStateFlow(emptyMap<Int, Int>())
    val cachedBussinessLast = MutableStateFlow(emptyList<BusinessLastItemDTO>())

    val cachedPlaces = MutableStateFlow(emptyMap<Pair<Float, Float>, Advert>())

    val cachedProfile = MutableStateFlow(ProfileShort())
    val cachedNotifications = MutableStateFlow(emptyMap<String, NoticeDTO>())

    val cachedAdvert = MutableStateFlow<Advert?>(null)
    val cachedOrder = MutableStateFlow<Advert?>(null)

    val cafTempPhotoUris = MutableStateFlow(Pair(0, mutableListOf<Uri>()))
    val adfTempPhotoUris = MutableStateFlow(Pair(0, emptyList<Bitmap>()))
    val profilePhotoUri = MutableStateFlow(Pair(-1, mutableListOf<Uri>()))

    val isCustomer = MutableStateFlow(
        prefs.getBoolean("isCustomer", false).takeIf { prefs.contains("isCustomer") }
    )

    val deletedAdvertPosition = MutableSharedFlow<Int>(extraBufferCapacity = 1)

    var dateTime = ""

    init {
        updateMainFragmentData()
    }

    private suspend fun updateStatic(type: String ="") = viewModelScope.launch(Dispatchers.IO) {
        val result = getStaticDatas(type) ?: return@launch
        cachedStatic.tryEmit(result)
    }

    private suspend fun getStaticDatas(type: String) =
        (repository.getStaticData(type) as? StaticDateResponse.Success)?.let { response ->
            response.id.text
        }

    private suspend fun getSearched(str: String) =
        (repository.search(str) as? SearchResponse.Success)?.let { response ->
            response.resMap.map { entry ->
                Advert(
                    id = entry.key.toInt(),
                    viewType = 1,
                    categoryId = 0,
                    category = entry.value.category,
                    subcategoryId = 0,
                    title = entry.value.title,
                    date = "null",
                    time = "null",
                    price = entry.value.price,
                    photo = emptyList()
                )
            }
        }

    private suspend fun updateSearched(
        updateCache: Boolean = false,
        str: String = ""
    ) = viewModelScope.launch(Dispatchers.IO) {
        val list = getSearched(str) ?: return@launch

        if (updateCache)
            cachedSearchResult.tryEmit(list)
    }

    fun updateMainFragmentData() = viewModelScope.launch(Dispatchers.IO) {
        when (val result = repository.updateAdvertCategories()) {
            null -> Unit
            is AuthTokenNotFoundException -> {
                messageEvent.tryEmit("401")
                logout()
            }
            is UnknownHostException -> {
                messageEvent.tryEmit("Проблемы с интернетом")
            }
            else -> {

            }
        }
    }

    fun getCategoryAdverts(categoryId: Int) = viewModelScope.launch(Dispatchers.IO) {
        if (isCustomer.value == true)
            updateAdverts(true, categoryId)
        else
            updateOrders(true, categoryId)

        updateFilterThirdLevelCategories(categoryId)
    }

    fun getAdvertsInCategory(categoryId: Int) = viewModelScope.launch (Dispatchers.IO){
        updateAdverts(true, categoryId)
    }

    fun getMyOrders() = viewModelScope.launch (Dispatchers.IO){
        updateOrders(true, 0)
    }
    fun getMyAdverts() = viewModelScope.launch (Dispatchers.IO){
        updateAdverts(true, 0)
    }
    fun getAdvertById(id: String) = viewModelScope.launch (Dispatchers.IO){
        updateCategoryAdvertsFull(true, 0, id)
    }

    fun getFeedbackOrdersAdverts() = viewModelScope.launch(Dispatchers.IO){
        updateFeedbackOrders()
        updateFeedbackAdverts()
    }

    fun getCategoryOrders(categoryId: Int) = viewModelScope.launch(Dispatchers.IO) {
        updateOrders(true, categoryId)

        updateFilterThirdLevelCategories(categoryId)
    }

    fun getUserAdvertByCategory(categoryId: String, userId: String) = viewModelScope.launch(Dispatchers.IO) {
        updateCategoryAdvertsUC(true, categoryId, userId)
    }

    fun getAllCategoryAdverts(categoryId: Int) = viewModelScope.launch(Dispatchers.IO) {
        updateCategoryAdvertsFull(true, categoryId)
        updateFilterThirdLevelCategories(categoryId)
    }

    fun getAllCategoryOrders(categoryId: Int) = viewModelScope.launch(Dispatchers.IO) {
        updateCategoryOrdersFull(true, categoryId)
        updateFilterThirdLevelCategories(categoryId)
    }

    fun getSearchResult(str: String) = viewModelScope.launch(Dispatchers.IO) {
        updateSearched(true, str)
    }

    fun getStaticData(type: String) = viewModelScope.launch(Dispatchers.IO) {
        updateStatic(type)
    }

    fun getOrdersFavorite() = viewModelScope.launch(Dispatchers.IO) {
        updateFavoriteOrdersFull()
    }

    fun getAdvertsFavorite() = viewModelScope.launch(Dispatchers.IO) {
        updateFavoriteAdvertsFull()
    }

    fun addAdvertFavorite(
        orderId: String
    ) = viewModelScope.launch(Dispatchers.IO) {
        val result = repository.addAdvertToFavorite(
            orderId = orderId
        )
        when (result) {
            is AddFavoriteResponsee.Success -> {
                if (result.message == "Success" || result.message == "Not change")
                    messageEvent.tryEmit("добавлено в избранное!")
            }
            is AddFavoriteResponsee.Failure -> {
                if(result.message.contains("Bussiness limit"))
                    messageEvent.tryEmit("Невозможно добавить в избранное!")
                else
                    messageEvent.tryEmit("Ошибка! " + result.message)
            }
        }
    }

    fun addOrderFavorite(
        orderId: String
    ) = viewModelScope.launch(Dispatchers.IO) {
        val result = repository.addOrderToFavorite(
            orderId = orderId
        )
        when (result) {
            is AddFavoriteResponsee.Success -> {
                if (result.message == "Success" || result.message == "Not change")
                    messageEvent.tryEmit("добавлено в избранное!")
            }
            is AddFavoriteResponsee.Failure -> {
                if(result.message.contains("Bussiness limit"))
                    messageEvent.tryEmit("Невозможно добавить в избранное!")
                else
                    messageEvent.tryEmit("Ошибка! " + result.message)
            }
        }
    }

    fun deleteAdvertFavorite(id: Int) =
        viewModelScope.launch(Dispatchers.IO) {
            when (val s = repository.deleteAdvertFavorite(id.toString()).message) {
                "Success" -> {
                    messageEvent.tryEmit("успешно удалено!")
                    getAdvertsPing()
                }
                else -> messageEvent.tryEmit("Ошибка при удалении!"+s+"|||"+id)
            }
        }

    fun deleteOrderFavorite(id: Int) =
        viewModelScope.launch(Dispatchers.IO) {
            when (val s = repository.deleteOrderFavorite(id.toString()).message) {
                "Success" -> {
                    messageEvent.tryEmit("успешно удалено!")
                    getOrdersPing()
                }
                else -> messageEvent.tryEmit("Ошибка при удалении!"+s+"|||"+id)
            }
        }

    fun setBusinessStatus(id: String, status: String) =
        viewModelScope.launch(Dispatchers.IO) {
            when (repository.setBusiness(id, status).message) {
                "Success" -> {
                    messageEvent.tryEmit("Бизнес статус активен!")
                    getAdvertsPing()
                }
                else -> messageEvent.tryEmit("Ошибка! Потеряно соединение")
            }
        }

    fun setOptionStatus(advertId: String, status: String) =
        viewModelScope.launch(Dispatchers.IO) {
            when (repository.setOptionStatus(advertId, status).message) {
                "Success" -> {
                    messageEvent.tryEmit("Платные функции активированы!")
                    getAdvertsPing()
                }
                else -> messageEvent.tryEmit("Ошибка! Потеряно соединение")
            }
        }

    fun setPingStatus(
        userId: String,
        orderId: String?,
        advertId: String?,
        status: String
    ) = viewModelScope.launch (Dispatchers.IO){
        val result = repository.setPing(
            userId = userId,
            orderId = orderId,
            advertId = advertId,
            status = status
        )
        when (result) {
            is AddPingResponse.Success -> {
                //if (result.message == "Success"||result.message == "Not change")
                    //messageEvent.tryEmit(status)
            }
            is AddPingResponse.Failure -> {
                messageEvent.tryEmit("Ошибка! "+result.message)
            }
        }
    }

    fun addOrderPing(
        orderId: String
    ) = viewModelScope.launch(Dispatchers.IO) {
        val result = repository.addOrderPing(
            orderId = orderId
        )
        when (result) {
            is AddPingResponse.Success -> {
                if (result.message == "Success"||result.message == "Not change")
                    messageEvent.tryEmit("отклик оставлен!")
            }
            is AddPingResponse.Failure -> {
                messageEvent.tryEmit("Ошибка! "+result.message)
            }
        }
    }

    fun addAdvertPing(
        orderId: String
    ) = viewModelScope.launch(Dispatchers.IO) {
        val result = repository.addAdvertPing(
            orderId = orderId
        )
        when (result) {
            is AddPingResponse.Success -> {
                if (result.message == "Success"||result.message == "Not change")
                    messageEvent.tryEmit("отклик оставлен!")
            }
            is AddPingResponse.Failure -> {
                messageEvent.tryEmit("Ошибка! "+result.message)
            }
        }
    }

    fun deleteAdvertPing(id: Int) =
        viewModelScope.launch(Dispatchers.IO) {
            when (repository.deleteAdvertPing(id.toString()).message) {
                "Success" -> {
                    messageEvent.tryEmit("Объявление успешно удалено!")
                    getAdvertsPing()
                }
                else -> messageEvent.tryEmit("Ошибка при удалении объявления!")
            }
        }

    fun deleteOrderPing(id: Int) =
        viewModelScope.launch(Dispatchers.IO) {
            when (repository.deleteOrderPing(id.toString()).message) {
                "Success" -> {
                    messageEvent.tryEmit("Объявление успешно удалено!")
                    getOrdersPing()
                }
                else -> messageEvent.tryEmit("Ошибка при удалении объявления!")
            }
        }

    fun getOrdersPing() = viewModelScope.launch(Dispatchers.IO) {
        updatePingOrdersFull()
    }

    fun getAdvertsPing() = viewModelScope.launch(Dispatchers.IO) {
        updatePingAdvertsFull()
    }
    fun getAdvertCatsList() = viewModelScope.launch (Dispatchers.IO){
        getAdvertCatgoryList()
    }
    fun getOrderCountNews() = viewModelScope.launch (Dispatchers.IO){
        getOrderCount()
    }
    fun resetOrderCountNews(id: String) = viewModelScope.launch (Dispatchers.IO){
        repository.resetOrderCountNews(id)
    }
    fun getBusinessLast() = viewModelScope.launch (Dispatchers.IO){
        getBuisnessSix()
    }
    fun getPlcs() = viewModelScope.launch (Dispatchers.IO){
        getPlaces()
    }

    private suspend fun getOrdersFavoriteFull() =
        (repository.getOrderFavoriteList() as? OrderFavResponse.Success)?.let { response ->
            response.orderMap.map { entry ->
                if (entry.key!="empty") {
                    val photoList: ArrayList<String> = ArrayList()
                    entry.value.photo.forEach { photoitem ->
                        photoitem.value?.let { photoList.add(it.replace("data:image/jpg;base64,", "")) }
                    }
                    Advert(
                        id = entry.value.order_id.toInt(),
                        viewType = 0,
                        categoryId = advertCategoriesFlow.value.find {
                            it.id == entry.value.categoryId.toInt()
                        }?.parentId ?: 4,
                        category = entry.value.category,
                        subcategoryId = entry.value.categoryId.toInt(),
                        title = entry.value.description,
                        date = entry.value.date,
                        time = entry.value.time,
                        fromCity = "${entry.value.fromCity}",
                        fromRegion = "${entry.value.fromRegion}",
                        fromPlace = "${entry.value.fromPlace}",
                        toCity = "${entry.value.toCity}",
                        toRegion = "${entry.value.toRegion}",
                        toPlace = "${entry.value.toPlace}",
                        payment = entry.value.payment!!,
                        description = entry.value.description,
                        photo = photoList.toList()
                    )
                }
                else
                {
                    Advert(
                        id = 0,
                        viewType = 2,
                        categoryId = 0,
                        category = "",
                        subcategoryId = 0,
                        title = "Избранного не найдено",
                        date = "",
                        time = "",
                        fromCity = "",
                        fromRegion = "",
                        fromPlace = "",
                        toCity = "",
                        toRegion = "",
                        toPlace = "",
                        payment = "",
                        description = "",
                        photo = emptyList())
                }
            }
        }
    private suspend fun getAdvertsFavoriteFull() =
        (repository.getAdvertFavoriteList() as? AdvertFavResponse.Success)?.let { response ->
            response.advertMap.map { entry ->
                if (entry.key != "empty") {
                    val photoList: ArrayList<String> = ArrayList()
                    entry.value.photo.forEach { photoitem ->
                        photoitem.value?.let { photoList.add(it.replace("data:image/jpg;base64,", "")) }
                    }
                    Advert(
                        id = entry.value.advert_id.toInt(),
                        viewType = if (photoList.size>0) 0 else 1,
                        categoryId = advertCategoriesFlow.value.find {
                            it.id == entry.value.categoryId.toInt()
                        }?.parentId ?: 4,
                        category = entry.value.category,
                        subcategoryId = entry.value.categoryId.toInt(),
                        title = entry.value.title,
                        date = entry.value.date,
                        time = entry.value.time,
                        price = entry.value.price,
                        payment = "advert",
                        photo = photoList.toList()
                    )
                }
                else{
                    Advert(
                        id = 0,
                        viewType = 2,
                        categoryId = 0,
                        category = "",
                        subcategoryId = 0,
                        title = "Избранного не найдено",
                        date = "",
                        time = "",
                        fromCity = "",
                        fromRegion = "",
                        fromPlace = "",
                        toCity = "",
                        toRegion = "",
                        toPlace = "",
                        payment = "",
                        description = "",
                        photo = emptyList())
                }
            }
        }

    private suspend fun getOrdersPingFull() =
        (repository.getOrderPingList() as? OrderPingResponse.Success)?.let { SFresponse ->
            var response : OrderPingResponse
            if(SFresponse is OrderPingResponse.Success) {
                response = SFresponse as OrderPingResponse.Success
                response.orderMap.map { entry ->
                    if (entry.key!="empty") {
                        val photoList: ArrayList<String> = ArrayList()
                        entry.value.photo.forEach { photoitem ->
                            photoList.add(photoitem.value.replace("data:image/jpg;base64,", ""))
                        }
                        Advert(
                            id = entry.value.order_id.toInt(),//entry.key.toInt(),
                            userId = entry.value.user_id,
                            viewType = 1,
                            categoryId = advertCategoriesFlow.value.find {
                                it.id == entry.value.categoryId.toInt()
                            }?.parentId ?: 4,
                            category = entry.value.category,
                            subcategoryId = entry.value.categoryId.toInt(),
                            title = entry.value.name+"|"+entry.value.phone,
                            date = entry.value.date,
                            time = entry.value.time,
                            fromCity = "${entry.value.fromCity}",
                            fromRegion = "${entry.value.fromRegion}",
                            fromPlace = "${entry.value.fromPlace}",
                            toCity = "${entry.value.toCity}",
                            toRegion = "${entry.value.toRegion}",
                            toPlace = "${entry.value.toPlace}",
                            payment = entry.value.payment!!,
                            description = entry.value.description,
                            photo = photoList.toList(),
                            profile = entry.value.ping,
                        )
                    }
                    else
                    {
                        Advert(
                            id = 0,
                            viewType = 2,
                            categoryId = 0,
                            category = "",
                            subcategoryId = 0,
                            title = "Откликов не найдено",
                            date = "",
                            time = "",
                            fromCity = "",
                            fromRegion = "",
                            fromPlace = "",
                            toCity = "",
                            toRegion = "",
                            toPlace = "",
                            payment = "",
                            description = "",
                            photo = emptyList())
                    }
                }
            }
            else{
                response = SFresponse as OrderPingResponse.Failure
                if (response.message.contains("header not found")) {
                    messageEvent.tryEmit("401")
                    null
                }
                else null
            }
        }
    private suspend fun getAdvertsPingFull() =
        (repository.getAdvertPingList() as? AdvertPingResponse)?.let { SFresponse ->
            var response : AdvertPingResponse
            if(SFresponse is AdvertPingResponse.Success) {
                response = SFresponse as AdvertPingResponse.Success
                response.advertMap.map { entry ->
                    if (entry.key != "empty") {
                        val photoList: ArrayList<String> = ArrayList()
                        entry.value.photo.forEach { photoitem ->
                            photoList.add(photoitem.value.replace("data:image/jpg;base64,", ""))
                        }
                        Advert(
                            id = entry.value.advert_id.toInt(),//entry.key.toInt(),
                            userId = entry.value.user_id,
                            viewType = 0,
                            categoryId = advertCategoriesFlow.value.find {
                                it.id == entry.value.categoryId.toInt()
                            }?.parentId ?: 4,
                            category = entry.value.category,
                            subcategoryId = entry.value.categoryId.toInt(),
                            title = entry.value.title,
                            date = entry.value.date,
                            time = entry.value.time,
                            price = entry.value.price,
                            photo = photoList.toList(),
                            fromCity = entry.value.city,
                            profile = entry.value.ping
                        )
                    } else {
                        Advert(
                            id = 0,
                            viewType = 2,
                            categoryId = 0,
                            category = "",
                            subcategoryId = 0,
                            title = "Откликов не найдено",
                            date = "",
                            time = "",
                            fromCity = "",
                            fromRegion = "",
                            fromPlace = "",
                            toCity = "",
                            toRegion = "",
                            toPlace = "",
                            payment = "",
                            description = "",
                            photo = emptyList()
                        )
                    }
                }
            }
            else{
                response = SFresponse as AdvertPingResponse.Failure
                if (response.message.contains("header not found")||response.message == "token is null") {
                    messageEvent.tryEmit("401")
                    null
                }
                else null
            }
        }

    private suspend fun getAdvertCatgoryList() =
        (repository.getAdvertCatgoryList() as? StringResponce.Success)?.let { response ->
            val l : ArrayList<Int> = ArrayList()
            response.categoriesList.forEach{
                l.add(it.toInt())
            }
            cachedAdvertCategories.tryEmit(l)
        }

    private suspend fun getOrderCount() =
        (repository.getOrderCountNews() as? IntIntResponce.Success)?.let { response ->
            cachedOrderNews.collect{
                if (it.isEmpty())
                    cachedOrderNews.tryEmit(response.categoriesList)
            }
        }

    private suspend fun getBuisnessSix() =
        (repository.getBussinessLast() as? businessLastResponce.Success)?.let { responce->
            cachedBussinessLast.collect{
                if (it.isEmpty())
                    cachedBussinessLast.tryEmit(responce.categoriesList)
            }
        }

    private suspend fun getPlaces() =
        cachedPlaces.tryEmit(mapOf(Pair(55.751951f, 37.574545f) to Advert(0,"",3,0,"ctegpory",0,0,"test dadafilter!","","","",null,"",emptyList(),"","","","","","","" ), Pair(55.750183f, 37.576155f) to Advert(0,"",3,0,"ctegporyyyy",0,0,"test dadadada!","","","",null,"",emptyList(),"","","","","","","" )))
        /*(repository.getPlaces() as? placesResponce.Success)?.let { responce->
            cachedPlaces.collect{
                if (it.isEmpty())
                    cachedPlaces.tryEmit(responce.categoriesList)
            }
        }*/

    private suspend fun updateFavoriteAdvertsFull() = viewModelScope.launch(Dispatchers.IO) {
        val list = getAdvertsFavoriteFull()
        if (authToken == ""){
            messageEvent.tryEmit("401")
        }

        if (list != null) {
            cachedAdvertFavorite.tryEmit(list)
        }
    }
    private suspend fun updateFavoriteOrdersFull() = viewModelScope.launch(Dispatchers.IO) {
        val list = getOrdersFavoriteFull()
        if (authToken == ""){
            messageEvent.tryEmit("401")
            cachedOrderFavorite.tryEmit(
                listOf(Advert(
                id = 0,
                viewType = 2,
                categoryId = 0,
                category = "",
                subcategoryId = 0,
                title = "Избранного не найдено",
                date = "",
                time = "",
                fromCity = "",
                fromRegion = "",
                fromPlace = "",
                toCity = "",
                toRegion = "",
                toPlace = "",
                payment = "",
                description = "",
                photo = emptyList())))
                return@launch
        }
        if (list != null) {
            cachedOrderFavorite.tryEmit(list)
        }
    }

    private suspend fun updatePingAdvertsFull() = viewModelScope.launch(Dispatchers.IO) {
        val list = getAdvertsPingFull()
        var isInProgress = false

        var reslist : ArrayList<Advert> = ArrayList()
        list?.forEach{
            it.profile.forEach{p->
                if (p.status=="PROGRESS") isInProgress=true
            }
        }
        list?.forEach{item->
            if (item.profile.size>1){
                for (i in 0 until item.profile.size){
                    var advert: Advert = item.copy()
                    advert.profile = emptyList()
                    advert.profile= listOf(item.profile[i])
                    if (item.profile[i].status!="PROGRESS"&&isInProgress) {
                        advert.profile.firstOrNull()?.firstName = ""
                        advert.profile.firstOrNull()?.lastName = ""
                        advert.profile.firstOrNull()?.phone = ""
                    }
                    //advert.id = (item.id.toString()+""+i.toString()).toInt()
                    reslist.add(advert)
                    }
            }
            else
                reslist.add(item)
        }

        cachedAdvertPing.tryEmit(reslist)
    }
    private suspend fun updatePingOrdersFull() = viewModelScope.launch(Dispatchers.IO) {
        val list = getOrdersPingFull()

        var reslist : ArrayList<Advert> = ArrayList()
        list?.forEach{item->
            if (item.profile.size>1){
                for (i in 0..item.profile.size-1){
                    var advert: Advert = item.copy()
                    advert.profile = emptyList()
                    advert.profile= listOf(item.profile[i])
                    //advert.id = (item.id.toString()+""+i.toString()).toInt()
                    reslist.add(advert)
                }
            }
            else
                reslist.add(item)
        }

        cachedOrderPing.tryEmit(reslist)
    }

    private suspend fun updateCategoryAdvertsUC(
    updateCache: Boolean = false,
    categoryId: String = "0",
    userId: String = "0"
    ) = viewModelScope.launch(Dispatchers.IO) {
        val list = getAdvertsFull() ?: return@launch

        cachedAdvert.tryEmit((list.filter {
            it.categoryId.toString()==categoryId&&it.userId==userId
        }).firstOrNull())
    }

    private suspend fun updateCategoryAdvertsFull(
        updateCache: Boolean = false,
        categoryId: Int = -1,
        advertId: String? = null
    ) = viewModelScope.launch(Dispatchers.IO) {
        val list = getAdvertsFull() ?: return@launch
println("dsdsdsdsd"+advertId+"|||"+list.toString())
        advertsSF.tryEmit(list)
        if (updateCache) {
            if (advertId == null) {
                cachedAdvertsSF.tryEmit(list.filter {
                    categoryId == it.subcategoryId || categoryId == it.categoryId ||
                            advertCategoriesFlow.value.find { acf ->
                                acf.id == categoryId
                            }?.parentId ?: 0 == it.subcategoryId
                })
            }
             else {
                val adv = list.filter { it.id.toString()==advertId}
                cachedAdvert.tryEmit(adv.firstOrNull())
             }
        }
    }
    private suspend fun updateCategoryOrdersFull(
        updateCache: Boolean = false,
        categoryId: Int = -1
    ) = viewModelScope.launch(Dispatchers.IO) {
        if (categoryId==-1){
            val list = getOrdersFull() ?: return@launch
            getChildrenID(cachedAdvertCategories.value).collect{ ids->
                cachedOrdersSF.tryEmit(list.filter {
                    ids.contains(it.categoryId) || ids.contains(it.subcategoryId)//categoryId == it.subcategoryId || categoryId == it.categoryId
                })
            }
        }
        else {
            getChildrenID(listOf(categoryId)).collect { ids ->
                val list = getOrdersFull() ?: return@collect

                ordersSF.tryEmit(list)
                if (updateCache)
                    cachedOrdersSF.tryEmit(list.filter {
                        ids.contains(it.categoryId) || ids.contains(it.subcategoryId)//categoryId == it.subcategoryId || categoryId == it.categoryId
                    })
            }
        }
    }

    private suspend fun getAdvertsFull() =
        (repository.getAdvertFullList() as? AdvertFullListResponse.Success)?.let { response ->
            response.advertMap.map { entry ->
                val photoList : ArrayList<String> = ArrayList()
                entry.value.photo.forEach{photoitem->
                    photoList.add(photoitem.value.replace("data:image/jpg;base64,", ""))
                }
                var isActiveBussiness = false
                if(entry.value.bussiness?.contains("NO_ACTIVE") == false){
                    isActiveBussiness = true
                }
                val l = listOf<optionDTO>(optionDTO("-1", "bussiness", "0", "ACTIVE"))
                var isPhotoActive = false
                entry.value.options.forEach{
                    if(it.option_id=="1"&&it.status=="ON"){
                        isPhotoActive=true
                    }
                }
                Advert(
                    id = entry.key.toInt(),
                    userId = entry.value.userId,
                    viewType = if(photoList.size==0||(!isPhotoActive&&!isActiveBussiness)) 1 else 0,
                    categoryId = advertCategoriesFlow.value.find {
                        it.id == entry.value.categoryId.toInt()
                    }?.parentId ?: 4,
                    category = entry.value.category,
                    subcategoryId = entry.value.categoryId.toInt(),
                    fourthLevelCategory = advertCategoriesFlow.value.find {
                        it.id == entry.value.categoryId.toInt()
                    }?.childId ?: 0,
                    title = entry.value.title,
                    date = entry.value.date,
                    time = entry.value.time,
                    price = entry.value.price,
                    photo = photoList.toList(),
                    fromCity = entry.value.city,
                    description = entry.value.description,
                    options = if(isActiveBussiness) (entry.value.options+l) else entry.value.options
                )
            }
        }

    private suspend fun updateAdverts(
        updateCache: Boolean = false,
        categoryId: Int = -1,
        advertId: String? = null
    ) = viewModelScope.launch(Dispatchers.IO) {
        val list = getAdverts() ?: return@launch
        advertsSF.tryEmit(list)

        if (updateCache) {
            if (advertId.isNullOrEmpty()) {
                cachedAdvertsSF.tryEmit(
                    if (categoryId != 0)
                        list.filter { categoryId == it.categoryId }
                    else list
                )
            } else {
                val adv = list.filter { it.id.toString()==advertId}

                cachedAdvert.tryEmit(adv.firstOrNull())
            }
        }
    }
    private suspend fun updateFeedbackAdverts() = viewModelScope.launch(Dispatchers.IO) {
        val list = getFeedbackAdverts() ?: return@launch
        cachedAdvertFeedbackPing.tryEmit(list)
    }

    private suspend fun getAdverts() =
        (repository.getAdvertList() as? AdvertListResponse.Success)?.let { response ->
            response.advertMap.map { entry ->
                val photoList : ArrayList<String> = ArrayList()
                entry.value.photo.forEach{photoitem->
                    photoList.add(photoitem.value.replace("data:image/jpg;base64,", ""))
                }

                Advert(
                    id = entry.key.toInt(),
                    viewType = if(photoList.size==0) 1 else 0,
                    categoryId = advertCategoriesFlow.value.find {
                        it.id == entry.value.categoryId.toInt()
                    }?.parentId ?: 4,
                    category = entry.value.category,
                    subcategoryId = entry.value.categoryId.toInt(),
                    description = entry.value.description,
                    title = entry.value.title,
                    date = entry.value.date,
                    time = entry.value.time,
                    price = entry.value.price,
                    photo = photoList.toList(),
                    fromCity = entry.value.city
                )
            }
        }

    private suspend fun getFeedbackAdverts() =
        (repository.getAdvertList() as? AdvertListResponse.Success)?.let { response ->
            var list : ArrayList<Advert> = ArrayList()
            response.advertMap.forEach { entry ->
                val photoList : ArrayList<String> = ArrayList()
                entry.value.photo.forEach{photoitem->
                    photoList.add(photoitem.value.replace("data:image/jpg;base64,", ""))
                }
                if (entry.value.ping.isNotEmpty()) {
                    entry.value.ping.forEach{
                        if (it.value!="REJECTED")
                            list.add(
                                Advert(
                                    id = entry.key.toInt(),
                                    viewType = 0,
                                    categoryId = advertCategoriesFlow.value.find {
                                        it.id == entry.value.categoryId.toInt()
                                    }?.parentId ?: 4,
                                    category = entry.value.category,
                                    subcategoryId = entry.value.categoryId.toInt(),
                                    title = entry.value.title,
                                    date = entry.value.date,
                                    time = entry.value.time,
                                    price = entry.value.price,
                                    description = "Отклик на ваше объявление в категории " + entry.value.category+" : "+entry.value.description,
                                    photo = photoList.toList(),
                                    ping = mapOf(it.toPair())
                                )
                            )
                    }
                }
            }
            list.toList()
        }

    private suspend fun updateOrders(
        updateCache: Boolean = false,
        categoryId: Int = -1
    ) = viewModelScope.launch(Dispatchers.IO) {
        val list = getOrders() ?: return@launch
        ordersSF.tryEmit(list)

        if (updateCache)
            cachedOrdersSF.tryEmit(
                if (categoryId!=0)
                    list.filter { categoryId == it.categoryId }
                else    {
                    list
                }
            )
    }
    private suspend fun updateFeedbackOrders() = viewModelScope.launch(Dispatchers.IO) {
        val list = getFeedbackOrders() ?: return@launch
        cachedOrderFeedbackPing.tryEmit(list)
    }

    private suspend fun getOrdersFull() =
        (repository.getOrderFullList() as? OrderFullListResponse.Success)?.let { response ->
            response.orderMap.map { entry ->
                val photoList : ArrayList<String> = ArrayList()
                entry.value.photo.forEach{photoitem->
                    photoList.add(photoitem.value.replace("data:image/jpg;base64,", ""))
                }
                var isActiveBussiness = false;
                if (entry.value.bussiness=="ACTIVE"){
                    isActiveBussiness = true;
                }
                Advert(
                    id = entry.key.toInt(),
                    userId = entry.value.userId,
                    viewType = if(entry.value.photo.isNotEmpty()) 0 else 1,
                    categoryId = advertCategoriesFlow.value.find {
                        it.id == entry.value.categoryId.toInt()
                    }?.parentId ?: 4,
                    category = entry.value.category,
                    subcategoryId = entry.value.categoryId.toInt(),
                    title = entry.value.description,
                    date = "${entry.value.fromDate}",
                    time = "${entry.value.fromTime}",
                    fromCity = "${entry.value.fromCity}",
                    fromRegion = "${entry.value.fromRegion}",
                    fromPlace = "${entry.value.fromPlace}",
                    toCity = "${entry.value.toCity}",
                    toRegion = "${entry.value.toRegion}",
                    toPlace = "${entry.value.toPlace}",
                    payment = entry.value.payment,
                    description = entry.value.description,
                    photo = photoList.toList(),
                    options = if (isActiveBussiness) listOf(optionDTO("-1", "bussiness", "0", "ACTIVE")) else emptyList()
                )
            }
        }

    private suspend fun getAdvertInfo(id: String) =
        (repository.getAdvertInfo(id) as? AdvertInfoResponse.Success)?.let { response ->
            val photoList : ArrayList<String> = ArrayList()
            response.photo.forEach{photoitem->
                photoList.add(photoitem.value.replace("data:image/jpg;base64,", ""))
            }
            var isActiveBussiness = true
            Advert(
                id = response.id.toInt(),
                userId = "0",
                viewType = if(response.photo.isNotEmpty()) 0 else 1,
                categoryId = advertCategoriesFlow.value.find {
                    it.id == response.categoryId.toInt()
                }?.parentId ?: 4,
                category = response.category,
                subcategoryId = response.categoryId.toInt(),
                title = response.description,
                date = "",
                time = "",
                fromCity = "${response.city}",
                fromRegion = "",
                fromPlace = "",
                toCity = "",
                toRegion = "",
                toPlace = "",
                payment = "card",
                description = response.description,
                photo = photoList.toList(),
                options = if (isActiveBussiness) listOf(optionDTO("-1", "bussiness", "0", "ACTIVE")) else emptyList()
            )
        }

    private suspend fun getOrdersMessagefailure() =
        (repository.getOrderList() as? OrderListResponse.Failure)?.let {
            it.message
        }
    private suspend fun getOrders() =
        (repository.getOrderList() as? OrderListResponse.Success)?.let { response ->
            response.orderMap.map { entry ->
                val photoList : ArrayList<String> = ArrayList()
                entry.value.photo.forEach{photoitem->
                    photoitem.value?.let { photoList.add(it.replace("data:image/jpg;base64,", "")) }
                }
                Advert(
                    id = entry.key.toInt(),
                    viewType = if (photoList.isEmpty()) 1 else 0,
                    categoryId = advertCategoriesFlow.value.find {
                        it.id == entry.value.categoryId.toInt()
                    }?.parentId ?: 4,
                    category = entry.value.category,
                    subcategoryId = entry.value.categoryId.toInt(),
                    title = entry.value.description,
                    date = entry.value.fromDate?:"",
                    time = entry.value.fromTime?:"",
                    fromCity = "${entry.value.fromCity}",
                    fromRegion = "${entry.value.fromRegion}",
                    fromPlace = "${entry.value.fromPlace}",
                    toCity = "${entry.value.toCity}",
                    toRegion = "${entry.value.toRegion}",
                    toPlace = "${entry.value.toPlace}",
                    payment = entry.value.payment,
                    description = entry.value.description,
                    photo = photoList.toList()
                )
            }
        }
    private suspend fun getFeedbackOrders() =
        (repository.getOrderList() as? OrderListResponse.Success)?.let { response ->
            var list : ArrayList<Advert> = ArrayList()
            response.orderMap.forEach { entry ->
                val photoList: ArrayList<String> = ArrayList()
                entry.value.photo.forEach { photoitem ->
                    photoitem.value?.let { photoList.add(it.replace("data:image/jpg;base64,", "")) }
                }
                if (entry.value.ping.isNotEmpty()) {
                    entry.value.ping.forEach{
                        if (it.value!="REJECTED")
                            list.add(Advert(
                                id = entry.key.toInt(),
                                viewType = 0,
                                categoryId = advertCategoriesFlow.value.find {
                                    it.id == entry.value.categoryId.toInt()
                                }?.parentId ?: 4,
                                category = entry.value.category,
                                subcategoryId = entry.value.categoryId.toInt(),
                                title = "Отклик #"+entry.key+it.key,//entry.value.description,
                                date = entry.value.date,
                                time = entry.value.time,
                                fromCity = "${entry.value.fromCity}",
                                fromRegion = "${entry.value.fromRegion}",
                                fromPlace = "${entry.value.fromPlace}",
                                toCity = "${entry.value.toCity}",
                                toRegion = "${entry.value.toRegion}",
                                toPlace = "${entry.value.toPlace}",
                                payment = entry.value.payment,
                                description = "Запрс на исполнение вашей заявки "+entry.value.description+" в категории " + entry.value.category,
                                photo = emptyList(),//photoList.toList()
                                ping = mapOf(it.toPair())
                            ))
                    }
                }
            }
            list.toList()
        }

    private fun updateFilterThirdLevelCategories(categoryId: Int) =
        viewModelScope.launch(Dispatchers.IO) {
            advertCategoriesFlow.value.filter { it.level == 3 && it.parentId == categoryId }
                .let { cats ->
                    cachedFilterCategoriesSF.tryEmit(
                        cats.map { ServiceType(it.id, it.name) }
                    )
                }
        }

    fun clearCachedAdverts() = viewModelScope.launch(Dispatchers.IO) {
        cachedAdvertsSF.tryEmit(emptyList())
    }

    val profileFlow = repository.profileFlow()
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val profileAdvertsFlow = channelFlow {
        advertCategoriesFlow.collectLatest { advertCats ->
            val firstLevelCatsDeferred = async { advertCats.filter { it.level == 1 } }
            //val thirdLevelCatsDeferred = advertCats.filter { it.level == 3 }

            val list = mutableListOf<ProfileRvItem>()
            var index = 0L
            val oders = getOrders()
            val adverts = getAdverts()
            firstLevelCatsDeferred.await().forEach {
                list.add(
                    ProfileRvItem(
                        id = index++,
                        viewType = 1,
                        title = it.name,
                        realId = it.id,
                        categoryId = it.parentId
                    )
                )
                if (oders?.isNotEmpty() == true) {
                    oders.forEach { order ->
                        if (getFirstLevelParentID(order.categoryId) == it.id) {
                            list.add(
                                ProfileRvItem(
                                    id = index++,
                                    viewType = 2,
                                    title = order.title + " (заказ)",
                                    realId = order.id,
                                    categoryId = order.subcategoryId//advert.categoryId
                                )
                            )
                        }
                    }
                }
                adverts?.filter { order ->
                    /*secondLevelCategoriesFlow.value
                        .find { cat -> cat.id == order.categoryId }?.parentId == it.id*/
                    getFirstLevelParentID(order.categoryId) == it.id
                }?.forEach { advert ->
                    list.add(
                        ProfileRvItem(
                            id = index++,
                            viewType = 2,
                            title = advert.title,
                            realId = advert.id,
                            categoryId = advert.subcategoryId//advert.categoryId
                        )
                    )
                }
                list.add(
                    ProfileRvItem(
                        id = index++,
                        viewType = 3,
                        categoryId = it.id
                    )
                )
            }
            send(list)
        }
    }.shareIn(viewModelScope, SharingStarted.WhileSubscribed(0L, 0L), 1)

    val userOrdersAdvertsFlow = channelFlow {
        advertCategoriesFlow.collectLatest { advertCats ->
            val firstLevelCatsDeferred = async { advertCats.filter { it.level == 1 } }
            //val thirdLevelCatsDeferred = advertCats.filter { it.level == 3 }

            val list = mutableListOf<Advert>()
            getOrders()?.forEach{
                list.add(it)
            }
            getAdverts()?.forEach{
                list.add(it)
            }
            /*firstLevelCatsDeferred.await().forEach {
                getOrders()?.filter { order ->
                    secondLevelCategoriesFlow.value
                        .find { cat -> cat.id == order.categoryId }?.parentId == it.id
                }?.forEach { advert ->
                    list.add(
                        advert
                    )
                }
                getAdverts()?.filter { order ->
                    secondLevelCategoriesFlow.value
                        .find { cat -> cat.id == order.categoryId }?.parentId == it.id
                }?.forEach { advert ->
                    list.add(
                        advert
                    )
                }
            }*/
            send(list)
        }
    }.shareIn(viewModelScope, SharingStarted.WhileSubscribed(0L, 0L), 1)


    fun updateProfile() = viewModelScope.launch(Dispatchers.IO) {
        when (val result = repository.updateProfile()) {
            is UpdateProfileResponse.Success -> Unit
            is UpdateProfileResponse.Failure -> {
                if (result.message.contains("Authorization header")) {
                    messageEvent.tryEmit(
                        "401"//"Ошибка!" + app.getString(R.string.user_not_found)
                    )
                    logout()
                } else if (result.message.contains("Authorization header is expired"))
                    "Ошибка! Сессия авторизации истекла!"
                //TODO need to re-authorize to obtain token
            }
        }
    }

    private suspend fun getProfileShort(id: String){
        (repository.getProfile(id) as? ProfileShortResponse.Success)?.let { response ->
            val p = ProfileShort(response.profile.firstName, response.profile.lastName, response.profile.phone, response.profile.location)
            cachedProfile.tryEmit(p)
            p
        }
    }

    fun getProfile(id: String) = viewModelScope.launch(Dispatchers.IO) {
        getProfileShort(id)
    }

    fun editProfile(
        name: String = "",
        telNumber: String = "",
        email: String = "",
        cityArea: String = "",
        paymentCard: String = "",
        avatar: List<String>? = null
    ) = viewModelScope.launch(Dispatchers.IO) {
        repository.editProfile(name, telNumber, email, cityArea, paymentCard, avatar)
        delay(1000)
        updateProfile()
    }

    fun logout() = viewModelScope.launch(Dispatchers.IO) {
        repository.logout()
        logoutSF.tryEmit("")
    }

    private suspend fun getTarif(){
        (repository.getTarif() as? TarifResponce.Success)?.let { response ->
            var listt : ArrayList<TarifDTO> = ArrayList()
            response.tarif.forEach{
                listt.add(it.value)
            }
            cachedTarif.tryEmit(listt.toList())
            listt
        }
    }
    private suspend fun getNews(){
        (repository.getNews() as? NewsResponse.Success)?.let { response ->
            var listt : ArrayList<NewsDTO> = ArrayList()
            response.tarif.forEach{
                listt.add(it.value)
            }
            cachedNews.tryEmit(listt.toList())
            listt
        }
    }

    private suspend fun getNotification(){
        (repository.getNotice() as? NoticeResponce.Success)?.let { response ->
            cachedNotifications.tryEmit(response.notice)
            response.notice
        }
    }
    fun deleteNotification(id: String) =
        viewModelScope.launch(Dispatchers.IO) {
            when (repository.deleteNotice(id).message) {
                "Success" -> {
                    getNotification()
                }
                else -> messageEvent.tryEmit("Ошибка при удалении оповещения!")
            }
        }

    fun getTarifs() = viewModelScope.launch (Dispatchers.IO){
        getTarif()
    }

    fun geetNews() = viewModelScope.launch (Dispatchers.IO){
        getNews()
    }

    fun getNotice() = viewModelScope.launch(Dispatchers.IO) {
        getNotification()
    }

    fun getChildrenID(categoryId : List<Int>) = channelFlow {
        advertCategoriesFlow.collect() {
            val ids : ArrayList<Int> = ArrayList()
            ids.addAll(categoryId)
            it.forEach{cat ->
                if (categoryId.contains(cat.parentId)||ids.contains(cat.parentId)){
                    ids.add(cat.id)
                }
            }
            send(ids)
        }
    }
    fun getParentID(categoryId : Int) = channelFlow {
        advertCategoriesFlow.collect() {
            var id : Int = 1
            it.forEach{cat ->
                if (cat.id==categoryId){
                    id = cat.parentId
                }
            }
            send(id)
        }
    }
    suspend fun getFirstLevelParentID(categoryId: Int) : Int {
        var id: Int = categoryId
            val list = advertCategoriesFlow.value
        /*list.forEach { cat ->
            if (categoryId == cat.id) {
                id=cat.id
                list.forEach { cat2 ->
                    if (cat.parentId == cat2.id) {
                        id=cat2.id
                        list.forEach { cat3 ->
                            if (cat2.parentId == cat3.id) {
                                return cat3.id
                            }
                        }
                    }
                }
            }
        }*/
        while (id !in 0..3){
            list.forEach{
                if (it.id==id) {
                    id = it.parentId
                }
            }
        }
        return id
    }

    fun getSecondLevelParentID(categoryId: Int) : Int {
        var id: Int = categoryId
        val list = advertCategoriesFlow.value
        while (id !in arrayOf(4, 8, 12, 13, 14, 24) ){
            list.forEach{
                if (it.id==id) {
                    id = it.parentId
                }
            }
        }
        return id
    }

    fun addAdvertScreenCategoriesFlow(categoryId: Int) = channelFlow {
        advertCategoriesFlow.collectLatest { advertCats ->
            val secondLevelCats = advertCats.filter { it.level == 2 && it.parentId == categoryId }
            val thirdLevelCats = advertCats.filter { it.level == 3 }
            val fourthLevelCats = advertCats.filter { it.level == 4 }
            val list = mutableListOf<SelectorCategory>()
            var index = 0
            secondLevelCats.forEach { advertCat ->
                list.add(advertCat.run { SelectorCategory(index++, id, level, name, parentId) })
                thirdLevelCats
                    .filter { it.parentId == advertCat.id }
                    .forEach {AC ->
                        list.add(AC.run { SelectorCategory(index++, id, level, name, parentId) })
                        fourthLevelCats
                            .filter { it.parentId == AC.id }
                            .forEach {
                                list.add(it.run { SelectorCategory(index++, id, level, name, parentId) })
                            }
                    }
            }
            send(list)
        }
    }

    fun addAdvertScreenCategoriesFlowAll() = channelFlow {
        advertCategoriesFlow.collectLatest { advertCats ->
            val secondLevelCats = advertCats.filter { it.level == 2}
            val thirdLevelCats = advertCats.filter { it.level == 3 }
            val fourthLevelCats = advertCats.filter { it.level == 4 }
            val list = mutableListOf<SelectorCategory>()
            var index = 0
            secondLevelCats.forEach { advertCat ->
                list.add(advertCat.run { SelectorCategory(index++, id, level, name, parentId) })
                thirdLevelCats
                    .filter { it.parentId == advertCat.id }
                    .forEach {advCat2 ->
                        list.add(advCat2.run { SelectorCategory(index++, id, level, name, parentId) })
                        fourthLevelCats
                            .filter { it.parentId == advCat2.id }
                            .forEach { advCat3 ->
                                list.add(advCat3.run { SelectorCategory(index++, id, level, name, parentId) })
                            }
                    }
            }
            send(list)
        }
    }

    fun addAdvertScreenCategoriesFlowFourthLevel(categoryId: Int) = channelFlow {
        advertCategoriesFlow.collectLatest { advertCats ->
            val secondLevelCats = advertCats.filter { it.level == 2}
            val thirdLevelCats = advertCats.filter { it.level == 3 }
            val fourthLevelCats = advertCats.filter { it.level == 4 }
            val list = mutableListOf<SelectorCategory>()
            var index = 0
            secondLevelCats.forEach { advertCat ->
                thirdLevelCats
                    .filter { it.parentId == advertCat.id }
                    .forEach {advCat2 ->
                        fourthLevelCats
                            .filter { it.parentId == advCat2.id && advCat2.id==categoryId}
                            .forEach { advCat3 ->
                                list.add(advCat3.run { SelectorCategory(index++, id, level, name, parentId) })
                            }
                    }
            }
            send(list)
        }
    }

    fun createAdvert(
        ctx: Context?,
        title: String,
        price: String,
        city: String,
        description: String,
        categoryId: String,
        photos: List<String>,
        options: List<String>
    ) = viewModelScope.launch(Dispatchers.IO) {
        when (val result = repository.createAdvert(ctx, title, price, city, description, categoryId, photos, options)) {
            is AdvertCreateResponse.Success -> {
                if (result.id != null)
                    lastAdvertAdded=result.id
                    messageEvent.tryEmit("Объявление добавлено!")
            }
            is AdvertCreateResponse.Failure -> {
                messageEvent.tryEmit("Ошибка при создании объявления!$AdvertCreateResponse")
            }
        }
    }

    fun editAdvert(id: String,
                    title: String,
                   price: String,
                   description: String,
                   photos: List<String>?,
                    options: List<String>)=
        viewModelScope.launch(Dispatchers.IO) {
            when (val result = repository.editAdvert(id, title, price, description, photos, options)) {
                is AdvertCreateResponse.Success -> {
                        messageEvent.tryEmit("Объявление изменено!")
                }
                is AdvertCreateResponse.Failure -> {
                    messageEvent.tryEmit("Ошибка при обновлении объявления!")
                }
            }
        }

    fun createOrder(
        ctx: Context? = null,
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
    ) = viewModelScope.launch(Dispatchers.IO) {
        val result = repository.createOrder(
            ctx = ctx,
            category = category,
            fromCity = fromCity,
            fromRegion = fromRegion,
            fromPlace = fromPlace,
            fromDateTime = fromDateTime,
            toCity = toCity,
            toRegion = toRegion,
            toPlace = toPlace,
            description = description,
            name = name,
            phone = phone,
            payment = payment,
            photos = photos
        )
        when (result) {
            is AdvertCreateResponse.Success -> {
                if (result.id != null)
                    messageEvent.tryEmit("Объявление добавлено!")
            }
            is AdvertCreateResponse.Failure -> {
                messageEvent.tryEmit("Ошибка при создании объявления!"+AdvertCreateResponse.Failure.toString())
            }
        }
    }

    fun editOrder(
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
    ) = viewModelScope.launch(Dispatchers.IO) {
        val result = repository.editOrder(
            orderId = orderId,
            category = category,
            fromCity = fromCity,
            fromRegion = fromRegion,
            fromPlace = fromPlace,
            fromDateTime = fromDateTime,
            toCity = toCity,
            toRegion = toRegion,
            toPlace = toPlace,
            description = description,
            name = name,
            phone = phone,
            payment = payment
        )
        when (result) {
            is AdvertCreateResponse.Success -> {
                if (result.id != null)
                    messageEvent.tryEmit("Объявление !")
            }
            is AdvertCreateResponse.Failure -> {
                messageEvent.tryEmit("Ошибка при создании объявления!")
            }
        }
    }

    fun deleteAdvert(isOrder: Boolean, id: Int, position: Int) =
        viewModelScope.launch(Dispatchers.IO) {
            when (repository.deleteAdvert(isOrder, id.toString()).message) {
                "Success deleted." -> {
                    messageEvent.tryEmit("Объявление успешно удалено!")
                    if(position>=0) deletedAdvertPosition.tryEmit(position)
                    /*if (isOrder)
                    updateOrders()
                else
                    updateAdverts()*/
                }
                else -> messageEvent.tryEmit("Ошибка при удалении объявления!")
            }
        }

    fun cafApplyPhotoByUri(uri: Uri) = viewModelScope.launch(Dispatchers.IO) {
        val newValue = cafTempPhotoUris.value.run {
            if (this.second.size<=5) {
                Pair(
                    first,
                    second.toMutableList().apply { add(uri) }
                )
            }else
                this
        }
        cafTempPhotoUris.tryEmit(newValue)
    }
    fun applyProfilePhotoByUri(uri: Uri) = viewModelScope.launch(Dispatchers.IO) {
        val newValue = profilePhotoUri.value.run {
            Pair(
                first+1,
                second.toMutableList().apply { add(uri) }
            )
        }
        profilePhotoUri.tryEmit(newValue)
    }

    fun cafRemoveCurrentPhoto() = viewModelScope.launch(Dispatchers.IO) {
        val position = cafTempPhotoUris.value.first
        val newValue = cafTempPhotoUris.value.run {
            Pair(
                first,
                second.toMutableList().apply { removeAt(position) }
            )
        }
        cafTempPhotoUris.tryEmit(newValue)
    }

    fun cafPrevPhoto() = viewModelScope.launch(Dispatchers.IO) {
        val position = cafTempPhotoUris.value.first
        if (position > 0) {
            val newValue = cafTempPhotoUris.value.run { Pair(first-1, second.toMutableList()) }
            cafTempPhotoUris.tryEmit(newValue)
        }
    }

    fun cafNextPhoto() = viewModelScope.launch(Dispatchers.IO) {
        val position = cafTempPhotoUris.value.first
        cafTempPhotoUris.value.second.getOrNull(position) ?: return@launch
        val newValue = cafTempPhotoUris.value.run { Pair(first+1, second.toMutableList()) }
        cafTempPhotoUris.tryEmit(newValue)
    }

    fun applyAdfPhotos(list: List<String>) = viewModelScope.launch(Dispatchers.IO) {
        val bitmapList = list.map {
            val byteArray = Base64.decode(it, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
        }
        adfTempPhotoUris.tryEmit(Pair(0, bitmapList.toList()))
    }

    fun adfPrevPhoto() = viewModelScope.launch(Dispatchers.IO) {
        val position = adfTempPhotoUris.value.first
        if (position > 0) {
            val newValue = adfTempPhotoUris.value.run { Pair(first-1, second.toList()) }
            adfTempPhotoUris.tryEmit(newValue)
        }
    }

    fun adfNextPhoto() = viewModelScope.launch(Dispatchers.IO) {
        val position = adfTempPhotoUris.value.first
        adfTempPhotoUris.value.second.getOrNull(position) ?: return@launch
        val newValue = adfTempPhotoUris.value.run { Pair(first+1, second.toList()) }
        adfTempPhotoUris.tryEmit(newValue)
    }
}