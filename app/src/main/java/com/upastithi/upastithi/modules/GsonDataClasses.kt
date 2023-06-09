package com.upastithi.upastithi.modules

import com.google.gson.annotations.SerializedName

data class User(
    @SerializedName("email")
    val email: String,
    @SerializedName("pass")
    val pass: String,
    @SerializedName("login_type")
    val login_type: String
)

data class LoginResponse(
    @SerializedName("login")
    val login: String
)

data class TeacherInfo(
    @SerializedName("name")
    val name: String,
    @SerializedName("email")
    val email: String,
    @SerializedName("subjects")
    val subjects: String
)

data class GenerateCode(
    @SerializedName("email")
    val email: String,
    @SerializedName("pass")
    val pass: String,
    @SerializedName("login_type")
    val login_type: String,
    @SerializedName("subject")
    val subject: String,
    @SerializedName("lect_time")
    val lect_time: String,
    @SerializedName("lect_type")
    val lect_type: String,
    @SerializedName("year")
    val year: String,
    @SerializedName("div")
    val div: String
)

data class GenResponseCode(
    @SerializedName("response_code")
    val response_code: String,
    @SerializedName("code")
    val code: String,
    @SerializedName("name")
    val name: String
)

data class StudentInfo(
    @SerializedName("response_code")
    val response_code: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("id")
    val id: String,
    @SerializedName("course")
    val course: String,
    @SerializedName("year")
    val year: String,
    @SerializedName("div")
    val div: String,
    @SerializedName("batch")
    val batch: String,
    @SerializedName("theory_attended")
    val theory_attended: Int,
    @SerializedName("prac_attended")
    val prac_attended: Int,
    @SerializedName("theory_total")
    val theory_total: Int,
    @SerializedName("prac_total")
    val prac_total: Int
)

data class StudentMark(
    @SerializedName("email")
    val email: String,
    @SerializedName("pass")
    val pass: String,
    @SerializedName("login_type")
    val login_type: String,
    @SerializedName("code")
    val code: String,
    @SerializedName("location")
    val location: String
)

data class StudentMarkResponse(
    @SerializedName("response_code")
    val response_code: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("id")
    val id: String,
    @SerializedName("course")
    val course: String,
    @SerializedName("year")
    val year: String,
    @SerializedName("div")
    val div: String,
    @SerializedName("batch")
    val batch: String,
    @SerializedName("subject")
    val subject: String,
    @SerializedName("lect_type")
    val lect_type: String,
    @SerializedName("lect_time")
    val lect_time: String
)