package com.example.weatherapp.fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.weatherapp.MainViewModel
import com.example.weatherapp.R
import com.example.weatherapp.adapters.WeatherAdapter
import com.example.weatherapp.adapters.WeatherModel
import com.example.weatherapp.databinding.FragmentHoursBinding
import org.json.JSONArray
import org.json.JSONObject


class HoursFragment : Fragment() {
    private lateinit var binding:FragmentHoursBinding
    private lateinit var adapter:WeatherAdapter
    private val model:MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding=FragmentHoursBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initRcView()
        model.liveDataCurrent.observe(viewLifecycleOwner){
            Log.d("MyLog","Hours: ${it.hours}")
            adapter.submitList(getHoursList(it))
        }

    }

    private fun initRcView()=with(binding){
        rcView.layoutManager=LinearLayoutManager(activity)
        adapter=WeatherAdapter(null)
        rcView.adapter=adapter
    }
    private fun getHoursList(dayItem:WeatherModel):List<WeatherModel>{
        val hoursArray=JSONArray(dayItem.hours)
        val list=ArrayList<WeatherModel>()
        for(i in 0 until hoursArray.length()) {
            val arrayItem = hoursArray[i] as JSONObject
            val hourItem = WeatherModel(
                "",
                arrayItem.getString("time"),
                arrayItem
                    .getJSONObject("condition").getString("text"),
                arrayItem.getString("temp_c").toFloat().toInt().toString()+"°C",
                "",
                "",
                arrayItem.getJSONObject("condition").getString("icon"),
                ""
            )
            list.add(hourItem)
        }
        return list

    }

    companion object {
        @JvmStatic
        fun newInstance() = HoursFragment()
    }
}