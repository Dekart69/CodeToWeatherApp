package com.example.weatherapp.fragments

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.activityViewModels
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.weatherapp.DialogManager
import com.example.weatherapp.MainViewModel
import com.example.weatherapp.R
import com.example.weatherapp.adapters.VpAdapter
import com.example.weatherapp.adapters.WeatherModel
import com.example.weatherapp.databinding.FragmentMainBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.material.tabs.TabLayoutMediator
import com.squareup.picasso.Picasso
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

const val API_KEY="21c2065d21a8450186a182710233110"

class MainFragment : Fragment() {
    private lateinit var fLocationClient:FusedLocationProviderClient
    private val fragList= listOf(
        HoursFragment.newInstance(),DaysFragment.newInstance()
    )
    private lateinit var binding:FragmentMainBinding
    private lateinit var pLauncher: ActivityResultLauncher<String>
    private val model:MainViewModel by activityViewModels()
    private val t_list= listOf(
        "Hours",
        "Days"
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding=FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        checkPermission()
        init()
        updateCurrentCard()//можна запускати будь бо колбак чекає результат
    }
    private fun updateCurrentCard()=with(binding){
        model.liveDataCurrent.observe(viewLifecycleOwner){
            tvData.text=it.time
            tvDayOfWeak.text=dayOfWeak(it.time)
            tvCity.text=it.city
            tvCurrentTemp.text=it.currentTemp.ifEmpty{"${it.maxTemp}°C/${it.minTemp}°C"}
            tvCondition.text=it.condition
            tvMaxMin.text=if(it.currentTemp.isEmpty()) "" else "${it.maxTemp}°C/${it.minTemp}°C"
            Picasso.get().load("https:"+it.imageUrl).into(imWeather)
        }
    }

    private fun parseWeatherData(result:String){
        val mainObject=JSONObject(result)
        val list=parseDays(mainObject)
        parseCurrentData(mainObject,list[0])
    }

    private fun parseCurrentData(mainObject:JSONObject,weatherItem:WeatherModel){
        val item=WeatherModel(
            mainObject.getJSONObject("location").getString("name"),
            mainObject.getJSONObject("current").getString("last_updated"),
            mainObject.getJSONObject("current").getJSONObject("condition").getString("text"),
            mainObject.getJSONObject("current").getString("temp_c").toFloat().toInt().toString()+"°C",
            weatherItem.maxTemp,
            weatherItem.minTemp,
            mainObject.getJSONObject("current").getJSONObject("condition").getString("icon"),
            weatherItem.hours
        )
        model.liveDataCurrent.value=item
    }
    private fun parseDays(mainObject:JSONObject):List<WeatherModel>{
        val list=ArrayList<WeatherModel>()
        val daysArray=mainObject.getJSONObject("forecast")
            .getJSONArray("forecastday")
        val name=mainObject.getJSONObject("location").getString("name")
        for(i in 0 until daysArray.length()){
            val day=daysArray[i] as JSONObject
            val item=WeatherModel(
                name,
                day.getString("date"),
                day.getJSONObject("day")
                    .getJSONObject("condition").getString("text"),
                "",
                day.getJSONObject("day").getString("maxtemp_c").toFloat().toInt().toString(),
                day.getJSONObject("day").getString("mintemp_c").toFloat().toInt().toString(),
                day.getJSONObject("day")
                    .getJSONObject("condition").getString("icon"),
                day.getJSONArray("hour").toString()
            )
            list.add(item)
        }
        model.liveDataList.value=list
        return list

    }


    private fun init()=with(binding){
        fLocationClient= LocationServices.getFusedLocationProviderClient(requireContext())

        val adapter=VpAdapter(activity as AppCompatActivity,fragList)
        vp.adapter=adapter
        TabLayoutMediator(tabLayout,vp){
            tab,pos-> tab.text=t_list[pos]
        }.attach()
        ibSync.setOnClickListener{
            tabLayout.selectTab(tabLayout.getTabAt(0))
            checkLocation()
        }
        ibSearch.setOnClickListener {
            DialogManager.searchByNameDialog(requireContext(),object: DialogManager.Listener{
                override fun OnClick(name: String?) {
                    if (name != null) {
                        requestWeatherData(name)
                    }
                }
                })

        }




    }

    override fun onResume() {
        super.onResume()
        checkLocation()
    }

    private fun checkLocation(){
        if(isLocationEnabled()){
            getLocation()
        }else{
            DialogManager.locationSettingsDialog(requireContext(),object: DialogManager.Listener{
                override fun OnClick(name:String?) {
                    startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                }
            })
        }
    }

    private fun isLocationEnabled():Boolean{
        val locManag=activity?.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locManag.isProviderEnabled(LocationManager.GPS_PROVIDER)

    }

    private fun getLocation(){
        val ct=CancellationTokenSource()

        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        fLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY,ct.token)
            .addOnCompleteListener {
                requestWeatherData("${it.result.latitude},${it.result.longitude}")
                //45.0883579,-6.589363
            }

    }

    private fun permissionListener(){
        pLauncher=registerForActivityResult(ActivityResultContracts.RequestPermission()){
            Toast.makeText(activity,"Permission is $it", Toast.LENGTH_LONG).show()
        }
    }
    private fun checkPermission(){
        if(!isPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION)){
            permissionListener()
            pLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun requestWeatherData(city:String){
        val url="http://api.weatherapi.com/v1/forecast.json?key=" +
                API_KEY +
                "&q=" +
                city +
                "&days=" +
                "14" +
                "&aqi=no&alerts=no"
        val queue= Volley.newRequestQueue(context)
        val request=StringRequest(
            Request.Method.GET,
            url,
            {
                result->parseWeatherData(result)
            },
            {
                error-> Log.d("MyLog","Error: $error")
            }
        )
        queue.add(request)

    }
    companion object {
        @JvmStatic
        fun newInstance() = MainFragment()
    }

    private fun dayOfWeak(dateString:String):String {
        // Задаємо дату, для якої потрібно визначити день тижня

        var day=""

        // Встановлюємо формат дати
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        try {
            // Перетворюємо рядок у тип дати
            val date = dateFormat.parse(dateString)

            // Створюємо об'єкт Calendar та встановлюємо задану дату
            val calendar = Calendar.getInstance()
            calendar.time = date

            // Отримуємо день тижня
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

            // Виводимо результат
            val dayOfWeekString = when (dayOfWeek) {
                Calendar.SUNDAY -> "Sunday"
                Calendar.MONDAY -> "Monday"
                Calendar.TUESDAY -> "Tuesday"
                Calendar.WEDNESDAY -> "Wednesday"
                Calendar.THURSDAY -> "Thursday"
                Calendar.FRIDAY -> "Friday"
                Calendar.SATURDAY -> "Saturday"
                else -> "Sunday" // Це значення повертається, якщо щось пішло не так
            }
            day="$dayOfWeekString"
        } catch (e: Exception) {
            println("Помилка: ${e.message}")
        }
        return day
    }
}