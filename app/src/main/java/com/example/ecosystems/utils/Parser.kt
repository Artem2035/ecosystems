package com.example.ecosystems.utils

import com.example.ecosystems.DataClasses.Device
import com.example.ecosystems.DataClasses.Profile

class Parser() {

    fun deviceParse(listOfDevices: MutableList<Map<String, Any?>>, mapOfDevices: MutableMap<Int, Map<String, Any?>>): MutableList<Device> {

        val devicesList: MutableList<Device> = mutableListOf()
        for (device in listOfDevices){
            if(!device.containsKey("device_id"))
                continue
            val name = device.getOrDefault("name", "").toString()
            val location = device.getOrDefault("location_description", "").toString()
            val lastUpdate = device.getOrDefault("last_update_datetime", "").toString()
            val deviceItem = Device(device.get("device_id").toString().toDouble().toInt(), name, location, lastUpdate)
            devicesList.add(deviceItem)

            mapOfDevices.put(device.get("device_id").toString().toDouble().toInt(), device)
        }
        return devicesList
    }

    fun  profileParse(personalAccountData: MutableMap<String, Any?>): Profile {
        if(!personalAccountData.containsKey("id_user"))
            throw NoSuchFieldException("нет ключа id_user")
        val id = personalAccountData.get("id_user").toString().toInt()
        val name = personalAccountData.getOrDefault("name", "").toString()
        val secondName = personalAccountData.getOrDefault("second_name", "").toString()
        val email = personalAccountData.getOrDefault("email", "").toString()
        val phone = personalAccountData.getOrDefault("phone", "").toString()
        val organization = personalAccountData.getOrDefault("organization", "").toString()
        val is_send_emails_not_devices_link = personalAccountData.getOrDefault("is_send_emails_not_devices_link", false) as Boolean
        val profile: Profile = Profile(id, name, secondName, email, phone, organization, is_send_emails_not_devices_link)
        return profile
    }
}