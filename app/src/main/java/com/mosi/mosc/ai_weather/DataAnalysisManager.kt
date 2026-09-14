package com.mosi.mosc.ai_weather

import android.util.Log
import com.google.gson.Gson
import com.mosi.mosc.ai_weather.been.AIBaseBeen
import com.mosi.mosc.ai_weather.been.ALLDataBeen
import com.mosi.mosc.ai_weather.been.CardBaseInfo
import java.util.Date

object DataAnalysisManager {
    private val gson: Gson by lazy { Gson() }

    private val TAG = "DataAnalysisManager"

    private var mALLDataBeen: ALLDataBeen? = null

    fun pauseData(string: String){
        mALLDataBeen = gson.fromJson(string, ALLDataBeen::class.java)
    }

    /**
     * data 抽取规则
     *
     * 先按 tasks[].taskId 区分任务。
     * 再按每个任务的 bindings 读取 weatherObjects 和 time。
     * granularity=day/multi_day 时，在 forecast 中按 predictDate 匹配 startTime 和 endTime 对应的自然日。
     * granularity=instant/period 时，在 hourly 中按 date 匹配时间窗口。
     * 实时天气对象从 condition、aqi、liveIndex、alert、currentLimit 等顶层区域读取。
     * 生活指数必须通过 liveIndex[].code.code 找到对应记录，不能依赖数组下标。
     * 不应展示没有被 slots 明确引用的数据，即使这些数据存在于全量 data 中。
     */
    fun requestShowInfo(): HashMap<String, Any?>{
        val dataPauseResult = HashMap<String, Any?>()
        mALLDataBeen?.let {allDataBeen->
            allDataBeen.payload.data.let {aiBaseBeen->
                val baseCardInfo = CardBaseInfo(
                    icon = aiBaseBeen.bizData.condition.icon,
                    location = aiBaseBeen.bizData.city.name,
                    showTime = aiBaseBeen.bizData.condition.updateTime,
                    currentTemp = aiBaseBeen.bizData.condition.temp,
                    highTemp = aiBaseBeen.bizData.condition.temp,
                    lowTemp = aiBaseBeen.bizData.condition.temp,
                    tempInfo = aiBaseBeen.bizData.condition.tips
                )
                var compareCarInfo:CardBaseInfo? = null

                aiBaseBeen.slots.bindings.forEach { bindingsBeen->
//                    val startTime = Date.parse(bindingsBeen.time.startTime)
//                    val endTime = Date.parse(bindingsBeen.time.endTime)
                    when (bindingsBeen.time.role) {
                        "target"-> {
                            baseCardInfo.showTime = bindingsBeen.time.expression
                        }
                        "baseline"-> {
                            baseCardInfo.showTime = bindingsBeen.time.expression
                        }
                        "comparison"-> {
                            compareCarInfo = CardBaseInfo(
                                icon = aiBaseBeen.bizData.condition.icon,
                                location = aiBaseBeen.bizData.city.name,
                                showTime = aiBaseBeen.bizData.condition.updateTime,
                                currentTemp = aiBaseBeen.bizData.condition.temp,
                                highTemp = aiBaseBeen.bizData.condition.temp,
                                lowTemp = aiBaseBeen.bizData.condition.temp,
                                tempInfo = aiBaseBeen.bizData.condition.tips
                            )
                            baseCardInfo.showTime = bindingsBeen.time.expression
                        }
                    }
                    //这里需要判断句
                    bindingsBeen.weatherObjects.forEach {
                        when(it){
                            "temperature"->{
                                //温度
                                when(bindingsBeen.time.granularity){
                                    "instant","period"->{
                                        //实时 时间段 抽取 其它信息
//                                        val result = aiBaseBeen.bizData.forecast.filter { it.updateTime in startTime .. endTime }
                                    }
                                    "day","multi_day"->{
                                        //一天  多天  抽取 其它信息
//                                        val result = aiBaseBeen.bizData.hourly.filter { it.date in startTime .. endTime }
                                    }
                                }
                            }
                            "apparent_temperature"->{
                                //体感温度
                            }
                            "precipitation_probability"->{
                                //降水概率

                            }
                            "precipitation_amount"->{
                                //降水量

                            }
                            "snowfall_amount"->{
                                //降雪量

                            }
                            "humidity"->{
                                //湿度

                            }
                            "pressure"->{
                                //气压

                            }
                            "weather_condition"->{
                                //天气现象

                            }
                            "wind"->{
                                //风向、风级、风速

                            }
                            "visibility"->{
                                //能见度

                            }
                            "ultraviolet"->{
                                //紫外线

                            }
                            "sunrise_sunset"->{
                                //日出日落

                            }
                            "moon_phase"->{
                                //月相
//                                val result = aiBaseBeen.bizData.forecast.filter { it.updateTime in startTime .. endTime }
                            }
                            "weather_alert"->{
                                //天气预警
                                aiBaseBeen.bizData.alert
                            }
                        }
                    }
                }

                //基础信息
                dataPauseResult["baseInfo"] = baseCardInfo
                dataPauseResult["compareInfo"] = compareCarInfo
            }
        }
        Log.d(TAG,"dataPauseResult  = $dataPauseResult")
        return dataPauseResult
    }

}