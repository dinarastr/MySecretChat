package ru.yandexpraktikum.blechat.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ru.yandexpraktikum.blechat.data.repository.ChatRepositoryImpl
import ru.yandexpraktikum.blechat.domain.repository.ChatRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface RepositoryModule {
    
    @Binds
    @Singleton
    fun bindChatRepository(
        chatRepositoryImpl: ChatRepositoryImpl
    ): ChatRepository
}