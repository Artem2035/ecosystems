package com.example.ecosystems.DataClasses

data class Profile(var id_user: Int, var name: String, var second_name: String, var email: String,
                   var phone: String, var organization: String, var is_send_emails_not_devices_link: Boolean)
