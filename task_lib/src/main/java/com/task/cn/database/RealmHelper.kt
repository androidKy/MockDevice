package com.task.cn.database

import com.safframework.log.L
import com.task.cn.jbean.TaskBean
import io.realm.Realm
import io.realm.RealmConfiguration

/**
 * Description:
 * Created by Quinin on 2020-03-04.
 **/
class RealmHelper : DBHelper {


    private var mTaskRealm: Realm? = null

    init {
        if (mTaskRealm == null) {
            val builder = RealmConfiguration.Builder()
                    .deleteRealmIfMigrationNeeded()//声明版本冲突时自动删除原数据库，开发时候打开
                    .name(TASK_REALM_NAME)
                    //.encryptionKey(TASK_DB_KEY.toByteArray(Charsets.UTF_8))
                    .schemaVersion(1L)
                    .build()
            mTaskRealm = Realm.getInstance(builder)
        }
    }

    companion object {
        const val TASK_REALM_NAME = "task.realm"
        const val TASK_DB_KEY = "task@quin"
/*
        val instance by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            RealmHelper()
        }*/
    }


    override fun insertTask(taskBean: TaskBean) {
        mTaskRealm?.executeTransaction {
            val result = it.where(TaskBean::class.java).equalTo("task_id", taskBean.task_id).findFirst()
            if (result != null) {
                L.d("只更新TaskBean")
            } else {
                var primaryKey = 0L
                val tasks = it.where(TaskBean::class.java).findAll()
                if (!tasks.isNullOrEmpty()) {
                    primaryKey = (tasks.size + 1).toLong()
                } else {
                    primaryKey++
                }
                taskBean.id = primaryKey

                L.d("主键: $primaryKey")
            }
            it.copyToRealmOrUpdate(taskBean)
        }
    }


    override fun queryTasksByStatus(taskStatus: Int): List<TaskBean> {
        val taskBeanList = ArrayList<TaskBean>()
        mTaskRealm?.executeTransaction {
            val taskRealms =
                    it.where(TaskBean::class.java)
                            .equalTo("task_status", taskStatus)
                            .sort("task_id")
                            .findAll()
            val copyFromRealm = it.copyFromRealm(taskRealms)
            taskBeanList.addAll(copyFromRealm)
        }

        return taskBeanList
    }

    override fun queryTaskByTaskId(taskId: Long): TaskBean {
        var taskBean = TaskBean()
        mTaskRealm?.executeTransaction {
            val realmTaskBean = it.where(TaskBean::class.java)
                    .equalTo("task_id", taskId)
                    .findFirst()
            if (realmTaskBean != null)
                taskBean = realmTaskBean
        }

        return taskBean
    }

    override fun deleteTaskByStatus(taskStatus: Int) {
        mTaskRealm?.executeTransaction {
            it.where(TaskBean::class.java).equalTo("task_status", taskStatus)
                    .findAll()
                    .deleteAllFromRealm()
        }
    }

    override fun deleteAllTask() {
        mTaskRealm?.executeTransaction {
            it.deleteAll()
        }
    }

    override fun closeRealm() {
        mTaskRealm?.apply {
            if (!this.isClosed)
                this.close()
        }
    }

}