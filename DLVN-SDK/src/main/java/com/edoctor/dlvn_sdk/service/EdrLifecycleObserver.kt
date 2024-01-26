package com.edoctor.dlvn_sdk.service

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import com.edoctor.dlvn_sdk.store.AppStore

interface EdrLifecycleObserver: LifecycleObserver {
    fun registerObserver() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    fun unregisterObserver() {
        ProcessLifecycleOwner.get().lifecycle.removeObserver(this)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onForegroundStart() {
        AppStore.isAppInForeground = true
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onForegroundStop() {
        AppStore.isAppInForeground = false
    }
}