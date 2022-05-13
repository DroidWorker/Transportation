package com.app.transportation.ui

import android.app.Application
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
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

    val advertCategoriesFlow = repository.advertCategoriesFlow()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val advertsSF = MutableStateFlow(emptyList<Advert>())
    val ordersSF = MutableStateFlow(emptyList<Advert>())

    private val prefs: SharedPreferences by inject(named("MainSettings"))

    private var authToken: String?
        get() = prefs.getString("authToken", null)
        set(value) = prefs.edit { putString("authToken", value) }


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

    val cachedAdvertsSF = MutableStateFlow(emptyList<Advert>())
    val cachedOrdersSF = MutableStateFlow(emptyList<Advert>())
    val cachedFilterCategoriesSF = MutableStateFlow(emptyList<ServiceType>())
    val cachedStatic = MutableStateFlow<String>("loading...")

    val cachedAdvertFavorite = MutableStateFlow(emptyList<Advert>())
    val cachedOrderFavorite = MutableStateFlow(emptyList<Advert>())

    val cachedAdvert = MutableStateFlow<Advert?>(null)
    val cachedOrder = MutableStateFlow<Advert?>(null)

    val cafTempPhotoUris = MutableStateFlow(Pair(0, mutableListOf<Uri>()))
    val adfTempPhotoUris = MutableStateFlow(Pair(0, emptyList<Bitmap>()))

    val isCustomer = MutableStateFlow(
        prefs.getBoolean("isCustomer", false).takeIf { prefs.contains("isCustomer") }
    )

    val deletedAdvertPosition = MutableSharedFlow<Int>(extraBufferCapacity = 1)

    var dateTime = ""

    init {
        updateMainFragmentData()
    }

    private suspend fun updateStatic(type: String) = viewModelScope.launch(Dispatchers.IO) {
        println("steeep01")
        val result: String = getStaticDatas(type) ?: return@launch
        cachedStatic.tryEmit(result)
    }

    private suspend fun getStaticDatas(type: String) =
        (repository.getStaticData(type) as? StaticDateResponse.Success)?.let { response ->
            println("steeep03")
            response.text
        }

    private suspend fun getSearched(str: String) =
        (repository.search(str) as? SearchResponse.Success)?.let { response ->
            response.resMap.map { entry ->
                Advert(
                    id = entry.key.toInt(),
                    viewType = 0,
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
        println("sresult = $list")

        if (updateCache)
            cachedSearchResult.tryEmit(list)
    }

    fun updateMainFragmentData() = viewModelScope.launch(Dispatchers.IO) {
        when (val result = repository.updateAdvertCategories()) {
            null -> Unit
            is AuthTokenNotFoundException -> {
                messageEvent.tryEmit("Проблемы с токеном. Переавторизуйтесь.")
            }
            is UnknownHostException -> {
                messageEvent.tryEmit("Проблемы с интернетом или сервером")
            }
            else -> {
                messageEvent.tryEmit(result.localizedMessage.toString())
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

    fun getCategoryOrders(categoryId: Int) = viewModelScope.launch(Dispatchers.IO) {
        updateOrders(true, categoryId)

        updateFilterThirdLevelCategories(categoryId)
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
                messageEvent.tryEmit("Ошибка! " + result.message)
                println(result.message)
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
                messageEvent.tryEmit("Ошибка! " + result.message)
                println(result.message + orderId)
            }
        }
    }

    private suspend fun getOrdersFavoriteFull() =
        (repository.getOrderFullList() as? OrderListResponse.Success)?.let { response ->
            response.orderMap.map { entry ->
                if (entry.key!="empty") {
                    val photoList: ArrayList<String> = ArrayList()
                    entry.value.photo.forEach { photoitem ->
                        photoList.add(photoitem.value.replace("data:image/jpg;base64,", ""))
                    }
                    Advert(
                        id = entry.key.toInt(),
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
                        payment = entry.value.payment,
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
        (repository.getAdvertFullList() as? AdvertListResponse.Success)?.let { response ->
            response.advertMap.map { entry ->
                if (entry.key != "empty") {
                    val photoList: ArrayList<String> = ArrayList()
                    entry.value.photo.forEach { photoitem ->
                        photoList.add(photoitem.value.replace("data:image/jpg;base64,", ""))
                    }
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

    private suspend fun updateFavoriteAdvertsFull() = viewModelScope.launch(Dispatchers.IO) {
        val list = getAdvertsFavoriteFull() ?: return@launch
        println("adverts Favorite fuuuuuuuuuuul = $list")

        cachedAdvertFavorite.tryEmit(list)
    }
    private suspend fun updateFavoriteOrdersFull() = viewModelScope.launch(Dispatchers.IO) {
        val list = getOrdersFavoriteFull() ?: return@launch
        println("orders Favorite fuuuuuuuuuuul = $list")

        cachedOrderFavorite.tryEmit(list)
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
                println(result.message)
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
                println(result.message)
            }
        }
    }

    private suspend fun updateCategoryAdvertsFull(
        updateCache: Boolean = false,
        categoryId: Int = -1
    ) = viewModelScope.launch(Dispatchers.IO) {
        val list = getAdvertsFull() ?: return@launch
        println("adverts fuuuuuuuuuuul = $list")

        advertsSF.tryEmit(list)

        if (updateCache)
            cachedAdvertsSF.tryEmit(list.filter {
                categoryId == it.subcategoryId || categoryId == it.categoryId
            })
    }
    private suspend fun updateCategoryOrdersFull(
        updateCache: Boolean = false,
        categoryId: Int = -1
    ) = viewModelScope.launch(Dispatchers.IO) {
        getChildrenID(categoryId).collect { ids ->
            val list = getOrdersFull() ?: return@collect
            println("orders fuuuuuuuuuuul = $list")

            ordersSF.tryEmit(list)

            if (updateCache)
                cachedOrdersSF.tryEmit(list.filter {
                    ids.contains(it.categoryId)||ids.contains(it.subcategoryId)//categoryId == it.subcategoryId || categoryId == it.categoryId
                })
        }
    }

    private suspend fun getAdvertsFull() =
        (repository.getAdvertFullList() as? AdvertListResponse.Success)?.let { response ->
            response.advertMap.map { entry ->
                val photoList : ArrayList<String> = ArrayList()
                entry.value.photo.forEach{photoitem->
                    photoList.add(photoitem.value.replace("data:image/jpg;base64,", ""))
                }
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
                    photo = photoList.toList()
                )
            }
        }

    private suspend fun updateAdverts(
        updateCache: Boolean = false,
        categoryId: Int = -1
    ) = viewModelScope.launch(Dispatchers.IO) {
        val list = getAdverts() ?: return@launch
        println("adverts = $list")
        advertsSF.tryEmit(list)

        if (updateCache)
            cachedAdvertsSF.tryEmit(
                list.filter { categoryId == it.categoryId }
            )
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
                    photo = photoList.toList()
                )
            }
        }

    private suspend fun updateOrders(
        updateCache: Boolean = false,
        categoryId: Int = -1
    ) = viewModelScope.launch(Dispatchers.IO) {
        val list = getOrders() ?: return@launch
        println("orders = $list")
        ordersSF.tryEmit(list)

        if (updateCache)
            cachedAdvertsSF.tryEmit(
                list.filter { categoryId == it.categoryId }
            )
    }

    private suspend fun getOrdersFull() =
        (repository.getOrderFullList() as? OrderListResponse.Success)?.let { response ->
            response.orderMap.map { entry ->
                val photoList : ArrayList<String> = ArrayList()
                entry.value.photo.forEach{photoitem->
                    photoList.add(photoitem.value.replace("data:image/jpg;base64,", ""))
                }
                Advert(
                    id = entry.key.toInt(),
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
                    payment = entry.value.payment,
                    description = entry.value.description,
                    photo = photoList.toList()
                )
            }
        }

    private suspend fun getOrders() =
        (repository.getOrderList() as? OrderListResponse.Success)?.let { response ->
            response.orderMap.map { entry ->
                val photoList : ArrayList<String> = ArrayList()
                entry.value.photo.forEach{photoitem->
                    photoList.add(photoitem.value.replace("data:image/jpg;base64,", ""))
                }
                Advert(
                    id = entry.key.toInt(),
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
                    payment = entry.value.payment,
                    description = entry.value.description,
                    photo = photoList.toList()
                )
            }
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
                getOrders()?.filter { order ->
                    secondLevelCategoriesFlow.value
                        .find { cat -> cat.id == order.categoryId }?.parentId == it.id
                }?.forEach { advert ->
                    list.add(
                        ProfileRvItem(
                            id = index++,
                            viewType = 2,
                            title = advert.title + " (заказ)",
                            realId = advert.id,
                            categoryId = advert.subcategoryId//advert.categoryId
                        )
                    )
                }
                getAdverts()?.filter { order ->
                    secondLevelCategoriesFlow.value
                        .find { cat -> cat.id == order.categoryId }?.parentId == it.id
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
            var index = 0L
            firstLevelCatsDeferred.await().forEach {
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
            }
            send(list)
        }
    }.shareIn(viewModelScope, SharingStarted.WhileSubscribed(0L, 0L), 1)


    fun updateProfile() = viewModelScope.launch(Dispatchers.IO) {
        //messageEvent.tryEmit("Обновление профиля...")
        when (val result = repository.updateProfile()) {
            is UpdateProfileResponse.Success -> Unit
                /*messageEvent.tryEmit("Профиль обновлен!")*/
            is UpdateProfileResponse.Failure -> {
                when (result.message) {
                    "Authorization header not found" -> messageEvent.tryEmit(
                        "Ошибка!" + app.getString(R.string.user_not_found)
                    )
                    "Authorization header is expired" -> messageEvent.tryEmit(
                        "Ошибка! Сессия авторизации истекла!"
                    )
                }
                //TODO need to re-authorize to obtain token
                logout()
            }
        }
    }

    fun editProfile(
        name: String = "",
        telNumber: String = "",
        email: String = "",
        cityArea: String = "",
        paymentCard: String = ""
    ) = viewModelScope.launch(Dispatchers.IO) {
        repository.editProfile(name, telNumber, email, cityArea, paymentCard)
        delay(1000)
        updateProfile()
    }

    fun logout() = viewModelScope.launch(Dispatchers.IO) {
        repository.logout()
        logoutSF.tryEmit("")
    }

    suspend fun getChildrenID(categoryId : Int) = channelFlow {
        advertCategoriesFlow.collect() {
            var ids : ArrayList<Int> = ArrayList()
            ids.add(categoryId)
            it.forEach{cat ->
                if (cat.parentId==categoryId||ids.contains(cat.parentId)){
                    ids.add(cat.id)
                }
            }
            send(ids)
        }
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

    fun updateCategories(id: Int) = viewModelScope.launch(Dispatchers.IO) {

    }

    fun createAdvert(
        title: String,
        price: String,
        description: String,
        categoryId: String,
        photos: List<String>
    ) = viewModelScope.launch(Dispatchers.IO) {
        when (val result = repository.createAdvert(title, price, description, categoryId, photos)) {
            is AdvertCreateResponse.Success -> {
                if (result.id != null)
                    messageEvent.tryEmit("Объявление добавлено!")
            }
            is AdvertCreateResponse.Failure -> {
                messageEvent.tryEmit("Ошибка при создании объявления!")
            }
        }
    }

    fun editAdvert(title: String,
                   price: String,
                   description: String,
                   categoryId: String,
                   photos: List<String>)=
        viewModelScope.launch(Dispatchers.IO) {
            when (val result = repository.editAdvert(title, price, description, categoryId, photos)) {
                is AdvertCreateResponse.Success -> {
                    if (result.id != null)
                        messageEvent.tryEmit("Объявление добавлено!")
                }
                is AdvertCreateResponse.Failure -> {
                    messageEvent.tryEmit("Ошибка при создании объявления!")
                }
            }
        }

    fun createOrder(
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
    ) = viewModelScope.launch(Dispatchers.IO) {
        val result = repository.createOrder(
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
                    messageEvent.tryEmit("Объявление добавлено!")
            }
            is AdvertCreateResponse.Failure -> {
                messageEvent.tryEmit("Ошибка при создании объявления!")
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
                    messageEvent.tryEmit("Объявление добавлено!")
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
                    deletedAdvertPosition.tryEmit(position)
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
            Pair(
                first,
                second.toMutableList().apply { add(uri) }
            )
        }
        cafTempPhotoUris.tryEmit(newValue)
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
        adfTempPhotoUris.tryEmit(0 to bitmapList)
    }

    fun adfPrevPhoto() = viewModelScope.launch(Dispatchers.IO) {
        val position = adfTempPhotoUris.value.first
        if (position > 0) {
            val newValue = adfTempPhotoUris.value.run { Pair(first-1, second.toMutableList()) }
            adfTempPhotoUris.tryEmit(newValue)
        }
    }

    fun adfNextPhoto() = viewModelScope.launch(Dispatchers.IO) {
        val position = adfTempPhotoUris.value.first
        adfTempPhotoUris.value.second.getOrNull(position) ?: return@launch
        val newValue = adfTempPhotoUris.value.run { Pair(first+1, second.toMutableList()) }
        adfTempPhotoUris.tryEmit(newValue)
    }
}