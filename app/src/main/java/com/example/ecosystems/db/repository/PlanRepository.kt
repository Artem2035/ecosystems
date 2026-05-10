package com.example.ecosystems.db.repository

import com.example.ecosystems.db.dao.PlanEntityDao
import com.example.ecosystems.db.entity.PlanEntity
import com.example.ecosystems.db.entity.PlanFileEntity

class PlanRepository(private val planEntityDao: PlanEntityDao) {

    suspend fun getAllPlans() = planEntityDao.getAllPlans()

    suspend fun insertAll(plans: List<PlanEntity>) {
        planEntityDao.insertAll(plans)
    }

    suspend fun insertAllFiles(planFiles: List<PlanFileEntity>) {
        planEntityDao.insertAllFiles(planFiles)
    }

    fun getAllPlansWithData() = planEntityDao.getAllPlansWithData()

    suspend fun getPlanById(planId: Int) = planEntityDao.getPlanById(planId)

    suspend fun getPlanUuidById(planId: Int) =  planEntityDao.getPlanUuidById(planId)

    suspend fun getPlanData(planId: Int) = planEntityDao.getPlanWithData(planId)

    suspend fun getPlanFiles(planId: Int) = planEntityDao.getPlanFiles(planId)

    suspend fun deleteAll() = planEntityDao.deleteAll()
    
    suspend fun hasPlanData(planId: Int): Boolean = planEntityDao.countLayersByPlanId(planId) > 0
}