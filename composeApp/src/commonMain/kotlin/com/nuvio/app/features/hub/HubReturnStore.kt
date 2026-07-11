package com.nuvio.app.features.hub

import com.nuvio.app.features.iptv.IptvChannel

object HubReturnStore {
    var subScreen: String = "Hub"
}

object MultiWindowPushStore {
    var pendingChannel: IptvChannel? = null
}
