package com.mosi.mosc.ai_weather.been

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ALLDataBeen(val header:HeaderBeen,val payload: PayloadBeen):Parcelable
