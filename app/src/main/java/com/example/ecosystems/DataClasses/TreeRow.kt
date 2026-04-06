package com.example.ecosystems.DataClasses

data  class TreeRow (
    val number: Int,
    val species: String = "",
    val age: Int = 0,
    val d13: Double = 0.0,
    val height: Double = 0.0,
    val lk: Double = 0.0,
    val hdk: Double = 0.0,
    val crownDiameterNS: Double = 0.0,
    val crownDiameterEW: Double = 0.0,
    val averageCrownDiameter: Double = 0.0)